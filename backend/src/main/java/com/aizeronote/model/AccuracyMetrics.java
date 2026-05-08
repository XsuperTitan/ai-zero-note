package com.aizeronote.model;

public record AccuracyMetrics(
        Double whisperWer,
        Double challengerWer,
        String betterProvider
) {
}
