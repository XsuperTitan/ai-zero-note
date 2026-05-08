package com.aizeronote.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "asr")
public record AsrProperties(
        String defaultProvider,
        ProviderConfig whisper,
        ProviderConfig challenger
) {
    public record ProviderConfig(
            String baseUrl,
            String apiKey,
            String model,
            String transcriptionPath
    ) {
    }
}
