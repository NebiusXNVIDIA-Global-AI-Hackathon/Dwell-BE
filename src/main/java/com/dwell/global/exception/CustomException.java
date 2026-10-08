package com.dwell.global.exception;

import com.dwell.global.exception.model.BaseErrorCode;
import lombok.Getter;

@Getter
// CustomException is thrown at runtime (unchecked)
public class CustomException extends RuntimeException {
    private final BaseErrorCode errorCode;

    public CustomException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
