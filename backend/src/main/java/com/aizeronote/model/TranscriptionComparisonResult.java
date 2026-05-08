package com.aizeronote.model;

public record TranscriptionComparisonResult(
        String sourceFilename,
        String whisperTranscription,
        String challengerTranscription,
        AccuracyMetrics metrics,
        String note
) {
}
