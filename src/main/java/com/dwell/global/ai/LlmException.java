package com.dwell.global.ai;

// LLM 호출 실패 (키 없음, HTTP 오류, 응답 형식 오류, 타임아웃)
public class LlmException extends RuntimeException {

    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
