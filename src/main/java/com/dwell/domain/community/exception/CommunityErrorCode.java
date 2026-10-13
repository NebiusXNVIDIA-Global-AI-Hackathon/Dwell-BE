package com.dwell.domain.community.exception;

import com.dwell.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CommunityErrorCode implements BaseErrorCode {

    PLACE_NOT_REGISTERED("CM001", "Register your home address before posting.", HttpStatus.BAD_REQUEST),
    TOO_MANY_TAGS("CM002", "You can add up to 5 tags.", HttpStatus.BAD_REQUEST),
    INVALID_POST_IMAGE("CM003", "Some images are invalid or already used.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
