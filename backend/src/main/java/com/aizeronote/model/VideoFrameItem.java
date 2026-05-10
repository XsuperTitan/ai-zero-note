package com.aizeronote.model;

public record VideoFrameItem(
        String fileName,
        String imageUrl,
        long presentationTimestamp
) {
}
