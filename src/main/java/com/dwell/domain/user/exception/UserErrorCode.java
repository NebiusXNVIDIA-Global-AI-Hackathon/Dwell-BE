package com.dwell.domain.user.exception;

import com.dwell.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements BaseErrorCode {

    DUPLICATE_EMAIL("U001", "This email is already in use.", HttpStatus.CONFLICT),
    DUPLICATE_NICKNAME("U002", "This nickname is already in use.", HttpStatus.CONFLICT),
    PASSWORD_MISMATCH("U003", "Passwords do not match.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
