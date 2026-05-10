package com.aizeronote.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "summary")
public record SummaryProperties(
        String baseUrl,
        String apiKey,
        String model,
        String userPrompt
) {
}
