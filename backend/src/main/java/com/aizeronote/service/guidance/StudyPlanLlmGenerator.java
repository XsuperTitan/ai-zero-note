package com.aizeronote.service.guidance;

import com.aizeronote.config.SummaryProperties;
import com.aizeronote.model.dto.guidance.LearningQuestionnaireSubmitRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class StudyPlanLlmGenerator {

    private static final String SYSTEM = """
            You output JSON only with keys:
            outlineMarkdown, suggestions, priorities, videos, currentVideoId.

            suggestions: array of concise strings (Chinese) for study habits / pacing.
            priorities: ordered array (Chinese), 3-5 items, most important first.
            videos: 2-4 objects with keys:
              id (short stable id like "v-1"),
              title (Chinese),
              platform (BILIBILI, YOUTUBE, or OTHER),
              url (string; empty if unsure),
              rationale (Chinese),
              sortOrder (integer, 1-based),
              linkKind (SEARCH_PLACEHOLDER | EXTERNAL | UNSPECIFIED).

            Rules:
            - If you cannot verify a real watch URL, set url to "" and linkKind to SEARCH_PLACEHOLDER.
            - Never invent BV IDs, YouTube watch?v= codes, or channel-specific URLs without evidence from input.
            - outlineMarkdown: Chinese Markdown with headings (#/##), include a short roadmap tied to the learner topic.

            currentVideoId must match one of videos[].id and point to the best first watch.
            """;

    private final RestClient restClient;
    private final SummaryProperties summaryProperties;
    private final ObjectMapper objectMapper;

    public StudyPlanLlmGenerator(
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

    public StudyPlanPayload generate(
            LearningQuestionnaireSubmitRequest q,
            String reportSummary,
            String llmConstraints
    ) throws Exception {
        String userBlock = buildUserBlock(q, reportSummary, llmConstraints);
        Map<String, Object> requestBody = Map.of(
                "model", Objects.requireNonNull(summaryProperties.model(), "summary.model is required"),
                "temperature", 0.35,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM),
                        Map.of("role", "user", "content", userBlock)
                )
        );
        String response = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);
        JsonNode root = objectMapper.readTree(Objects.requireNonNull(response));
        String content = root.path("choices").path(0).path("message").path("content").asText("");
        return parsePayload(content);
    }

    private String buildUserBlock(
            LearningQuestionnaireSubmitRequest q,
            String reportSummary,
            String llmConstraints
    ) {
        try {
            String qJson = objectMapper.writeValueAsString(q);
            return """
                    根据以下问卷 JSON、画像报告与 LLM 约束，生成学习方案 JSON：

                    【问卷】
                    %s

                    【画像报告（Markdown）】
                    %s

                    【LLM 约束】
                    %s
                    """.formatted(
                    qJson,
                    StringUtils.hasText(reportSummary) ? reportSummary : "(empty)",
                    StringUtils.hasText(llmConstraints) ? llmConstraints : "(empty)"
            );
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private StudyPlanPayload parsePayload(String content) throws Exception {
        JsonNode node = objectMapper.readTree(content);
        String outline = node.path("outlineMarkdown").asText("");
        List<String> suggestions = readStringArray(node.path("suggestions"));
        List<String> priorities = readStringArray(node.path("priorities"));
        List<StudyPlanVideoPayload> videos = new ArrayList<>();
        if (node.path("videos").isArray()) {
            for (JsonNode v : node.path("videos")) {
                String id = v.path("id").asText("").trim();
                if (id.isEmpty()) {
                    continue;
                }
                int sort = v.path("sortOrder").asInt(0);
                videos.add(new StudyPlanVideoPayload(
                        id,
                        v.path("title").asText(""),
                        v.path("platform").asText("OTHER"),
                        v.path("url").asText(""),
                        v.path("rationale").asText(""),
                        sort > 0 ? sort : videos.size() + 1,
                        v.path("linkKind").asText("UNSPECIFIED")
                ));
            }
        }
        if (videos.isEmpty()) {
            throw new IllegalStateException("LLM returned no videos");
        }
        String current = node.path("currentVideoId").asText("").trim();
        if (current.isEmpty()) {
            current = videos.get(0).id();
        }
        return new StudyPlanPayload(outline, suggestions, priorities, videos, current);
    }

    private static List<String> readStringArray(JsonNode arr) {
        List<String> out = new ArrayList<>();
        if (!arr.isArray()) {
            return out;
        }
        for (JsonNode n : arr) {
            String s = n.asText("").trim();
            if (StringUtils.hasText(s)) {
                out.add(s);
            }
        }
        return out;
    }
}
