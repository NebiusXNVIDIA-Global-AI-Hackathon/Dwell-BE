package com.dwell.global.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/*
Nebius Token Factory (OpenAI 호환 /chat/completions) 클라이언트
- 환경변수 NEBIUS_API_KEY만 넣으면 동작 (주소·모델은 NEBIUS_BASE_URL, NEBIUS_MODEL로 바꿀 수 있음)
- 기본 모델: nvidia/NVIDIA-Nemotron-3-Nano-30B-A3B
- 추론 모델이라 reasoning_content와 <think>...</think>는 버리고 최종 답변만 넘김
 */
@Component
@Slf4j
public class NebiusChatClient {

    private static final String THINK_START = "<think>";
    private static final String THINK_END = "</think>";

    private final JsonMapper jsonMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final double temperature;
    private final int maxTokens;
    private final Duration timeout;

    public NebiusChatClient(JsonMapper jsonMapper,
                            @Value("${nebius.api-key:}") String apiKey,
                            @Value("${nebius.base-url:https://api.tokenfactory.nebius.com/v1}") String baseUrl,
                            @Value("${nebius.model:nvidia/NVIDIA-Nemotron-3-Nano-30B-A3B}") String model,
                            @Value("${nebius.temperature:0.3}") double temperature,
                            @Value("${nebius.max-tokens:4096}") int maxTokens,
                            @Value("${nebius.timeout-seconds:120}") long timeoutSeconds) {
        this.jsonMapper = jsonMapper;
        this.apiKey = apiKey == null ? "" : apiKey.strip();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    // 키가 설정됐는지 (없으면 AI 기능 비활성)
    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    // 스트리밍 호출: 최종 답변 조각(delta)을 받는 대로 onDelta로 넘김 (호출한 스레드에서 끝날 때까지 블로킹)
    public void stream(List<LlmMessage> messages, Consumer<String> onDelta) {
        if (!isConfigured()) {
            throw new LlmException("NEBIUS_API_KEY is not set.");
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
                .timeout(timeout)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(toRequestBody(messages)))
                .build();

        ThinkFilter thinkFilter = new ThinkFilter(onDelta);
        try {
            HttpResponse<Stream<String>> response = httpClient.send(request, HttpResponse.BodyHandlers.ofLines());
            if (response.statusCode() / 100 != 2) {
                String body = String.join("\n", response.body().limit(20).toList());
                log.error("[NebiusChatClient] 호출 실패: status={}, body={}", response.statusCode(), body);
                throw new LlmException("LLM request failed with status " + response.statusCode());
            }
            try (Stream<String> lines = response.body()) {
                lines.forEach(line -> handleLine(line, thinkFilter));
            }
            thinkFilter.finish();
        } catch (IOException e) {
            throw new LlmException("LLM request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("LLM request was interrupted.", e);
        }
    }

    // 한 번에 받는 호출 (스트리밍 결과를 모아서 반환)
    public String complete(List<LlmMessage> messages) {
        StringBuilder answer = new StringBuilder();
        stream(messages, answer::append);
        return answer.toString().strip();
    }

    // ========== 헬퍼 함수 ==========

    private String toRequestBody(List<LlmMessage> messages) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages.stream().map(m -> Map.of("role", m.role(), "content", m.content())).toList());
        body.put("stream", true);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        return jsonMapper.writeValueAsString(body);
    }

    // SSE 한 줄 처리 (data: {...} / data: [DONE]), 답변(content)만 넘기고 추론(reasoning_content)은 버림
    private void handleLine(String line, ThinkFilter thinkFilter) {
        if (!line.startsWith("data:")) {
            return;
        }
        String data = line.substring(5).strip();
        if (data.isEmpty() || data.equals("[DONE]")) {
            return;
        }
        JsonNode chunk = jsonMapper.readTree(data);
        JsonNode choices = chunk.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return;
        }
        JsonNode content = choices.get(0).path("delta").path("content");
        if (content.isString() && !content.asString().isEmpty()) {
            thinkFilter.accept(content.asString());
        }
    }

    // content 안에 섞여 오는 <think>...</think> 구간을 걸러냄 (태그가 조각 사이에 잘려 와도 처리)
    private static final class ThinkFilter {

        private final Consumer<String> downstream;
        private final StringBuilder pending = new StringBuilder();
        private boolean thinking;

        private ThinkFilter(Consumer<String> downstream) {
            this.downstream = downstream;
        }

        void accept(String delta) {
            pending.append(delta);
            while (true) {
                String tag = thinking ? THINK_END : THINK_START;
                int index = pending.indexOf(tag);
                if (index >= 0) {
                    if (!thinking && index > 0) {
                        downstream.accept(pending.substring(0, index));
                    }
                    pending.delete(0, index + tag.length());
                    thinking = !thinking;
                    continue;
                }
                // 태그 앞부분이 끝에 걸려 있을 수 있으니 그만큼은 남겨 둠
                int keep = Math.min(pending.length(), tag.length() - 1);
                int flush = pending.length() - keep;
                if (!thinking && flush > 0) {
                    downstream.accept(pending.substring(0, flush));
                }
                if (flush > 0) {
                    pending.delete(0, flush);
                }
                return;
            }
        }

        // 스트림 끝: 태그 확인용으로 남겨 둔 글자 내보내기
        void finish() {
            if (!thinking && !pending.isEmpty()) {
                downstream.accept(pending.toString());
            }
            pending.setLength(0);
        }
    }
}
