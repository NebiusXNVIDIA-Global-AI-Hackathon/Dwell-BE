package com.dwell.global.exception;

import com.dwell.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GlobalErrorCode implements BaseErrorCode {

    INVALID_INPUT_VALUE("G001", "Invalid input value.", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("G002", "The requested resource was not found.", HttpStatus.NOT_FOUND),
    INTERNAL_SERVER_ERROR("G003", "An internal server error occurred.", HttpStatus.INTERNAL_SERVER_ERROR),
    METHOD_NOT_ALLOWED("G004", "HTTP method not supported.", HttpStatus.METHOD_NOT_ALLOWED),
    INVALID_JSON_FORMAT("G005", "Malformed JSON request body.", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("G006", "Authentication is required.", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("G007", "Access denied.", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus status;

}
