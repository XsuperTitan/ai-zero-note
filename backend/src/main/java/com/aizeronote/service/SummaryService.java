package com.aizeronote.service;

import com.aizeronote.config.SummaryProperties;
import com.aizeronote.model.NoteSummary;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class SummaryService {

    private static final String SYSTEM_PROMPT = """
            You are an English learning note assistant.
            Return valid JSON only with keys:
            title, abstractText, keyPoints, codeSnippets, todos, markdownContent.
            markdownContent must be a complete Markdown note and should be the final user-facing output.
            Keep keyPoints and todos concise; if no code snippets exist, return an empty array.
            """;
    private static final String FALLBACK_USER_PROMPT = """
            以上是某视频的学习录音，要求根据上述转写内容生成详细的英文学习笔记。
            目标是让我用更通俗的方式理解视频内容。
            请覆盖：
            1) 核心概念解释（简单易懂）
            2) 关键术语中英对照与释义
            3) 重要知识点分层总结
            4) 示例句或可复用表达
            5) 容易混淆点与纠错建议
            6) 最后给出复习清单和可执行练习建议
            """;

    private final RestClient restClient;
    private final SummaryProperties summaryProperties;
    private final ObjectMapper objectMapper;

    public SummaryService(
            SummaryProperties summaryProperties,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder
    ) {
        this.summaryProperties = summaryProperties;
        this.objectMapper = objectMapper;
        String baseUrl = Objects.requireNonNull(summaryProperties.baseUrl(), "summary.base-url is required");
        String apiKey = Objects.requireNonNull(summaryProperties.apiKey(), "summary.api-key is required");
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    public NoteSummary summarize(String transcription) {
        return summarizeWithSupplemental(transcription, "");
    }

    public NoteSummary summarizeWithSupplemental(String transcription, String supplementalText) {
        try {
            String userPrompt = StringUtils.hasText(summaryProperties.userPrompt())
                    ? summaryProperties.userPrompt()
                    : FALLBACK_USER_PROMPT;
            String mergedInput = buildMergedInput(transcription, supplementalText, userPrompt);

            Map<String, Object> requestBody = Map.of(
                    "model", Objects.requireNonNull(summaryProperties.model(), "summary.model is required"),
                    "temperature", 0.2,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", mergedInput)
                    )
            );

            String response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                    .body(Objects.requireNonNull(requestBody))
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            return parseSummary(content);
        } catch (Exception ex) {
            throw new IllegalStateException("Summary generation failed: " + ex.getMessage(), ex);
        }
    }

    private String buildMergedInput(String transcription, String supplementalText, String userPrompt) {
        String transcriptionText = StringUtils.hasText(transcription) ? transcription : "(Empty transcription)";
        String textInput = StringUtils.hasText(supplementalText) ? supplementalText : "(Empty text input)";
        return """
                以下是提取的mp3音频转写内容：
                %s

                以下是用户提供的补充文字内容：
                %s

                %s
                """.formatted(transcriptionText, textInput, userPrompt);
    }

    private NoteSummary parseSummary(String content) {
        try {
            JsonNode summaryJson = objectMapper.readTree(content);
            String markdownContent = summaryJson.path("markdownContent").asText("");
            String title = summaryJson.path("title").asText(extractTitleFromMarkdown(markdownContent));
            String abstractText = summaryJson.path("abstractText").asText("");

            return new NoteSummary(
                    title,
                    abstractText,
                    toStringList(summaryJson.path("keyPoints")),
                    toStringList(summaryJson.path("codeSnippets")),
                    toStringList(summaryJson.path("todos")),
                    markdownContent
            );
        } catch (Exception jsonParseError) {
            String markdown = StringUtils.hasText(content) ? content : "# Notes\n\nNo content.";
            return new NoteSummary(
                    extractTitleFromMarkdown(markdown),
                    "",
                    List.of(),
                    List.of(),
                    List.of(),
                    markdown
            );
        }
    }

    private String extractTitleFromMarkdown(String markdown) {
        if (!StringUtils.hasText(markdown)) {
            return "Untitled Notes";
        }
        String[] lines = markdown.split("\\R");
        for (String line : lines) {
            if (line.startsWith("# ")) {
                return line.substring(2).trim();
            }
        }
        return "Untitled Notes";
    }

    private List<String> toStringList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return values;
        }
        node.forEach(item -> values.add(item.asText("")));
        return values;
    }
}
