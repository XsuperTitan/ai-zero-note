package com.aizeronote.model;

public record AsrStatus(
        String defaultProvider,
        boolean whisperConfigured,
        boolean challengerConfigured
) {
}
