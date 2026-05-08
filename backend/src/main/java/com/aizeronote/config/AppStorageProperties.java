package com.aizeronote.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record AppStorageProperties(
        @NotBlank String outputDir,
        @Positive long maxUploadSizeBytes
) {
}
