package com.aizeronote.model;

public record VideoMetaResult(
        String sourceUrl,
        String title,
        long durationSeconds,
        String durationText,
        String uploader,
        String thumbnailUrl
) {
}
