package com.aizeronote.service;

import com.aizeronote.config.ImageNotesEnvSupport;
import com.aizeronote.config.ImageNotesProperties;
import com.aizeronote.model.ImageNoteJobStatus;
import com.aizeronote.model.entity.ImageNoteJob;
import com.aizeronote.repository.ImageNoteJobRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ImageNoteGenerationProcessor {

    static final String PROVIDER_OPENAI = "openai";
    static final String PROVIDER_WANX = "wanx";

    private static final String WAN_SYNC_PATH =
            "/services/aigc/multimodal-generation/generation";
    private static final Logger log = LoggerFactory.getLogger(ImageNoteGenerationProcessor.class);
    private static final String STYLE_PREFIX =
            "Single wide educational infographic or playful comic-style sketch note. "
                    + "Vivid colors, legible captions only when essential, symbolic icons instead of recognizable real people. "
                    + "No tiny unreadable lettering. Illustrate learning content visually with metaphors. Content:\n";

    private final ImageNoteJobRepository repository;
    private final ImageNoteStorageService imageNoteStorageService;
    private final ImageNotesProperties props;
    private final Environment environment;
    private final ObjectMapper objectMapper;
    private volatile RestClient lazilyConfiguredOpenAiClient;
    private volatile RestClient lazilyConfiguredWanClient;

    public ImageNoteGenerationProcessor(
            ImageNoteJobRepository repository,
            ImageNoteStorageService imageNoteStorageService,
            ImageNotesProperties props,
            Environment environment,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.imageNoteStorageService = imageNoteStorageService;
        this.props = props;
        this.environment = environment;
        this.objectMapper = objectMapper;
    }

    public void process(String jobId) {
        ImageNoteJob job = repository.findByJobId(jobId).orElse(null);
        if (job == null) {
            log.warn("image_note_job not found {}", jobId);
            return;
        }
        try {
            job.setStatus(ImageNoteJobStatus.PROCESSING.name());
            repository.save(job);
            String excerpt = job.getSourceExcerpt() != null ? job.getSourceExcerpt() : "";
            String prompt = buildPrompt(excerpt);
            byte[] png = callImageApi(prompt);
            String fileName = jobId + ".png";
            imageNoteStorageService.writeBytes(fileName, png);
            job.setStatus(ImageNoteJobStatus.SUCCEEDED.name());
            job.setImageFileName(fileName);
            job.setErrorMessage(null);
            repository.save(job);
        } catch (Exception ex) {
            log.warn("Image note generation failed jobId={}", jobId, ex);
            job.setStatus(ImageNoteJobStatus.FAILED.name());
            job.setErrorMessage(truncateMessage(ex.getMessage()));
            repository.save(job);
        }
    }

    private RestClient openAiClient() {
        RestClient existing = lazilyConfiguredOpenAiClient;
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            if (lazilyConfiguredOpenAiClient == null) {
                String baseUrl = Objects.requireNonNull(
                        trimToPresent(ImageNotesEnvSupport.openAiBaseUrl(environment, props)),
                        "image-notes.base-url is required when provider=" + PROVIDER_OPENAI);
                String apiKey = Objects.requireNonNull(
                        trimToPresent(ImageNotesEnvSupport.apiKey(environment, props)),
                        "image-notes.api-key is required when enabled");
                String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
                lazilyConfiguredOpenAiClient = RestClient.builder()
                        .baseUrl(normalized)
                        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .build();
            }
            return lazilyConfiguredOpenAiClient;
        }
    }

    /** DashScope 「万相同步 HTTP」——仅用于 path，不含尾随斜杠。 */
    private RestClient wanApiClient() {
        RestClient existing = lazilyConfiguredWanClient;
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            if (lazilyConfiguredWanClient == null) {
                String wanRoot = Objects.requireNonNull(
                        trimToPresent(ImageNotesEnvSupport.effectiveWanBaseUrl(environment, props)),
                        "image-notes.wan-base-url is required when provider=" + PROVIDER_WANX);
                String apiKey = Objects.requireNonNull(
                        trimToPresent(ImageNotesEnvSupport.apiKey(environment, props)),
                        "image-notes.api-key is required when enabled");
                String normalized = wanRoot.endsWith("/") ? wanRoot.substring(0, wanRoot.length() - 1) : wanRoot;
                lazilyConfiguredWanClient = RestClient.builder()
                        .baseUrl(normalized)
                        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .build();
            }
            return lazilyConfiguredWanClient;
        }
    }

    private RestClient wanDownloadClient() {
        return RestClient.builder().build();
    }

    private String buildPrompt(String excerpt) {
        String body = excerpt;
        int maxChars = props.maxSourceChars() > 0 ? props.maxSourceChars() : 12000;
        if (body.length() > maxChars) {
            body = body.substring(0, maxChars);
        }
        String full = STYLE_PREFIX + body;
        int cap = wanProvider() ? 5000 : 3800;
        if (full.length() > cap) {
            full = full.substring(0, cap);
        }
        return full;
    }

    private byte[] callImageApi(String prompt) throws Exception {
        if (wanProvider()) {
            return callDashScopeWanSync(prompt);
        }
        return callOpenAiImages(prompt);
    }

    private boolean wanProvider() {
        return ImageNotesEnvSupport.isWan(environment, props);
    }

    private static String trimToPresent(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String firstNonBlank(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        String t = raw.trim();
        return StringUtils.hasText(t) ? t : "";
    }

    private byte[] callOpenAiImages(String prompt) throws Exception {
        String modelStr = ImageNotesEnvSupport.model(environment, props);
        String model = StringUtils.hasText(modelStr) ? modelStr : "dall-e-3";
        String sizeStr = ImageNotesEnvSupport.size(environment, props);
        String size = StringUtils.hasText(sizeStr) ? sizeStr : "1792x1024";
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "prompt", prompt,
                "n", 1,
                "size", size,
                "response_format", "b64_json"
        );

        String responseBody = Objects.requireNonNull(openAiClient().post()
                .uri("/images/generations")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class));

        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode data = root.path("data");
        String b64 = "";
        if (data.isArray() && !data.isEmpty()) {
            b64 = data.get(0).path("b64_json").asText("");
        }
        if (!StringUtils.hasText(b64)) {
            String errDetail = extractDashScopeRootError(root);
            throw new IllegalStateException(
                    StringUtils.hasText(errDetail)
                            ? "Image API returned no b64_json. " + errDetail
                            : "Image API returned no b64_json."
            );
        }
        return Base64.getDecoder().decode(b64);
    }

    /**
     * <a href="https://help.aliyun.com/zh/model-studio/wan-image-generation-and-editing-api-reference">
     * 万相 2.x 同步文生图</a>：<code>/services/aigc/multimodal-generation/generation</code>，
     * 响应体 <code>output.choices[].message.content[].image</code> 为临时 PNG URL（需限时下载）。
     */
    private byte[] callDashScopeWanSync(String prompt) throws Exception {
        String modelStr = ImageNotesEnvSupport.model(environment, props);
        String model = StringUtils.hasText(modelStr) ? modelStr : "wan2.7-image-pro";
        String wanSize = resolveWanOutputSize(firstNonBlank(ImageNotesEnvSupport.size(environment, props)));

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "input", Map.of(
                        "messages", List.of(
                                Map.of(
                                        "role", "user",
                                        "content", List.of(Map.of("text", prompt))
                                )
                        )
                ),
                "parameters", Map.of(
                        "size", wanSize,
                        "n", 1,
                        "watermark", false,
                        "thinking_mode", false
                )
        );

        String responseBody = Objects.requireNonNull(wanApiClient().post()
                .uri(WAN_SYNC_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class));

        JsonNode root = objectMapper.readTree(responseBody);

        JsonNode topCode = root.path("code");
        if (!topCode.isMissingNode() && StringUtils.hasText(topCode.asText())) {
            String msg = root.path("message").asText(topCode.asText());
            throw new IllegalStateException("DashScope error: " + msg);
        }

        JsonNode imageUrlNode = root
                .path("output")
                .path("choices")
                .path(0)
                .path("message")
                .path("content")
                .path(0)
                .path("image");

        String imageUrl = imageUrlNode.asText("");
        if (!StringUtils.hasText(imageUrl)) {
            throw new IllegalStateException(
                    "DashScope WAN returned no image URL. " + extractDashScopeRootError(root));
        }

        byte[] png = wanDownloadClient().get()
                .uri(URI.create(imageUrl))
                .retrieve()
                .body(byte[].class);
        if (png == null || png.length == 0) {
            throw new IllegalStateException("Downloaded WAN image bytes are empty.");
        }
        return png;
    }

    /**
     * 万相 size：方式一为 1K / 2K / 4K；方式二为 "宽*高" 像素写法。若为 OpenAI 风格（1792x1024）则降级为 2K。
     */
    static String resolveWanOutputSize(String configured) {
        String s = firstNonBlank(configured);
        if (!StringUtils.hasText(s)) {
            return "2K";
        }
        String trimmed = s.trim();
        String upper = trimmed.toUpperCase();
        if (upper.equals("1K") || upper.equals("2K") || upper.equals("4K")) {
            return upper;
        }
        if (trimmed.contains("*")) {
            return trimmed;
        }
        if (trimmed.matches("(?i)\\d+\\s*[x×]\\s*\\d+")) {
            return "2K";
        }
        return "2K";
    }

    private static String extractDashScopeRootError(JsonNode root) {
        if (root.hasNonNull("code")) {
            return root.path("code").asText("") + ": " + root.path("message").asText("");
        }
        JsonNode err = root.path("error");
        if (!err.isMissingNode()) {
            return err.path("message").asText(err.toString());
        }
        return "";
    }

    private static String truncateMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "Unknown error.";
        }
        return message.length() <= 2048 ? message : message.substring(0, 2048);
    }
}
