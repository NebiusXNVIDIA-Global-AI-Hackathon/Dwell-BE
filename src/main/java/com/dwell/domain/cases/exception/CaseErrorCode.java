package com.dwell.domain.cases.exception;

import com.dwell.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CaseErrorCode implements BaseErrorCode {

    CASE_NOT_FOUND("C001", "Case not found.", HttpStatus.NOT_FOUND),
    NOT_CASE_OWNER("C002", "You can only access your own cases.", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
