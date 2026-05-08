package com.aizeronote.service;

import com.aizeronote.config.SummaryProperties;
import com.aizeronote.model.NoteSummary;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SummaryService {

    private static final String SYSTEM_PROMPT = """
            You are a technical note assistant.
            Return valid JSON only with keys:
            title, abstractText, keyPoints, codeSnippets, todos.
            Keep keyPoints and todos concise.
            If no code snippets exist, return an empty array.
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
        this.restClient = restClientBuilder
                .baseUrl(summaryProperties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + summaryProperties.apiKey())
                .build();
    }

    public NoteSummary summarize(String transcription) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", summaryProperties.model(),
                    "temperature", 0.2,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", SYSTEM_PROMPT),
                            Map.of("role", "user", "content", "Transcription:\n" + transcription)
                    )
            );

            String response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").path(0).path("message").path("content").asText("{}");
            JsonNode summaryJson = objectMapper.readTree(content);

            return new NoteSummary(
                    summaryJson.path("title").asText("Untitled Notes"),
                    summaryJson.path("abstractText").asText(""),
                    toStringList(summaryJson.path("keyPoints")),
                    toStringList(summaryJson.path("codeSnippets")),
                    toStringList(summaryJson.path("todos"))
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Summary generation failed: " + ex.getMessage(), ex);
        }
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
