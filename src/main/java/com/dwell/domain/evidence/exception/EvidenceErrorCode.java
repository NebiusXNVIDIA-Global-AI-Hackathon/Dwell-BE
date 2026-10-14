package com.dwell.domain.evidence.exception;

import com.dwell.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum EvidenceErrorCode implements BaseErrorCode {

    EVIDENCE_NOT_FOUND("E001", "Evidence not found.", HttpStatus.NOT_FOUND),
    NOT_EVIDENCE_OWNER("E002", "You can only access your own evidence.", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
