package com.dwell.domain.assistant.service;

import com.dwell.domain.assistant.dto.request.ChatRoomCreateRequest;
import com.dwell.domain.assistant.dto.response.CaseChatResponse;
import com.dwell.domain.assistant.dto.response.CaseSummaryResponse;
import com.dwell.domain.assistant.dto.response.ChatRoomCreateResponse;
import com.dwell.domain.assistant.dto.response.ConversationListResponse;
import com.dwell.domain.assistant.dto.response.GreetingResponse;
import com.dwell.domain.assistant.dto.response.OtherChatResponse;
import com.dwell.domain.assistant.entity.Conversation;
import com.dwell.domain.assistant.entity.Message;
import com.dwell.domain.assistant.enums.ConversationIntent;
import com.dwell.domain.assistant.enums.ConversationType;
import com.dwell.domain.assistant.enums.MessageRole;
import com.dwell.domain.assistant.exception.AssistantErrorCode;
import com.dwell.domain.assistant.repository.ConversationRepository;
import com.dwell.domain.assistant.repository.MessageRepository;
import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.enums.CaseStage;
import com.dwell.domain.cases.repository.CaseRepository;
import com.dwell.domain.user.entity.User;
import com.dwell.domain.user.repository.UserRepository;
import com.dwell.global.exception.CustomException;
import com.dwell.global.common.enums.IssueType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AssistantServiceImpl implements AssistantService {

    private static final String ASSISTANT_NAME = "Deaver";
    private static final String DEFAULT_TITLE = "New Chat"; // Other 채팅방에서 제목 기본값
    private static final int TITLE_MAX_LENGTH = 30; // 제목 글자수 제한

    // Deaver 인사 메시지
    private static final String OTHER_GREETING = "Hi %s! Ask me anything about your home, your rights, or a message from your landlord. You can write in any language.";
    private static final List<String> OTHER_QUICK_REPLIES = List.of(
            "Can my landlord enter without notice?", "Translate a letter from my landlord", "How do I file with 311?");
    private static final String CASE_GREETING = "I've loaded your %s case (%s). What would you like to do first?";
    private static final List<String> CASE_QUICK_REPLIES = List.of(
            "Draft a notice to my landlord", "What are my rights?", "What should I do next?");
    private static final String REPLY_RECEIVED_GREETING = "Show me the message you received from your landlord. You can paste the text or upload a screenshot.";

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final CaseRepository caseRepository;
    private final UserRepository userRepository;

    // 채팅 목록 조회 서비스
    @Override
    public ConversationListResponse getChatRooms(Long userId) {

        log.info("[AssistantService] 채팅방 조회 서비스 - 시작: userId={}", userId);

        // 사용자의 전체 대화 조회 (케이스 함께 조회)
        List<Conversation> conversations = conversationRepository.findAllWithCaseByUserId(userId);

        // 채팅방이 없을 경우
        if (conversations.isEmpty()) {
            log.info("[AssistantService] 채팅방 조회 서비스 - 완료: 채팅방이 존재 하지 않습니다.");
            return ConversationListResponse.builder()
                    .caseChats(List.of())
                    .otherChats(List.of())
                    .build();
        }

        // 대화별 마지막 메시지, 첫 사용자 메시지 조회
        List<Long> conversationIds = conversations.stream().map(Conversation::getId).toList();
        Map<Long, Message> lastMessages = toMapByConversationId(
                messageRepository.findLastMessages(conversationIds));
        Map<Long, Message> firstUserMessages = toMapByConversationId(
                messageRepository.findFirstMessages(conversationIds, MessageRole.USER));

        // 최근 메시지 순 정렬 (메시지 없으면 대화 생성 시각 기준)
        Comparator<Conversation> latestFirst = Comparator.comparing(
                (Conversation c) -> sortKey(c, lastMessages.get(c.getId()))).reversed();

        // Case 채팅방 리스트: 해결된·삭제된 케이스 제외
        List<CaseChatResponse> caseChats = conversations.stream()
                .filter(c -> c.getType() == ConversationType.CASE && isActiveCase(c))
                .sorted(latestFirst)
                .map(c -> toCaseChat(c, lastMessages.get(c.getId())))
                .toList();

        // Other 채팅방 리스트
        List<OtherChatResponse> otherChats = conversations.stream()
                .filter(c -> c.getType() == ConversationType.OTHER)
                .sorted(latestFirst)
                .map(c -> toOtherChat(c, lastMessages.get(c.getId()), firstUserMessages.get(c.getId())))
                .toList();

        log.info("[AssistantService] 채팅방 조회 서비스 - 완료: caseChats 개수={}, otherChats 개수={}", caseChats.size(), otherChats.size());

        // 응답 반환
        return ConversationListResponse.builder()
                .caseChats(caseChats)
                .otherChats(otherChats)
                .build();
    }


    // 채팅방 생성 서비스
    @Override
    @Transactional
    public ChatRoomCreateResponse createChatRoom(Long userId, ChatRoomCreateRequest request) {

        Long caseId = request != null ? request.getCaseId() : null;
        ConversationIntent intent = request != null ? request.getIntent() : null;

        log.info("[AssistantService] 채팅방 생성 서비스 - 시작: userId={}, caseId={}, intent={}", userId, caseId, intent);

        User user = userRepository.getReferenceById(userId);

        // caseId 없으면 Other 채팅방 생성
        if (caseId == null) {
            Conversation conversation = conversationRepository.save(Conversation.builder()
                    .user(user)
                    .type(ConversationType.OTHER)
                    .build());

            Message greeting = intent == ConversationIntent.REPLY_RECEIVED
                    ? saveGreeting(conversation, REPLY_RECEIVED_GREETING, null)
                    : saveGreeting(conversation, OTHER_GREETING.formatted(user.getNickname()), OTHER_QUICK_REPLIES);

            log.info("[AssistantService] 채팅방 생성 서비스 - 완료: Other 채팅방 생성, conversationId={}", conversation.getId());
            return toCreateResponse(conversation, greeting, true);
        }

        // 케이스 조회 (삭제된 케이스는 없는 것으로 처리)
        Case caseEntity = caseRepository.findById(caseId)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> {
                    log.warn("[AssistantService] 채팅방 생성 서비스 - 케이스를 찾을 수 없습니다: caseId={}", caseId);
                    return new CustomException(AssistantErrorCode.CASE_NOT_FOUND);
                });

        // 본인 케이스인지 확인
        if (!caseEntity.getUser().getId().equals(userId)) {
            log.warn("[AssistantService] 채팅방 생성 서비스 - 본인 케이스가 아닙니다: userId={}, caseId={}", userId, caseId);
            throw new CustomException(AssistantErrorCode.CASE_ACCESS_DENIED);
        }

        // 케이스당 채팅방 1개 -> 이미 있으면 기존 채팅방 반환
        Optional<Conversation> existing = conversationRepository.findByCaseEntityId(caseId);
        if (existing.isPresent()) {
            Conversation conversation = existing.get();

            // intent로 진입하면 새 안내 메시지 추가, 아니면 처음 인사 메시지 반환
            Message greeting = intent == ConversationIntent.REPLY_RECEIVED
                    ? saveGreeting(conversation, REPLY_RECEIVED_GREETING, null)
                    : messageRepository.findFirstByConversationIdAndRoleOrderByIdAsc(
                            conversation.getId(), MessageRole.ASSISTANT).orElse(null);

            log.info("[AssistantService] 채팅방 생성 서비스 - 완료: 기존 Case 채팅방 반환, conversationId={}", conversation.getId());
            return toCreateResponse(conversation, greeting, false);
        }

        // Case 채팅방 생성 (동시 요청으로 unique 제약 위반 시 409)
        Conversation conversation;
        try {
            conversation = conversationRepository.saveAndFlush(Conversation.builder()
                    .user(user)
                    .type(ConversationType.CASE)
                    .caseEntity(caseEntity)
                    .build());
        } catch (DataIntegrityViolationException e) {
            log.warn("[AssistantService] 채팅방 생성 서비스 - 동시 생성 충돌: caseId={}", caseId);
            throw new CustomException(AssistantErrorCode.CHAT_ROOM_CREATE_CONFLICT);
        }

        Message greeting = intent == ConversationIntent.REPLY_RECEIVED
                ? saveGreeting(conversation, REPLY_RECEIVED_GREETING, null)
                : saveGreeting(conversation,
                        CASE_GREETING.formatted(issueLabel(caseEntity.getIssueType()), caseEntity.getCaseNumber()),
                        CASE_QUICK_REPLIES);

        log.info("[AssistantService] 채팅방 생성 서비스 - 완료: Case 채팅방 생성, conversationId={}", conversation.getId());
        return toCreateResponse(conversation, greeting, true);
    }


    // 채팅방 삭제 서비스
    @Override
    @Transactional
    public void deleteChatRoom(Long userId, Long conversationId) {

        log.info("[AssistantService] 채팅방 삭제 서비스 - 시작: userId={}, conversationId={}", userId, conversationId);

        // 채팅방 조회
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> {
                    log.warn("[AssistantService] 채팅방 삭제 서비스 - 채팅방을 찾을 수 없습니다: conversationId={}", conversationId);
                    return new CustomException(AssistantErrorCode.CONVERSATION_NOT_FOUND);
                });

        // 본인 채팅방인지 확인
        if (!conversation.getUser().getId().equals(userId)) {
            log.warn("[AssistantService] 채팅방 삭제 서비스 - 본인 채팅방이 아닙니다: userId={}, conversationId={}", userId, conversationId);
            throw new CustomException(AssistantErrorCode.CONVERSATION_ACCESS_DENIED);
        }

        // 채팅방 삭제 (메시지·액션은 DB ON DELETE CASCADE로 함께 삭제, 케이스는 유지)
        conversationRepository.delete(conversation);

        log.info("[AssistantService] 채팅방 삭제 서비스 - 완료: conversationId={}", conversationId);
    }



    // ========== 헬퍼 함수 ==========

    // 메시지 목록 -> { 대화 ID : 메시지 } Map
    private Map<Long, Message> toMapByConversationId(List<Message> messages) {
        return messages.stream()
                .collect(Collectors.toMap(m -> m.getConversation().getId(), Function.identity()));
    }

    // 진행 중인 케이스인지 (삭제·RESOLVED 제외)
    private boolean isActiveCase(Conversation conversation) {
        Case caseEntity = conversation.getCaseEntity();

        // CASE 채팅방에서 해당 case가 존재 하는지 조회
        if (caseEntity == null) {
            log.warn("[AssistantService] 채팅방 조회 서비스 - CASE 대화방이지만 CASE를 찾을 수 없습니다.: conversationId={}",
                    conversation.getId());
            return false;
        }
        return caseEntity.getDeletedAt() == null
                && caseEntity.getStage() != CaseStage.RESOLVED;
    }

    // 정렬 기준 시각 (메시지 없으면 대화 생성 시각)
    private LocalDateTime sortKey(Conversation conversation, Message lastMessage) {
        return lastMessage != null ? lastMessage.getCreatedAt() : conversation.getCreatedAt();
    }

    // 케이스 대화 -> DTO (제목: Water Leak · C-006)
    private CaseChatResponse toCaseChat(Conversation conversation, Message lastMessage) {
        Case caseEntity = conversation.getCaseEntity();

        return CaseChatResponse.builder()
                .conversationId(conversation.getId())
                .caseId(caseEntity.getId())
                .title(issueLabel(caseEntity.getIssueType()) + " · " + caseEntity.getCaseNumber())
                .lastMessage(preview(lastMessage))
                .lastMessageAt(toInstant(lastMessage))
                .build();
    }

    // 일반 대화 -> DTO (제목: 첫 사용자 메시지 앞 30자)
    private OtherChatResponse toOtherChat(Conversation conversation, Message lastMessage, Message firstUserMessage) {

        // 첫 사용자 메시지 앞 30자, 없으면 기본 제목
        String title = firstUserMessage != null
                ? truncate(firstUserMessage.getContent().strip(), TITLE_MAX_LENGTH)
                : DEFAULT_TITLE;

        return OtherChatResponse.builder()
                .conversationId(conversation.getId())
                .title(title)
                .lastMessage(preview(lastMessage))
                .lastMessageAt(toInstant(lastMessage))
                .build();
    }

    // 미리보기 문구 (AI 답변이면 "Deaver: " 접두어)
    private String preview(Message message) {
        if (message == null) {
            return null;
        }
        return message.getRole() == MessageRole.ASSISTANT
                ? ASSISTANT_NAME + ": " + message.getContent()
                : message.getContent();
    }

    // 메시지 시각 -> UTC (예: 2026-10-07T09:58:00Z)
    private Instant toInstant(Message message) {
        if (message == null) {
            return null;
        }
        return message.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant();
    }

    // 최대 길이까지 자르기
    private String truncate(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    // 이슈 유형 -> 표시 문구 (WATER_LEAK -> Water Leak)
    private String issueLabel(IssueType issueType) {
        return Arrays.stream(issueType.name().split("_"))
                .map(word -> word.charAt(0) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    // Deaver 인사 메시지 저장
    private Message saveGreeting(Conversation conversation, String content, List<String> quickReplies) {
        return messageRepository.save(Message.builder()
                .conversation(conversation)
                .role(MessageRole.ASSISTANT)
                .content(content)
                .quickReplies(quickReplies)
                .build());
    }

    // 채팅방 생성 결과 -> DTO
    private ChatRoomCreateResponse toCreateResponse(Conversation conversation, Message greeting, boolean created) {
        Case caseEntity = conversation.getCaseEntity();

        CaseSummaryResponse caseSummary = caseEntity == null ? null : CaseSummaryResponse.builder()
                .caseId(caseEntity.getId())
                .caseNumber(caseEntity.getCaseNumber())
                .title(caseEntity.getTitle())
                .location(caseEntity.getLocation())
                .statusBadge(statusBadge(caseEntity.getStage()))
                .build();

        GreetingResponse greetingResponse = greeting == null ? null : GreetingResponse.builder()
                .messageId(greeting.getId())
                .content(greeting.getContent())
                .quickReplies(greeting.getQuickReplies())
                .build();

        return ChatRoomCreateResponse.builder()
                .conversationId(conversation.getId())
                .type(conversation.getType())
                .caseSummary(caseSummary)
                .greeting(greetingResponse)
                .created(created)
                .build();
    }

    // 케이스 단계 -> 상태 배지 (LOGGED / IN_PROGRESS / RESOLVED)
    private String statusBadge(CaseStage stage) {
        return switch (stage) {
            case LOGGED -> "LOGGED";
            case RESOLVED -> "RESOLVED";
            default -> "IN_PROGRESS";
        };
    }
}
