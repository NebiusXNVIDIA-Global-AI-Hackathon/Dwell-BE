package com.dwell.domain.admin.exception;

import com.dwell.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AdminErrorCode implements BaseErrorCode {

    JOB_NOT_SUPPORTED("A001", "This job is not supported. Available jobs: sla-check.", HttpStatus.BAD_REQUEST),
    DEMO_DISABLED("A002", "Demo time shift is disabled on this server.", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
