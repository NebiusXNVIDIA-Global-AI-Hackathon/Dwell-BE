package com.dwell.domain.file.exception;

import com.dwell.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum FileErrorCode implements BaseErrorCode {

    EMPTY_FILE("F001", "The file is empty.", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_FILE_TYPE("F002", "Only photos, videos, audio, and PDF or Word documents are allowed.", HttpStatus.BAD_REQUEST),
    FILE_NOT_FOUND("F003", "File not found.", HttpStatus.NOT_FOUND),
    FILE_ALREADY_LINKED("F004", "This file is already linked to other evidence.", HttpStatus.CONFLICT),
    NOT_EVIDENCE_FILE("F005", "Only files uploaded as evidence can be attached to a case.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
