package com.aizeronote.model;

public record VideoTextResult(
        VideoMetaResult meta,
        String subtitleText,
        String textContent
) {
}
