package com.aizeronote.model.dto.history;

import java.time.Instant;

public record VideoTaskDetailDto(
        String taskId,
        String sourceUrl,
        String titleSnapshot,
        Integer durationSeconds,
        Integer frameCount,
        Instant createdAt
) {}
