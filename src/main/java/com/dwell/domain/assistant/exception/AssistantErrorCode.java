package com.dwell.domain.assistant.exception;

import com.dwell.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AssistantErrorCode implements BaseErrorCode {

    CONVERSATION_NOT_FOUND("A001", "Conversation not found.", HttpStatus.NOT_FOUND),
    CONVERSATION_ACCESS_DENIED("A002", "You can only access your own conversations.", HttpStatus.FORBIDDEN),
    ACTION_NOT_FOUND("A003", "Assistant action not found.", HttpStatus.NOT_FOUND),
    ACTION_ALREADY_CHOSEN("A004", "This action has already been chosen.", HttpStatus.CONFLICT),
    AI_RESPONSE_FAILED("A005", "Failed to get a response from the AI.", HttpStatus.BAD_GATEWAY);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
