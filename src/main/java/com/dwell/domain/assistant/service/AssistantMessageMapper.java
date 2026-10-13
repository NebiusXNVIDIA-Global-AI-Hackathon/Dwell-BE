package com.dwell.domain.assistant.service;

import com.dwell.domain.assistant.dto.response.ActionResponse;
import com.dwell.domain.assistant.dto.response.AttachmentResponse;
import com.dwell.domain.assistant.dto.response.MessageResponse;
import com.dwell.domain.assistant.entity.AssistantAction;
import com.dwell.domain.assistant.entity.Message;
import com.dwell.domain.file.entity.File;
import com.dwell.domain.file.repository.FileRepository;
import com.dwell.domain.file.service.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

// 메시지 엔티티 -> 응답 DTO (대화 조회 · 액션 실행 결과에서 같이 사용)
@Component
@RequiredArgsConstructor
public class AssistantMessageMapper {

    private final FileRepository fileRepository;
    private final S3Uploader s3Uploader;

    // 메시지들 -> DTO (첨부 파일은 한 번에 조회, 메시지 순서 유지)
    public List<MessageResponse> toResponses(List<Message> messages, Map<Long, AssistantAction> actionsByMessageId) {
        List<Long> fileIds = messages.stream()
                .map(Message::getAttachments)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .distinct()
                .toList();
        Map<Long, File> files = fileIds.isEmpty() ? Map.of() : fileRepository.findAllById(fileIds).stream()
                .collect(Collectors.toMap(File::getId, Function.identity()));

        return messages.stream()
                .map(message -> toResponse(message, actionsByMessageId.get(message.getId()), files))
                .toList();
    }

    public MessageResponse toResponse(Message message, AssistantAction action, Map<Long, File> files) {
        List<AttachmentResponse> attachments = message.getAttachments() == null ? List.of() : message.getAttachments().stream()
                .map(files::get)
                .filter(Objects::nonNull)
                .map(file -> AttachmentResponse.builder()
                        .fileId(file.getId())
                        .mediaType(file.getMediaType())
                        .url(s3Uploader.getPresignedUrl(file.getStorageKey()))
                        .build())
                .toList();

        return MessageResponse.builder()
                .messageId(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .cards(message.getCards() == null ? List.of() : message.getCards())
                .citations(message.getCitations() == null ? List.of() : message.getCitations())
                .quickReplies(message.getQuickReplies() == null ? List.of() : message.getQuickReplies())
                .action(action == null ? null : ActionResponse.builder()
                        .actionId(action.getId())
                        .type(action.getType())
                        .choices(action.getChoices())
                        .chosen(action.getChosen())
                        .build())
                .attachments(attachments)
                .createdAt(message.getCreatedAt())
                .build();
    }
}
