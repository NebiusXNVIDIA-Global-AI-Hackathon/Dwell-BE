package com.dwell.domain.assistant.service;

import com.dwell.domain.assistant.dto.request.ActionChooseRequest;
import com.dwell.domain.assistant.dto.request.MessageSendRequest;
import com.dwell.domain.assistant.dto.response.ActionResultResponse;
import com.dwell.domain.assistant.entity.AssistantAction;
import com.dwell.domain.assistant.entity.Conversation;
import com.dwell.domain.assistant.enums.AssistantActionType;
import com.dwell.domain.assistant.exception.AssistantErrorCode;
import com.dwell.domain.assistant.repository.AssistantActionRepository;
import com.dwell.domain.assistant.repository.ConversationRepository;
import com.dwell.domain.assistant.repository.MessageRepository;
import com.dwell.domain.cases.service.CaseEventService;
import com.dwell.domain.file.repository.FileRepository;
import com.dwell.domain.notice.repository.LandlordResponseRepository;
import com.dwell.domain.user.entity.User;
import com.dwell.global.ai.NebiusChatClient;
import com.dwell.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AssistantChatServiceTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private AssistantActionRepository assistantActionRepository;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private LandlordResponseRepository landlordResponseRepository;
    @Mock
    private CaseEventService caseEventService;
    @Mock
    private NebiusChatClient chatClient;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private AssistantMessageMapper messageMapper;

    private AssistantChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new AssistantChatService(conversationRepository, messageRepository, assistantActionRepository, fileRepository,
                landlordResponseRepository, caseEventService, chatClient, JsonMapper.builder().build(), transactionTemplate, messageMapper);
    }

    @Test
    @DisplayName("메시지 전송: NEBIUS_API_KEY가 없으면 스트림을 열기 전에 AI_NOT_CONFIGURED")
    void sendMessageWithoutKey() {
        given(chatClient.isConfigured()).willReturn(false);
        MessageSendRequest request = new MessageSendRequest();
        request.setContent("hi");

        assertThatThrownBy(() -> chatService.sendMessage(1L, 21L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(AssistantErrorCode.AI_NOT_CONFIGURED);
        verifyNoInteractions(messageRepository);
    }

    @Test
    @DisplayName("액션: 이미 고른 액션이면 ACTION_ALREADY_CHOSEN")
    void chooseActionAlreadyChosen() {
        runTransactionsInline();
        ownConversation();
        AssistantAction action = action(AssistantActionType.CREATE_CASE, Map.of());
        action.choose("NO", null);
        given(assistantActionRepository.findByIdAndMessageConversationId(90L, 21L)).willReturn(Optional.of(action));

        assertThatThrownBy(() -> chatService.chooseAction(1L, 21L, 90L, choose("YES", null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(AssistantErrorCode.ACTION_ALREADY_CHOSEN);
    }

    @Test
    @DisplayName("액션: 선택지에 없는 key면 INVALID_CHOICE")
    void chooseActionInvalidChoice() {
        runTransactionsInline();
        ownConversation();
        given(assistantActionRepository.findByIdAndMessageConversationId(90L, 21L))
                .willReturn(Optional.of(action(AssistantActionType.CREATE_CASE, Map.of())));

        assertThatThrownBy(() -> chatService.chooseAction(1L, 21L, 90L, choose("MAYBE", null)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(AssistantErrorCode.INVALID_CHOICE);
    }

    @Test
    @DisplayName("액션: CREATE_CASE + YES면 저장된 prefill을 돌려줌")
    void chooseCreateCase() {
        runTransactionsInline();
        ownConversation();
        AssistantAction action = action(AssistantActionType.CREATE_CASE,
                Map.of("prefill", Map.of("issueCategory", "MOLD_MOISTURE", "location", "BATHROOM")));
        given(assistantActionRepository.findByIdAndMessageConversationId(90L, 21L)).willReturn(Optional.of(action));

        ActionResultResponse response = chatService.chooseAction(1L, 21L, 90L, choose("YES", null));

        assertThat(response.getPrefill()).containsEntry("issueCategory", "MOLD_MOISTURE");
        assertThat(action.getChosen()).isEqualTo("YES");
        verifyNoInteractions(caseEventService, chatClient);
    }

    @Test
    @DisplayName("액션: SEND_MESSAGE + YES면 고친 본문(editedBody)을 보낼 본문으로 돌려주고 기록")
    void chooseSendMessageEdited() {
        runTransactionsInline();
        ownConversation();
        AssistantAction action = action(AssistantActionType.SEND_MESSAGE, Map.of("draftBody", "Original draft"));
        given(assistantActionRepository.findByIdAndMessageConversationId(90L, 21L)).willReturn(Optional.of(action));

        ActionResultResponse response = chatService.chooseAction(1L, 21L, 90L, choose("YES", " Tuesday works. "));

        assertThat(response.getMessageBody()).isEqualTo("Tuesday works.");
        assertThat(action.getPayload()).containsEntry("sentBody", "Tuesday works.");
    }

    @SuppressWarnings("unchecked")
    private void runTransactionsInline() {
        given(transactionTemplate.execute(any())).willAnswer(invocation ->
                ((TransactionCallback<Object>) invocation.getArgument(0)).doInTransaction(null));
    }

    private void ownConversation() {
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        Conversation conversation = mock(Conversation.class);
        given(conversation.getUser()).willReturn(user);
        given(conversationRepository.findById(21L)).willReturn(Optional.of(conversation));
    }

    private AssistantAction action(AssistantActionType type, Map<String, Object> payload) {
        return AssistantAction.builder()
                .type(type)
                .choices(List.of(Map.of("key", "YES", "label", "Yes"), Map.of("key", "NO", "label", "No")))
                .payload(payload)
                .build();
    }

    private ActionChooseRequest choose(String choice, String editedBody) {
        ActionChooseRequest request = new ActionChooseRequest();
        request.setChoice(choice);
        request.setEditedBody(editedBody);
        return request;
    }
}
