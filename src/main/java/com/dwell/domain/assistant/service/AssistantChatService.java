package com.dwell.domain.assistant.service;

import com.dwell.domain.assistant.dto.request.ActionChooseRequest;
import com.dwell.domain.assistant.dto.request.MessageSendRequest;
import com.dwell.domain.assistant.dto.response.ActionResultResponse;
import com.dwell.domain.assistant.dto.response.CaseUpdateResponse;
import com.dwell.domain.assistant.dto.response.MessageResponse;
import com.dwell.domain.assistant.entity.AssistantAction;
import com.dwell.domain.assistant.entity.Conversation;
import com.dwell.domain.assistant.entity.Message;
import com.dwell.domain.assistant.enums.AssistantActionType;
import com.dwell.domain.assistant.enums.ConversationType;
import com.dwell.domain.assistant.enums.MessageRole;
import com.dwell.domain.assistant.exception.AssistantErrorCode;
import com.dwell.domain.assistant.repository.AssistantActionRepository;
import com.dwell.domain.assistant.repository.ConversationRepository;
import com.dwell.domain.assistant.repository.MessageRepository;
import com.dwell.domain.building.entity.InspectionResult;
import com.dwell.domain.cases.dto.request.CaseEventCreateRequest;
import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.entity.CaseEvent;
import com.dwell.domain.cases.enums.CaseEventRule;
import com.dwell.domain.cases.enums.CaseEventType;
import com.dwell.domain.cases.enums.CaseStage;
import com.dwell.domain.cases.service.CaseEventService;
import com.dwell.domain.file.entity.File;
import com.dwell.domain.file.enums.FilePurpose;
import com.dwell.domain.file.repository.FileRepository;
import com.dwell.domain.legal.dto.LegalSource;
import com.dwell.domain.legal.service.LegalSourceService;
import com.dwell.domain.notice.entity.LandlordResponse;
import com.dwell.domain.notice.repository.LandlordResponseRepository;
import com.dwell.domain.user.entity.User;
import com.dwell.global.ai.LlmException;
import com.dwell.global.ai.LlmMessage;
import com.dwell.global.ai.NebiusChatClient;
import com.dwell.global.common.enums.IssueCategory;
import com.dwell.global.common.enums.Location;
import com.dwell.global.exception.CustomException;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
Assistant 채팅 (Nemotron 호출)
- 메시지 전송: 사용자 메시지 저장 -> 모델 답변을 SSE로 스트리밍 -> 답변·카드·액션 저장
- 액션 실행: 선택 기록 + (집주인 답장 반영이면) 케이스 이벤트 기록·답장 초안 생성
DB 작업은 TransactionTemplate으로 짧게 끊고, 모델 호출은 트랜잭션 밖에서 함
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssistantChatService {

    private static final int HISTORY_SIZE = 20; // 모델에 보내는 최근 메시지 수
    private static final long SSE_TIMEOUT_MS = 180_000;
    private static final int MAX_CARDS = 2;
    private static final int MAX_QUICK_REPLIES = 3;
    private static final int QUICK_REPLY_MAX_LENGTH = 80;
    private static final String YES = "YES";
    private static final String NO = "NO";
    private static final String FALLBACK_ANSWER = "Sorry, I couldn't put together an answer. Could you try asking again?";
    private static final String REPLY_DRAFT_INTRO = "Here's a reply you can send to your landlord. Want to send it?";

    private static final Set<String> CARD_TYPES = Set.of("TRANSLATION", "LEGAL_RULE", "MESSAGE_DRAFT", "CASE_PREFILL");
    private static final Set<String> REPLY_INTENTS = Set.of("REPAIR_CONFIRMED", "SCHEDULING", "REFUSED", "BLAME_SHIFT", "VAGUE");

    // 액션별 선택지 (클라 버튼 문구)
    private static final Map<AssistantActionType, List<Map<String, Object>>> CHOICES = Map.of(
            AssistantActionType.APPLY_LANDLORD_REPLY, List.of(choice(YES, "Yes"), choice(NO, "No, I'll do it myself.")),
            AssistantActionType.SEND_MESSAGE, List.of(choice(YES, "Yes, send it"), choice(NO, "Not now")),
            AssistantActionType.CREATE_CASE, List.of(choice(YES, "Start a New Case"), choice(NO, "Just keep chatting")));

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final AssistantActionRepository assistantActionRepository;
    private final FileRepository fileRepository;
    private final LandlordResponseRepository landlordResponseRepository;
    private final CaseEventService caseEventService;
    private final NebiusChatClient chatClient;
    private final JsonMapper jsonMapper;
    private final TransactionTemplate transactionTemplate;
    private final AssistantMessageMapper messageMapper;
    private final LegalSourceService legalSourceService;

    // 모델 호출은 응답 스레드를 막지 않도록 가상 스레드에서
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }

    // 메시지 전송 서비스 (SSE: token -> card -> action -> quick_replies -> done, 실패하면 error)
    public SseEmitter sendMessage(Long userId, Long conversationId, MessageSendRequest request) {

        log.info("[AssistantChatService] 메시지 전송 서비스 - 시작: userId={}, conversationId={}", userId, conversationId);

        // 키가 없으면 스트림을 열기 전에 503
        if (!chatClient.isConfigured()) {
            log.warn("[AssistantChatService] 메시지 전송 서비스 - NEBIUS_API_KEY가 설정되지 않았습니다");
            throw new CustomException(AssistantErrorCode.AI_NOT_CONFIGURED);
        }

        // 사용자 메시지 저장 + 모델에 보낼 대화 준비 (트랜잭션 안에서 끝냄)
        ChatContext context = transactionTemplate.execute(status -> {
            Conversation conversation = findOwnedConversation(userId, conversationId);
            List<Long> fileIds = validateAttachments(userId, request.getFileIds());
            messageRepository.save(Message.builder()
                    .conversation(conversation)
                    .role(MessageRole.USER)
                    .content(request.getContent().strip())
                    .attachments(fileIds.isEmpty() ? null : fileIds)
                    .build());
            return buildContext(conversation);
        });

        // 모델 답변은 별도 스레드에서 스트리밍
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        executor.execute(() -> generateAnswer(context, emitter));
        return emitter;
    }

    // 채팅 액션 실행 서비스
    public ActionResultResponse chooseAction(Long userId, Long conversationId, Long actionId, ActionChooseRequest request) {

        log.info("[AssistantChatService] 액션 실행 서비스 - 시작: userId={}, conversationId={}, actionId={}, choice={}",
                userId, conversationId, actionId, request.getChoice());

        // 1) 선택 기록 + 케이스 반영 (트랜잭션)
        ActionOutcome outcome = transactionTemplate.execute(status -> applyChoice(userId, conversationId, actionId, request));

        // 2) 집주인 답장 반영(YES)이면 회신 초안 생성 (트랜잭션 밖에서 모델 호출, 실패해도 선택·케이스 반영은 유지)
        MessageResponse nextMessage = null;
        if (outcome.replyDraftContext() != null) {
            nextMessage = createReplyDraftMessage(conversationId, outcome.replyDraftContext());
        }

        log.info("[AssistantChatService] 액션 실행 서비스 - 완료: actionId={}, type={}, choice={}, caseUpdate={}",
                actionId, outcome.type(), outcome.chosen(), outcome.caseUpdate() != null);

        return ActionResultResponse.builder()
                .actionId(actionId)
                .chosen(outcome.chosen())
                .prefill(outcome.prefill())
                .messageBody(outcome.messageBody())
                .nextMessage(nextMessage)
                .caseUpdate(outcome.caseUpdate())
                .build();
    }

    // ========== 메시지 전송 ==========

    // 모델 호출 -> 본문 토큰 스트리밍 -> META 검증 -> 저장 -> 카드·액션·빠른 답장·완료 이벤트
    private void generateAnswer(ChatContext context, SseEmitter emitter) {
        try {
            MetaSplitter splitter = new MetaSplitter(delta -> send(emitter, "token", Map.of("delta", delta)));
            chatClient.stream(context.llmMessages(), splitter::accept);
            splitter.finish();

            ParsedMeta meta = parseMeta(splitter.meta(), context);
            String answer = splitter.answer().strip();
            if (answer.isEmpty()) {
                answer = FALLBACK_ANSWER;
                send(emitter, "token", Map.of("delta", answer));
            }

            String content = answer;
            SavedAnswer saved = transactionTemplate.execute(status -> saveAnswer(context.conversationId(), content, meta));

            for (Map<String, Object> card : meta.cards()) {
                send(emitter, "card", card);
            }
            if (saved.actionId() != null) {
                Map<String, Object> action = new LinkedHashMap<>();
                action.put("actionId", saved.actionId());
                action.put("type", meta.actionType());
                action.put("choices", CHOICES.get(meta.actionType()));
                send(emitter, "action", action);
            }
            if (!meta.quickReplies().isEmpty()) {
                send(emitter, "quick_replies", Map.of("items", meta.quickReplies()));
            }
            send(emitter, "done", Map.of("messageId", saved.messageId()));
            emitter.complete();

            log.info("[AssistantChatService] 메시지 전송 서비스 - 완료: conversationId={}, messageId={}, cards={}, action={}",
                    context.conversationId(), saved.messageId(), meta.cards().size(), meta.actionType());
        } catch (LlmException e) {
            log.error("[AssistantChatService] 메시지 전송 서비스 - 모델 호출 실패: conversationId={}", context.conversationId(), e);
            sendErrorAndComplete(emitter, AssistantErrorCode.AI_RESPONSE_FAILED);
        } catch (ClientDisconnected e) {
            log.info("[AssistantChatService] 메시지 전송 서비스 - 클라이언트 연결 종료: conversationId={}", context.conversationId());
        } catch (Exception e) {
            log.error("[AssistantChatService] 메시지 전송 서비스 - 처리 실패: conversationId={}", context.conversationId(), e);
            sendErrorAndComplete(emitter, AssistantErrorCode.AI_RESPONSE_FAILED);
        }
    }

    // 답변 메시지 + 액션 저장
    private SavedAnswer saveAnswer(Long conversationId, String content, ParsedMeta meta) {
        Conversation conversation = conversationRepository.getReferenceById(conversationId);
        conversation.markRead(); // 사용자가 스트리밍으로 보고 있으니 읽음 처리
        Message message = messageRepository.save(Message.builder()
                .conversation(conversation)
                .role(MessageRole.ASSISTANT)
                .content(content)
                .citations(meta.citations().isEmpty() ? null : meta.citations())
                .cards(meta.cards().isEmpty() ? null : meta.cards())
                .quickReplies(meta.quickReplies().isEmpty() ? null : meta.quickReplies())
                .build());

        Long actionId = null;
        if (meta.actionType() != null) {
            actionId = assistantActionRepository.save(AssistantAction.builder()
                    .message(message)
                    .type(meta.actionType())
                    .choices(CHOICES.get(meta.actionType()))
                    .payload(meta.actionPayload())
                    .build()).getId();
        }
        return new SavedAnswer(message.getId(), actionId);
    }

    // 모델이 붙인 META JSON 검증 (형식이 틀리면 카드·액션 없이 본문만)
    @SuppressWarnings("unchecked")
    private ParsedMeta parseMeta(String metaText, ChatContext context) {
        Map<String, Object> meta = Map.of();
        int start = metaText.indexOf('{');
        int end = metaText.lastIndexOf('}');
        if (start >= 0 && end > start) {
            try {
                meta = jsonMapper.readValue(metaText.substring(start, end + 1), Map.class);
            } catch (Exception e) {
                log.warn("[AssistantChatService] META 파싱 실패 - 카드·액션 없이 진행: conversationId={}", context.conversationId());
            }
        }

        boolean caseChat = context.type() == ConversationType.CASE;
        List<Map<String, Object>> cards = new ArrayList<>();
        List<Map<String, Object>> citations = new ArrayList<>();
        if (meta.get("cards") instanceof List<?> rawCards) {
            for (Object raw : rawCards) {
                if (!(raw instanceof Map<?, ?> card) || cards.size() >= MAX_CARDS) {
                    continue;
                }
                Map<String, Object> validated = validateCard((Map<String, Object>) card, caseChat, context.legalSources(), citations);
                if (validated != null) {
                    cards.add(validated);
                }
            }
        }

        List<String> quickReplies = meta.get("quickReplies") instanceof List<?> raw ? raw.stream()
                .filter(item -> item instanceof String text && !text.isBlank())
                .map(item -> ((String) item).strip())
                .filter(text -> text.length() <= QUICK_REPLY_MAX_LENGTH)
                .limit(MAX_QUICK_REPLIES)
                .toList() : List.of();

        // 액션: 채팅 종류·카드와 맞을 때만
        AssistantActionType actionType = null;
        Map<String, Object> payload = null;
        if (meta.get("action") instanceof Map<?, ?> rawAction) {
            Map<String, Object> action = (Map<String, Object>) rawAction;
            String type = String.valueOf(action.get("type"));
            Map<String, Object> draftCard = findCard(cards, "MESSAGE_DRAFT");
            Map<String, Object> prefillCard = findCard(cards, "CASE_PREFILL");

            if (type.equals("SEND_MESSAGE") && caseChat && draftCard != null) {
                actionType = AssistantActionType.SEND_MESSAGE;
                payload = Map.of("draftBody", draftCard.get("body"));
            } else if (type.equals("CREATE_CASE") && !caseChat && prefillCard != null) {
                actionType = AssistantActionType.CREATE_CASE;
                payload = Map.of("prefill", prefillCard.get("prefill"));
            } else if (type.equals("APPLY_LANDLORD_REPLY") && caseChat
                    && REPLY_INTENTS.contains(String.valueOf(action.get("intent")))
                    && action.get("landlordText") instanceof String text && !text.isBlank()) {
                actionType = AssistantActionType.APPLY_LANDLORD_REPLY;
                payload = new LinkedHashMap<>();
                payload.put("intent", action.get("intent"));
                payload.put("landlordText", text.strip());
                String scheduledStart = action.get("scheduledStart") instanceof String s ? s : null;
                if (scheduledStart != null && CaseEvent.parseDateTime(scheduledStart, ZoneId.systemDefault()) != null) {
                    payload.put("scheduledStart", scheduledStart);
                }
            }
        }
        return new ParsedMeta(cards, citations, quickReplies, actionType, payload);
    }

    // 카드 검증 (모르는 타입·빈 본문·목록에 없는 법률 근거는 버림)
    private Map<String, Object> validateCard(Map<String, Object> card, boolean caseChat, Map<String, LegalSource> legalSources,
                                             List<Map<String, Object>> citations) {
        String type = String.valueOf(card.get("type"));
        if (!CARD_TYPES.contains(type) || !(card.get("body") instanceof String body) || body.isBlank()) {
            return null;
        }
        Map<String, Object> validated = new LinkedHashMap<>();
        validated.put("type", type);
        validated.put("label", card.get("label") instanceof String label ? label : type.replace('_', ' '));
        validated.put("body", body.strip());

        switch (type) {
            case "LEGAL_RULE" -> {
                LegalSource source = legalSources.get(String.valueOf(card.get("citationId")));
                if (source == null) {
                    return null;
                }
                Map<String, Object> citation = Map.of("label", source.label() + " (" + source.title() + ")", "url", source.url());
                validated.put("citation", citation);
                citations.add(citation);
            }
            case "CASE_PREFILL" -> {
                if (caseChat || !(card.get("prefill") instanceof Map<?, ?> prefill)) {
                    return null;
                }
                Map<String, Object> clean = new LinkedHashMap<>();
                clean.put("issueCategory", enumOrNull(IssueCategory.class, prefill.get("issueCategory")));
                clean.put("location", enumOrNull(Location.class, prefill.get("location")));
                clean.put("description", prefill.get("description") instanceof String d ? d.strip() : null);
                validated.put("prefill", clean);
            }
            default -> {
            }
        }
        return validated;
    }

    // ========== 액션 실행 ==========

    @SuppressWarnings("unchecked")
    private ActionOutcome applyChoice(Long userId, Long conversationId, Long actionId, ActionChooseRequest request) {
        Conversation conversation = findOwnedConversation(userId, conversationId);
        AssistantAction action = assistantActionRepository.findByIdAndMessageConversationId(actionId, conversationId)
                .orElseThrow(() -> {
                    log.warn("[AssistantChatService] 액션 실행 실패 - 없는 액션: conversationId={}, actionId={}", conversationId, actionId);
                    return new CustomException(AssistantErrorCode.ACTION_NOT_FOUND);
                });
        if (action.getChosen() != null) {
            log.warn("[AssistantChatService] 액션 실행 실패 - 이미 선택함: actionId={}, chosen={}", actionId, action.getChosen());
            throw new CustomException(AssistantErrorCode.ACTION_ALREADY_CHOSEN);
        }
        String choice = request.getChoice().strip();
        boolean validChoice = action.getChoices().stream().anyMatch(c -> choice.equals(c.get("key")));
        if (!validChoice) {
            log.warn("[AssistantChatService] 액션 실행 실패 - 없는 선택지: actionId={}, choice={}", actionId, choice);
            throw new CustomException(AssistantErrorCode.INVALID_CHOICE);
        }

        Map<String, Object> payload = action.getPayload() == null ? Map.of() : action.getPayload();
        boolean yes = YES.equals(choice);

        return switch (action.getType()) {
            case CREATE_CASE -> {
                action.choose(choice, null);
                Map<String, Object> prefill = yes && payload.get("prefill") instanceof Map<?, ?> p ? (Map<String, Object>) p : null;
                yield new ActionOutcome(action.getType(), choice, prefill, null, null, null);
            }
            case SEND_MESSAGE -> {
                String body = request.getEditedBody() != null && !request.getEditedBody().isBlank()
                        ? request.getEditedBody().strip() : (String) payload.get("draftBody");
                action.choose(choice, yes ? Map.of("sentBody", body) : null);
                yield new ActionOutcome(action.getType(), choice, null, yes ? body : null, null, null);
            }
            case APPLY_LANDLORD_REPLY -> {
                if (!yes) {
                    action.choose(choice, null);
                    yield new ActionOutcome(action.getType(), choice, null, null, null, null);
                }
                yield applyLandlordReply(userId, conversation, action, payload);
            }
        };
    }

    // 집주인 답장 반영: 답장 저장 + intent에 맞는 케이스 이벤트 기록 (전이가 안 되면 기록 없이 진행)
    private ActionOutcome applyLandlordReply(Long userId, Conversation conversation, AssistantAction action, Map<String, Object> payload) {
        Case caseEntity = conversation.getCaseEntity();
        if (caseEntity == null || caseEntity.getDeletedAt() != null) {
            log.warn("[AssistantChatService] 액션 실행 실패 - 케이스가 없거나 삭제됨: conversationId={}", conversation.getId());
            throw new CustomException(AssistantErrorCode.ACTION_EXPIRED);
        }

        String landlordText = String.valueOf(payload.get("landlordText"));
        String intent = String.valueOf(payload.get("intent"));
        String scheduledStart = payload.get("scheduledStart") instanceof String s ? s : null;

        LandlordResponse response = landlordResponseRepository.save(LandlordResponse.builder()
                .caseEntity(caseEntity)
                .rawText(landlordText)
                .receivedAt(LocalDateTime.now())
                .build());

        // intent -> 케이스 이벤트
        CaseEventType eventType;
        Map<String, Object> eventPayload = new LinkedHashMap<>();
        if (intent.equals("REPAIR_CONFIRMED") && scheduledStart != null) {
            eventType = CaseEventType.REPAIR_SCHEDULED;
            eventPayload.put("scheduledStart", scheduledStart);
        } else if (intent.equals("REFUSED")) {
            eventType = CaseEventType.LANDLORD_REFUSED;
        } else if (intent.equals("BLAME_SHIFT")) {
            eventType = CaseEventType.LANDLORD_BLAME;
        } else {
            eventType = CaseEventType.LANDLORD_REPLIED;
            eventPayload.put("responseId", response.getId());
        }

        // 현재 stage에서 가능한 전이일 때만 기록 (안 되는 전이로 호출하면 트랜잭션이 롤백 표시됨)
        CaseUpdateResponse caseUpdate = null;
        CaseStage fromStage = caseEntity.getStage();
        boolean allowed = CaseEventRule.of(eventType).map(rule -> rule.canTransitFrom(fromStage)).orElse(false);
        if (allowed) {
            CaseEventCreateRequest eventRequest = new CaseEventCreateRequest();
            eventRequest.setType(eventType);
            eventRequest.setPayload(eventPayload);
            caseEventService.recordEvent(caseEntity.getId(), userId, eventRequest);
            caseUpdate = CaseUpdateResponse.builder()
                    .caseId(caseEntity.getId())
                    .fromStage(fromStage)
                    .toStage(caseEntity.getStage())
                    .build();
        } else {
            log.info("[AssistantChatService] 집주인 답장 반영 - 현재 stage에서 기록할 수 없는 이벤트라 건너뜀: caseId={}, stage={}, event={}",
                    caseEntity.getId(), fromStage, eventType);
        }

        action.choose(YES, Map.of("responseId", response.getId()));

        ReplyDraftContext draftContext = new ReplyDraftContext(caseContext(caseEntity), landlordText, intent);
        return new ActionOutcome(action.getType(), YES, null, null, caseUpdate, draftContext);
    }

    // 회신 초안 생성 + 메시지 저장 (MESSAGE_DRAFT 카드 + SEND_MESSAGE 액션)
    private MessageResponse createReplyDraftMessage(Long conversationId, ReplyDraftContext draftContext) {
        if (!chatClient.isConfigured()) {
            return null;
        }
        String draft;
        try {
            draft = chatClient.complete(List.of(
                    LlmMessage.system(AssistantPrompts.replyDraftSystemPrompt(
                            draftContext.caseContext(), draftContext.landlordText(), draftContext.intent())),
                    LlmMessage.user("Write the reply.")));
        } catch (LlmException e) {
            log.error("[AssistantChatService] 회신 초안 생성 실패 - 선택·케이스 반영은 유지: conversationId={}", conversationId, e);
            return null;
        }
        if (draft.isBlank()) {
            return null;
        }

        Map<String, Object> card = new LinkedHashMap<>();
        card.put("type", "MESSAGE_DRAFT");
        card.put("label", "Want to send this?");
        card.put("body", draft);

        return transactionTemplate.execute(status -> {
            Conversation conversation = conversationRepository.getReferenceById(conversationId);
            conversation.markRead(); // 액션 응답으로 바로 보여 주니 읽음 처리
            Message message = messageRepository.save(Message.builder()
                    .conversation(conversation)
                    .role(MessageRole.ASSISTANT)
                    .content(REPLY_DRAFT_INTRO)
                    .cards(List.of(card))
                    .build());
            AssistantAction action = assistantActionRepository.save(AssistantAction.builder()
                    .message(message)
                    .type(AssistantActionType.SEND_MESSAGE)
                    .choices(CHOICES.get(AssistantActionType.SEND_MESSAGE))
                    .payload(Map.of("draftBody", draft))
                    .build());
            return messageMapper.toResponse(message, action, Map.of());
        });
    }

    // ========== 헬퍼 함수 ==========

    // 본인 채팅방 조회 (없으면 404, 남의 채팅방이면 403)
    private Conversation findOwnedConversation(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> {
                    log.warn("[AssistantChatService] 채팅방을 찾을 수 없습니다: conversationId={}", conversationId);
                    return new CustomException(AssistantErrorCode.CONVERSATION_NOT_FOUND);
                });
        if (!conversation.getUser().getId().equals(userId)) {
            log.warn("[AssistantChatService] 본인 채팅방이 아닙니다: userId={}, conversationId={}", userId, conversationId);
            throw new CustomException(AssistantErrorCode.CONVERSATION_ACCESS_DENIED);
        }
        return conversation;
    }

    // 첨부 파일 확인 (내가 올린 채팅 첨부 파일만)
    private List<Long> validateAttachments(Long userId, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return List.of();
        }
        List<Long> ids = fileIds.stream().distinct().toList();
        Map<Long, File> files = fileRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(File::getId, Function.identity()));
        boolean valid = files.size() == ids.size() && files.values().stream()
                .allMatch(f -> f.getUser().getId().equals(userId) && f.getPurpose() == FilePurpose.CHAT_ATTACHMENT);
        if (!valid) {
            log.warn("[AssistantChatService] 사용할 수 없는 첨부 파일: userId={}, fileIds={}", userId, ids);
            throw new CustomException(AssistantErrorCode.INVALID_ATTACHMENT);
        }
        return ids;
    }

    // 모델에 보낼 대화 (시스템 프롬프트 + 최근 메시지, 오래된 순)
    private ChatContext buildContext(Conversation conversation) {
        User user = conversation.getUser();
        Case caseEntity = conversation.getType() == ConversationType.CASE ? conversation.getCaseEntity() : null;
        InspectionResult inspection = conversation.getType() == ConversationType.INSPECTION ? conversation.getInspection() : null;

        // 법률 근거 검색: 케이스·확인 결과의 문제 유형에 맞는 조항 (일반 채팅이면 전체)
        IssueCategory issue = caseEntity != null ? caseEntity.getIssueCategory()
                : inspection != null ? inspection.getIssueCategory() : null;
        List<LegalSource> legalSources = legalSourceService.findSources(issue, LegalSourceService.USAGE_ASSISTANT);

        List<LlmMessage> llmMessages = new ArrayList<>();
        llmMessages.add(LlmMessage.system(AssistantPrompts.chatSystemPrompt(
                user.getNickname() == null ? "there" : user.getNickname(),
                user.getPreferredLanguage(),
                caseEntity == null ? null : caseContext(caseEntity),
                inspectionContext(inspection),
                LocalDate.now().toString(),
                legalSources)));

        List<Message> recent = new ArrayList<>(messageRepository.findByConversationIdOrderByIdDesc(
                conversation.getId(), PageRequest.of(0, HISTORY_SIZE)));
        Collections.reverse(recent);
        for (Message message : recent) {
            String content = message.getContent();
            if (message.getAttachments() != null && !message.getAttachments().isEmpty()) {
                content += "\n[The user attached " + message.getAttachments().size()
                        + " file(s). You cannot see them; ask the user to describe or paste the text if needed.]";
            }
            llmMessages.add(message.getRole() == MessageRole.USER ? LlmMessage.user(content) : LlmMessage.assistant(content));
        }
        Map<String, LegalSource> sourcesByCode = legalSources.stream()
                .collect(Collectors.toMap(LegalSource::code, source -> source));
        return new ChatContext(conversation.getId(), conversation.getType(), llmMessages, sourcesByCode);
    }

    // 케이스 요약 (프롬프트용)
    private String caseContext(Case caseEntity) {
        return "Case " + caseEntity.getCaseNumber() + " \"" + caseEntity.getTitle() + "\""
                + ", issue: " + caseEntity.getIssueCategory() + (caseEntity.getIssueDetail() == null ? "" : " / " + caseEntity.getIssueDetail())
                + ", location: " + caseEntity.getLocation()
                + ", stage: " + caseEntity.getStage()
                + (caseEntity.getDescription() == null ? "" : ", description: " + caseEntity.getDescription());
    }

    // 내 집 확인 결과 요약 (프롬프트용)
    private String inspectionContext(InspectionResult inspection) {
        return inspection == null ? null
                : "issue: " + inspection.getIssueCategory() + ", symptoms the user checked: " + String.join(", ", inspection.getSymptoms());
    }

    private static Map<String, Object> choice(String key, String label) {
        return Map.of("key", key, "label", label);
    }

    private static Map<String, Object> findCard(List<Map<String, Object>> cards, String type) {
        return cards.stream().filter(card -> type.equals(card.get("type"))).findFirst().orElse(null);
    }

    private static <E extends Enum<E>> String enumOrNull(Class<E> type, Object value) {
        if (!(value instanceof String text)) {
            return null;
        }
        return Arrays.stream(type.getEnumConstants()).map(Enum::name).filter(name -> name.equals(text)).findFirst().orElse(null);
    }

    // SSE 이벤트 보내기 (클라이언트가 끊겼으면 모델 호출도 멈추도록 예외)
    private void send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException | IllegalStateException e) {
            throw new ClientDisconnected(e);
        }
    }

    private void sendErrorAndComplete(SseEmitter emitter, AssistantErrorCode errorCode) {
        try {
            emitter.send(SseEmitter.event().name("error")
                    .data(Map.of("code", errorCode.getCode(), "message", errorCode.getMessage())));
            emitter.complete();
        } catch (IOException | IllegalStateException e) {
            emitter.completeWithError(e);
        }
    }

    // 모델 출력에서 본문과 META를 나눔 (본문은 받는 대로 내보내되 마커가 잘려 올 수 있어 마커 길이-1만큼은 남겨 둠)
    private static final class MetaSplitter {

        private final Consumer<String> onAnswer;
        private final StringBuilder answer = new StringBuilder();
        private final StringBuilder pending = new StringBuilder();
        private final StringBuilder meta = new StringBuilder();
        private boolean inMeta;

        private MetaSplitter(Consumer<String> onAnswer) {
            this.onAnswer = onAnswer;
        }

        void accept(String delta) {
            if (inMeta) {
                meta.append(delta);
                return;
            }
            pending.append(delta);
            int index = pending.indexOf(AssistantPrompts.META_MARKER);
            if (index >= 0) {
                emit(pending.substring(0, index));
                meta.append(pending.substring(index + AssistantPrompts.META_MARKER.length()));
                pending.setLength(0);
                inMeta = true;
                return;
            }
            int flush = pending.length() - (AssistantPrompts.META_MARKER.length() - 1);
            if (flush > 0) {
                emit(pending.substring(0, flush));
                pending.delete(0, flush);
            }
        }

        void finish() {
            if (!inMeta && !pending.isEmpty()) {
                emit(pending.toString());
                pending.setLength(0);
            }
        }

        private void emit(String text) {
            if (!text.isEmpty()) {
                answer.append(text);
                onAnswer.accept(text);
            }
        }

        String answer() {
            return answer.toString();
        }

        String meta() {
            return meta.toString();
        }
    }

    private static final class ClientDisconnected extends RuntimeException {
        private ClientDisconnected(Throwable cause) {
            super(cause);
        }
    }

    private record ChatContext(Long conversationId, ConversationType type, List<LlmMessage> llmMessages,
                               Map<String, LegalSource> legalSources) {
    }

    private record ParsedMeta(List<Map<String, Object>> cards, List<Map<String, Object>> citations, List<String> quickReplies,
                              AssistantActionType actionType, Map<String, Object> actionPayload) {
    }

    private record SavedAnswer(Long messageId, Long actionId) {
    }

    private record ReplyDraftContext(String caseContext, String landlordText, String intent) {
    }

    private record ActionOutcome(AssistantActionType type, String chosen, Map<String, Object> prefill, String messageBody,
                                 CaseUpdateResponse caseUpdate, ReplyDraftContext replyDraftContext) {
    }
}
