package com.dwell.domain.assistant.service;

import com.dwell.domain.assistant.dto.response.CaseChatResponse;
import com.dwell.domain.assistant.dto.response.ConversationListResponse;
import com.dwell.domain.assistant.dto.response.OtherChatResponse;
import com.dwell.domain.assistant.entity.Conversation;
import com.dwell.domain.assistant.entity.Message;
import com.dwell.domain.assistant.enums.ConversationType;
import com.dwell.domain.assistant.enums.MessageRole;
import com.dwell.domain.assistant.repository.ConversationRepository;
import com.dwell.domain.assistant.repository.MessageRepository;
import com.dwell.domain.cases.entity.Case;
import com.dwell.domain.cases.enums.CaseStage;
import com.dwell.global.common.enums.IssueType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

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
}
