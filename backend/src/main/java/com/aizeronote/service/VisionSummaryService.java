package com.aizeronote.service;

import com.aizeronote.config.VisionProperties;
import com.aizeronote.model.VideoMetaResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class VisionSummaryService {

    private static final String DEFAULT_PROMPT = """
            你是视频截图学习内容分析助手。请在“信息完整性”和“证据约束”之间保持平衡：
            可以总结、归纳和适度抽象，但不得编造截图中未出现的具体人名、编号、金额、链接或步骤细节。
            输出语言优先遵循“目标输出语言”参数；若参数为空或为auto，再根据截图主导语言输出（中文主导输出中文，英文主导输出英文）。
            请按以下结构输出：
            1) 主题（1-2句）
            2) 关键要点（4-8条）
            3) 可确认示例（仅列截图可见证据，必要时引用可见关键词）
            4) 合理推断（可选，需明确标注“推断”）
            5) 不确定信息（可选：仅当存在关键缺失时输出；若无则写“无”）
            每条要点末尾标注证据等级：高/中/低。
            """;
    private static final int DEFAULT_MAX_FRAMES = 30;

    private final VisionProperties visionProperties;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    public VisionSummaryService(
            VisionProperties visionProperties,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder
    ) {
        this.visionProperties = visionProperties;
        this.objectMapper = objectMapper;
        this.restClientBuilder = restClientBuilder;
    }

    public String summarizeFrames(VideoMetaResult meta, List<Path> framePaths, String targetLanguage) {
        if (!Boolean.TRUE.equals(visionProperties.enabled())) {
            throw new IllegalStateException("VISION_ENABLED=false，图生文功能未启用。");
        }
        if (!StringUtils.hasText(visionProperties.baseUrl())
                || !StringUtils.hasText(visionProperties.apiKey())
                || !StringUtils.hasText(visionProperties.model())) {
            throw new IllegalStateException("VISION_BASE_URL / VISION_API_KEY / VISION_MODEL 配置不完整。");
        }
        if (framePaths == null || framePaths.isEmpty()) {
            throw new IllegalArgumentException("未提供可用于图生文的截图。");
        }

        List<Path> selectedFrames = selectRepresentativeFrames(
                framePaths.stream().filter(Objects::nonNull).toList(),
                resolveMaxFrames()
        );
        if (selectedFrames.isEmpty()) {
            throw new IllegalArgumentException("可用截图为空，请重新生成关键截图后重试。");
        }

        try {
            String model = Objects.requireNonNull(visionProperties.model());
            String baseUrl = Objects.requireNonNull(visionProperties.baseUrl());
            String apiKey = Objects.requireNonNull(visionProperties.apiKey());
            RestClient client = restClientBuilder
                    .baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .build();

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "temperature", 0.2,
                    "messages", List.of(
                            Map.of("role", "user", "content", buildUserContent(meta, selectedFrames, targetLanguage))
                    )
            );

            String response = client.post()
                    .uri("/chat/completions")
                    .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                    .body(Objects.requireNonNull(requestBody))
                    .retrieve()
                    .body(String.class);
            String content = extractContent(response);
            if (!StringUtils.hasText(content)) {
                throw new IllegalStateException("图生文接口返回为空，请检查模型是否支持图片输入。");
            }
            return content;
        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            String briefBody = StringUtils.hasText(body) ? body : "(empty body)";
            throw new IllegalStateException(
                    "图生文请求失败，HTTP " + ex.getStatusCode().value() + "，响应: " + briefBody,
                    ex
            );
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            String message = StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : ex.getClass().getSimpleName();
            throw new IllegalStateException("图生文请求异常: " + message, ex);
        }
    }

    private int resolveMaxFrames() {
        Integer configured = visionProperties.maxFrames();
        if (configured == null || configured <= 0) {
            return DEFAULT_MAX_FRAMES;
        }
        return Math.min(configured, 30);
    }

    private List<Path> selectRepresentativeFrames(List<Path> source, int maxFrames) {
        if (source.isEmpty() || source.size() <= maxFrames) {
            return source;
        }
        if (maxFrames <= 1) {
            return List.of(source.get(source.size() / 2));
        }
        List<Path> selected = new ArrayList<>();
        double step = (double) (source.size() - 1) / (maxFrames - 1);
        for (int i = 0; i < maxFrames; i++) {
            int index = (int) Math.round(i * step);
            selected.add(source.get(index));
        }
        return selected;
    }

    private List<Map<String, Object>> buildUserContent(
            VideoMetaResult meta,
            List<Path> framePaths,
            String targetLanguage
    ) throws Exception {
        List<Map<String, Object>> content = new ArrayList<>();
        String prompt = StringUtils.hasText(visionProperties.prompt()) ? visionProperties.prompt() : DEFAULT_PROMPT;
        String normalizedTargetLanguage = normalizeTargetLanguage(targetLanguage);
        content.add(Map.of(
                "type", "text",
                "text", """
                        视频标题：%s
                        视频链接：%s
                        视频时长：%s
                        目标输出语言：%s

                        %s
                        """.formatted(meta.title(), meta.sourceUrl(), meta.durationText(), normalizedTargetLanguage, prompt)
        ));
        for (Path path : framePaths) {
            byte[] bytes = Files.readAllBytes(path);
            if (bytes.length == 0) {
                continue;
            }
            String base64 = Base64.getEncoder().encodeToString(bytes);
            Map<String, Object> imageUrl = new LinkedHashMap<>();
            imageUrl.put("url", "data:image/jpeg;base64," + base64);
            content.add(Map.of(
                    "type", "image_url",
                    "image_url", imageUrl
            ));
        }
        return content;
    }

    private String normalizeTargetLanguage(String targetLanguage) {
        if (!StringUtils.hasText(targetLanguage)) {
            return "auto";
        }
        String normalized = targetLanguage.trim().toLowerCase();
        if ("zh".equals(normalized) || "zh-cn".equals(normalized) || "chinese".equals(normalized)) {
            return "zh";
        }
        if ("en".equals(normalized) || "english".equals(normalized)) {
            return "en";
        }
        return "auto";
    }

    private String extractContent(String rawResponse) throws Exception {
        JsonNode root = objectMapper.readTree(rawResponse);
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (contentNode.isTextual()) {
            return contentNode.asText("");
        }
        if (!contentNode.isArray()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (JsonNode part : contentNode) {
            String text = part.path("text").asText("");
            if (!StringUtils.hasText(text)) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append("\n");
            }
            builder.append(text.trim());
        }
        return builder.toString().trim();
    }
}
