package com.dwell.global.exception;

import com.dwell.global.common.BaseResponse;
import com.dwell.global.exception.model.BaseErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
// Handles exceptions thrown from controllers
public class GlobalExceptionHandler {

    // 1. Custom exception handler
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<BaseResponse<Object>> handleCustomException(CustomException ex) {
        BaseErrorCode errorCode = ex.getErrorCode();
        log.warn("CustomException 발생: {} - {}", errorCode.getCode(), errorCode.getMessage());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(BaseResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

    // 2. @Valid validation failure handler
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<?>> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessages = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> String.format("[%s] %s", e.getField(), e.getDefaultMessage()))
                .collect(Collectors.joining(", "));

        log.warn("Validation 오류 발생: {}", errorMessages);

        return ResponseEntity
                .badRequest()
                .body(BaseResponse.error(GlobalErrorCode.INVALID_INPUT_VALUE.getCode(), errorMessages));
    }

    // 3. Unsupported HTTP method handler (405 Method Not Allowed)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<BaseResponse<?>> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        log.warn("지원하지 않는 HTTP 메서드 요청: {}", ex.getMethod());

        return ResponseEntity
                .status(GlobalErrorCode.METHOD_NOT_ALLOWED.getStatus())
                .body(BaseResponse.error(
                        GlobalErrorCode.METHOD_NOT_ALLOWED.getCode(),
                        GlobalErrorCode.METHOD_NOT_ALLOWED.getMessage()
                ));
    }

    // 4. Malformed JSON request body handler (400 Bad Request)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<?>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("JSON 파싱 오류 발생: {}", ex.getMessage());

        return ResponseEntity
                .status(GlobalErrorCode.INVALID_JSON_FORMAT.getStatus())
                .body(BaseResponse.error(
                        GlobalErrorCode.INVALID_JSON_FORMAT.getCode(),
                        GlobalErrorCode.INVALID_JSON_FORMAT.getMessage()
                ));
    }

    // 5. Missing / type-mismatched request parameter handler (400 Bad Request)
    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<BaseResponse<?>> handleInvalidParameterException(Exception ex) {
        log.warn("요청 파라미터 오류: {}", ex.getMessage());
        return toResponse(GlobalErrorCode.INVALID_INPUT_VALUE);
    }

    // 6. Unknown URL handler (404 Not Found)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<BaseResponse<?>> handleNoResourceFoundException(NoResourceFoundException ex) {
        log.warn("존재하지 않는 경로 요청: {}", ex.getResourcePath());
        return toResponse(GlobalErrorCode.RESOURCE_NOT_FOUND);
    }

    // 7. Authentication failure handler (401 Unauthorized) - delegated from SecurityConfig's authenticationEntryPoint
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<BaseResponse<?>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("인증 실패: {}", ex.getMessage());
        return toResponse(GlobalErrorCode.UNAUTHORIZED);
    }

    // 8. Access denied handler (403 Forbidden) - includes @PreAuthorize failures
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseResponse<?>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("접근 거부: {}", ex.getMessage());
        return toResponse(GlobalErrorCode.FORBIDDEN);
    }

    // 9. Fallback handler for all other unexpected exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<?>> handleException(Exception ex) {
        log.error("Server 오류 발생: ", ex);

        return ResponseEntity
                .status(GlobalErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(BaseResponse.error(
                        GlobalErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                        GlobalErrorCode.INTERNAL_SERVER_ERROR.getMessage()
                ));
    }

    private ResponseEntity<BaseResponse<?>> toResponse(GlobalErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(BaseResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }
}
