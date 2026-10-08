package com.dwell.global.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(title = "BaseResponse DTO", description = "Common API response format")
public class BaseResponse<T> {

    @Schema(description = "Whether the request succeeded", example = "true")
    private boolean success;

    @Schema(description = "Response code (success: HTTP status code, failure: error code)", example = "200")
    private String code;

    @Schema(description = "Response message", example = "Request processed successfully.")
    private String message;

    @Schema(description = "Response data")
    private T data;

    // Success response - data only
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(true, "200", "Request processed successfully.", data);
    }

    // Success response - message and data
    public static <T> BaseResponse<T> success(String message, T data) {
        return new BaseResponse<>(true, "200", message, data);
    }

    // Success response - status code, message, and data
    public static <T> BaseResponse<T> success(int code, String message, T data) {
        return new BaseResponse<>(true, String.valueOf(code), message, data);
    }

    // Error response
    public static <T> BaseResponse<T> error(String code, String message) {
        return new BaseResponse<>(false, code, message, null);
    }
}
