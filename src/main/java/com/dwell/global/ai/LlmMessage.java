package com.dwell.global.ai;

// OpenAI 형식 chat 메시지 (role: system / user / assistant)
public record LlmMessage(String role, String content) {

    public static LlmMessage system(String content) {
        return new LlmMessage("system", content);
    }

    public static LlmMessage user(String content) {
        return new LlmMessage("user", content);
    }

    public static LlmMessage assistant(String content) {
        return new LlmMessage("assistant", content);
    }
}
