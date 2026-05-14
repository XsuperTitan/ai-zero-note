package com.aizeronote.service;

import com.aizeronote.config.SummaryProperties;
import com.aizeronote.model.NoteStyle;
import com.aizeronote.model.NoteSummary;
import com.aizeronote.model.OutputLanguage;
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

    private static final String SYSTEM_PROMPT_LEARNING = """
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

    private static final String SYSTEM_PROMPT_DETAILED = """
            You produce structured study notes as JSON only with keys:
            title, abstractText, keyPoints, codeSnippets, todos, markdownContent.
            markdownContent must be the full user-facing Markdown (headings, lists, tables OK).
            keyPoints summarize high-signal bullets; todos are actionable checklist items.
            codeSnippets: empty array unless real code appeared in source.
            Respect the OUTPUT_LANGUAGE_DIRECTIVE appended in the user message.
            """;
    private static final String FALLBACK_DETAILED_USER = """
            根据材料写「更详细的学习笔记」（比纲要更充实，仍可分层阅读）。
            要求：
            1) 尽量覆盖材料中的论点、定义、前提与结论；
            2) 可适当补充背景，但不要编造材料未出现的具体数据、引用或细节；
            3) keyPoints 与 markdownContent 要一致且不空洞；
            4) markdownContent 中保留清晰的小节结构与必要列表、对照表；
            """;

    private static final String SYSTEM_PROMPT_MIND_MAP = """
            You output JSON only with keys:
            title, abstractText, keyPoints, codeSnippets, todos, markdownContent, mindMap.
            mindMap MUST be nested objects: {\"topic\":\"...\",\"keywords\":[short strings],\"children\":[ same shape recursively ]}.
            Use short keyword phrases in topic/keywords—no paragraphs. Mirror material coverage without inventing unseen facts.
            markdownContent MUST be Markdown that reflects the tree (nested bullet list) for download/fallback.
            codeSnippets: usually empty arrays.
            Abstract should be one or two terse sentences summarizing themes.
            Follow OUTPUT_LANGUAGE_DIRECTIVE below in mindMap.topic/keywords/text fields and markdown.
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
        return summarizeWithSupplemental(transcription, "", NoteStyle.LEARNING, OutputLanguage.AUTO);
    }

    public NoteSummary summarizeWithSupplemental(
            String transcription,
            String supplementalText,
            NoteStyle noteStyle,
            OutputLanguage outputLanguage
    ) {
        Objects.requireNonNull(noteStyle);
        Objects.requireNonNull(outputLanguage);
        try {
            String systemPrompt = resolveSystemPrompt(noteStyle);
            String userBlock = resolveUserInstructions(transcription, supplementalText, noteStyle, outputLanguage);

            Map<String, Object> requestBody = Map.of(
                    "model", Objects.requireNonNull(summaryProperties.model(), "summary.model is required"),
                    "temperature", 0.2,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userBlock)
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

    private String resolveSystemPrompt(NoteStyle noteStyle) {
        return switch (noteStyle) {
            case LEARNING -> SYSTEM_PROMPT_LEARNING;
            case DETAILED -> SYSTEM_PROMPT_DETAILED;
            case MIND_MAP -> SYSTEM_PROMPT_MIND_MAP;
        };
    }

    private String resolveUserInstructions(
            String transcription,
            String supplementalText,
            NoteStyle noteStyle,
            OutputLanguage outputLanguage
    ) {
        return switch (noteStyle) {
            case LEARNING -> {
                String userPrompt = StringUtils.hasText(summaryProperties.userPrompt())
                        ? summaryProperties.userPrompt()
                        : FALLBACK_USER_PROMPT;
                yield buildMergedInput(transcription, supplementalText, userPrompt);
            }
            case DETAILED ->
                    attachLanguageDirective(buildMergedDetailedInput(transcription, supplementalText), outputLanguage);
            case MIND_MAP ->
                    attachLanguageDirective(buildMergedMindMapInput(transcription, supplementalText), outputLanguage);
        };
    }

    private static String attachLanguageDirective(String mergedInput, OutputLanguage outputLanguage) {
        OutputLanguage effective = outputLanguage == OutputLanguage.AUTO ? OutputLanguage.BILINGUAL : outputLanguage;
        String directive = switch (effective) {
            case ZH ->
                    "OUTPUT_LANGUAGE_DIRECTIVE：正文（含 mindMap 层级文案若以中文更合适则）以简体中文为主；需要时可保留少量英文专有名词。\n";
            case EN ->
                    "OUTPUT_LANGUAGE_DIRECTIVE: Write Markdown and mind-map label text primarily in fluent English.\n";
            case BILINGUAL, AUTO ->
                    "OUTPUT_LANGUAGE_DIRECTIVE：关键小节采用中英双语对照（例如中文段落后附英文段落，或使用对照表）；保持可读性。\n";
        };
        return directive + mergedInput;
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

    private String buildMergedDetailedInput(String transcription, String supplementalText) {
        String transcriptionText = StringUtils.hasText(transcription) ? transcription : "(Empty transcription)";
        String textInput = StringUtils.hasText(supplementalText) ? supplementalText : "(Empty text input)";
        return """
                以下是音频转写或视频相关的文字材料（可能为空）：
                %s

                以下是用户粘贴的补充文本（讲义、图生文摘要等，可为空）：
                %s

                %s
                """.formatted(transcriptionText, textInput, FALLBACK_DETAILED_USER);
    }

    private String buildMergedMindMapInput(String transcription, String supplementalText) {
        String transcriptionText = StringUtils.hasText(transcription) ? transcription : "(Empty transcription)";
        String textInput = StringUtils.hasText(supplementalText) ? supplementalText : "(Empty text input)";
        return """
                材料（音频转写，可为空）：
                %s

                补充文本（讲义、摘录、图生文等，可为空）：
                %s

                仅输出简短关键词层级：思维导图不要超过 8 个一级分支；每层用短语概括，不写长段落。
                若材料稀疏，可减少分支并保持诚实；不要编造具体时间、姓名、数额或未见细节。
                """.formatted(transcriptionText, textInput);
    }

    private NoteSummary parseSummary(String content) {
        try {
            JsonNode summaryJson = objectMapper.readTree(content);
            String markdownContent = summaryJson.path("markdownContent").asText("");
            String title = summaryJson.path("title").asText(extractTitleFromMarkdown(markdownContent));
            String abstractText = summaryJson.path("abstractText").asText("");
            JsonNode mm = summaryJson.path("mindMap");
            String mindMapJson = "";
            if (!mm.isMissingNode() && !mm.isNull()) {
                mindMapJson = objectMapper.writeValueAsString(mm);
            }

            return new NoteSummary(
                    title,
                    abstractText,
                    toStringList(summaryJson.path("keyPoints")),
                    toStringList(summaryJson.path("codeSnippets")),
                    toStringList(summaryJson.path("todos")),
                    markdownContent,
                    mindMapJson
            );
        } catch (Exception jsonParseError) {
            String markdown = StringUtils.hasText(content) ? content : "# Notes\n\nNo content.";
            return new NoteSummary(
                    extractTitleFromMarkdown(markdown),
                    "",
                    List.of(),
                    List.of(),
                    List.of(),
                    markdown,
                    ""
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
