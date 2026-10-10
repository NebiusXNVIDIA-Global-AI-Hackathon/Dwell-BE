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
    AI_RESPONSE_FAILED("A005", "Failed to get a response from the AI.", HttpStatus.BAD_GATEWAY),
    CASE_NOT_FOUND("A006", "Case not found.", HttpStatus.NOT_FOUND),
    CASE_ACCESS_DENIED("A007", "You can only chat about your own cases.", HttpStatus.FORBIDDEN),
    CHAT_ROOM_CREATE_CONFLICT("A008", "A chat for this case is already being created. Please try again.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
