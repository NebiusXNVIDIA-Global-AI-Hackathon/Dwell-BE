package com.dwell.domain.auth.exception;

import com.dwell.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    INVALID_CREDENTIALS("A001", "The email or password you entered is incorrect.", HttpStatus.UNAUTHORIZED),
    DEMO_ACCOUNT_NOT_FOUND("A002", "Demo account not found.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
