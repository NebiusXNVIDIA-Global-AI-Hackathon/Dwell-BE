package com.dwell.domain.building.exception;

import com.dwell.global.exception.model.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BuildingErrorCode implements BaseErrorCode {

    PLACE_NOT_REGISTERED("B001", "Register your home address first.", HttpStatus.BAD_REQUEST),
    SIGNAL_NOT_FOUND("B002", "Building signal not found.", HttpStatus.NOT_FOUND),
    INVALID_SYMPTOM("B003", "Some symptoms are not part of this checklist.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
