package com.aizeronote.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vision")
public record VisionProperties(
        Boolean enabled,
        String baseUrl,
        String apiKey,
        String model,
        String prompt,
        Integer maxFrames
) {
}
