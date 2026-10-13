package com.dwell.domain.cases.exception;

import com.dwell.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CaseErrorCode implements BaseErrorCode {

    CASE_NOT_FOUND("C001", "Case not found.", HttpStatus.NOT_FOUND),
    NOT_CASE_OWNER("C002", "You can only access your own cases.", HttpStatus.FORBIDDEN),
    INVALID_TRANSITION("C003", "This event is not allowed in the current case stage.", HttpStatus.CONFLICT),
    INVALID_PAYLOAD("C004", "Required payload is missing or has an invalid format.", HttpStatus.BAD_REQUEST),
    EVENT_TYPE_NOT_ALLOWED("C005", "This event type cannot be recorded through this API.", HttpStatus.BAD_REQUEST),
    INVALID_OCCURRED_AT("C006", "occurredAt must be a valid ISO-8601 time, not in the future, and not earlier than the last case event.", HttpStatus.BAD_REQUEST),
    INVALID_AFFECTED_AREA("C007", "The affected area is not available for the selected location.", HttpStatus.BAD_REQUEST),
    OTHER_TEXT_REQUIRED("C008", "Please enter a value when Other is selected.", HttpStatus.BAD_REQUEST),
    EVIDENCE_REQUIRED("C009", "At least one evidence file is required.", HttpStatus.BAD_REQUEST),
    INVALID_ISSUE_DETAIL("C010", "The specific issue is not available for the selected issue category.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
