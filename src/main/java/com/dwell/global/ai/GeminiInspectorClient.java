package com.dwell.global.ai;

import com.dwell.domain.analysis.dto.VisionObservation;
import com.dwell.domain.analysis.enums.AssessedSeverity;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
튜닝한 Gemini(Vertex AI 엔드포인트) 클라이언트 — 사진·영상 1개를 보고 하자 1차 판정(VisionObservation)
- 환경변수 GEMINI_ENDPOINT_URL(…/endpoints/{id}:generateContent)이 없으면 비활성
- 인증: Application Default Credentials (GOOGLE_APPLICATION_CREDENTIALS 서비스 계정 키 또는 gcloud ADC)
- 시스템 지시문은 튜닝 데이터(build_tuning_set.py의 SYSTEM)와 글자 하나 다르지 않게 유지해야 튜닝 효과가 남음
- 사진은 긴 변 1024px로 줄여서, 영상은 그대로 inline base64로 보냄
- 응답이 JSON이 아니거나 값이 틀리면 1회 재시도, 그래도 실패하면 Inconclusive
 */
@Component
@Slf4j
public class GeminiInspectorClient {

    static final String SYSTEM = """
            You analyze a tenant's photo of a housing problem in a NYC apartment.
            Return JSON only, describing what is visible. This is a repair-priority assessment, not legal advice.
            defect_type: water_stain_or_leak | mold | both_leak_and_mold | crack | none | other_defect
            severity (from the photo only):
            - High: active dripping or wet surface; sagging, bulging or collapsing ceiling/wall; water near lights
              or outlets; mold over a large area (~30+ sq ft) or across multiple surfaces; wide/displaced crack.
            - Medium: large or spreading dry stain; moisture bubbling/peeling paint; moderate mold (hand-sized
              patches up to ~10-30 sq ft); long, branching or multiple cracks.
            - Low: small dry old stain or discoloration; a few small mold spots or mold only on caulk/grout;
              hairline crack. Use Low when no defect is visible.
            - Inconclusive: too close, blurry or cropped to judge extent or condition.
            severity_reason: one sentence citing the visible evidence.""";

    static final Set<String> DEFECT_TYPES = Set.of("water_stain_or_leak", "mold", "both_leak_and_mold", "crack", "none", "other_defect");

    private static final String SCOPE = "https://www.googleapis.com/auth/cloud-platform";
    private static final int MAX_IMAGE_SIDE = 1024;
    private static final long MAX_INLINE_BYTES = 15L * 1024 * 1024; // inline 요청 한도(20MB)보다 여유 있게

    private final JsonMapper jsonMapper;
    private final HttpClient httpClient;
    private final String endpointUrl;
    private final Duration timeout;
    private GoogleCredentials credentials;

    public GeminiInspectorClient(JsonMapper jsonMapper,
                                 @Value("${gemini.endpoint-url:}") String endpointUrl,
                                 @Value("${gemini.timeout-seconds:60}") long timeoutSeconds) {
        this.jsonMapper = jsonMapper;
        this.endpointUrl = endpointUrl == null ? "" : endpointUrl.strip();
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    // 엔드포인트가 설정됐는지 (없으면 사진 판정 없이 Inconclusive)
    public boolean isConfigured() {
        return !endpointUrl.isEmpty();
    }

    // 사진·영상 1개 판정 (실패해도 예외 대신 Inconclusive)
    public VisionObservation inspect(byte[] content, String mimeType, boolean video) {
        if (!isConfigured()) {
            return VisionObservation.inconclusive(null, null, "Vision model is not configured.");
        }
        if (content.length > MAX_INLINE_BYTES) {
            log.warn("[GeminiInspectorClient] 파일이 너무 커서 판정 생략: bytes={}", content.length);
            return VisionObservation.inconclusive(null, null, "The file is too large to analyze.");
        }

        Media media = video ? new Media(content, mimeType) : shrinkImage(content, mimeType);
        String body = toRequestBody(media, video);
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                VisionObservation observation = parse(call(body));
                if (observation != null) {
                    return observation;
                }
                log.warn("[GeminiInspectorClient] 응답 형식 오류: attempt={}", attempt);
            } catch (LlmException e) {
                log.warn("[GeminiInspectorClient] 호출 실패: attempt={}, message={}", attempt, e.getMessage());
            }
        }
        return VisionObservation.inconclusive(null, null, "The photo could not be analyzed.");
    }

    // 모델 출력 텍스트 -> VisionObservation (형식·값이 틀리면 null)
    VisionObservation parse(String text) {
        if (text == null) {
            return null;
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        JsonNode node;
        try {
            node = jsonMapper.readTree(text.substring(start, end + 1));
        } catch (Exception e) {
            return null;
        }
        String defectType = node.path("defect_type").asString(null);
        AssessedSeverity severity = AssessedSeverity.parse(node.path("severity").asString(null));
        String reason = node.path("severity_reason").asString(null);
        if (!DEFECT_TYPES.contains(defectType) || severity == null || reason == null || reason.isBlank()) {
            return null;
        }
        List<String> signs = new ArrayList<>();
        node.path("visible_signs").forEach(sign -> {
            if (sign.isString()) {
                signs.add(sign.asString());
            }
        });
        return new VisionObservation(null, null, defectType, severity, reason.strip(),
                node.path("location").asString(null), signs,
                node.path("active_water_or_dampness").asString(null), node.path("extent").asString(null));
    }

    // ========== 헬퍼 함수 ==========

    private String call(String body) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpointUrl))
                .timeout(timeout)
                .header("Authorization", "Bearer " + accessToken())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.error("[GeminiInspectorClient] 호출 실패: status={}, body={}", response.statusCode(), response.body());
                throw new LlmException("Gemini request failed with status " + response.statusCode());
            }
            // 튜닝 모델은 thinking 파트 없이 답변 파트 1개, 혹시 여러 개면 이어 붙임
            StringBuilder text = new StringBuilder();
            jsonMapper.readTree(response.body()).path("candidates").path(0).path("content").path("parts")
                    .forEach(part -> text.append(part.path("text").asString("")));
            return text.toString();
        } catch (IOException e) {
            throw new LlmException("Gemini request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("Gemini request was interrupted.", e);
        }
    }

    private synchronized String accessToken() {
        try {
            if (credentials == null) {
                credentials = GoogleCredentials.getApplicationDefault().createScoped(List.of(SCOPE));
            }
            credentials.refreshIfExpired();
            return credentials.getAccessToken().getTokenValue();
        } catch (IOException e) {
            throw new LlmException("Google credentials are not available: " + e.getMessage(), e);
        }
    }

    private String toRequestBody(Media media, boolean video) {
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", 0);
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("thinkingConfig", Map.of("thinkingLevel", "MINIMAL"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", SYSTEM))));
        body.put("contents", List.of(Map.of("role", "user", "parts", List.of(
                Map.of("inlineData", Map.of("mimeType", media.mimeType(), "data", Base64.getEncoder().encodeToString(media.bytes()))),
                Map.of("text", video ? "Analyze this video." : "Analyze this photo.")))));
        body.put("generationConfig", generationConfig);
        return jsonMapper.writeValueAsString(body);
    }

    // 긴 변이 1024px보다 크면 줄여서 JPEG로 (읽을 수 없는 형식(HEIC 등)이면 원본 그대로)
    private Media shrinkImage(byte[] content, String mimeType) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
            if (image == null || Math.max(image.getWidth(), image.getHeight()) <= MAX_IMAGE_SIDE) {
                return new Media(content, mimeType);
            }
            double scale = (double) MAX_IMAGE_SIDE / Math.max(image.getWidth(), image.getHeight());
            int width = (int) Math.round(image.getWidth() * scale);
            int height = (int) Math.round(image.getHeight() * scale);
            BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = resized.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(image, 0, 0, width, height, null);
            graphics.dispose();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(resized, "jpg", out);
            return new Media(out.toByteArray(), "image/jpeg");
        } catch (IOException e) {
            return new Media(content, mimeType);
        }
    }

    private record Media(byte[] bytes, String mimeType) {
    }
}
