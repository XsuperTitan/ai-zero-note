package com.aizeronote.model.dto.guidance;

import java.time.Instant;

public record GuidanceProfileResponse(
        long sessionId,
        String tutorPersona,
        String status,
        String reportSummary,
        String llmPromptConstraints,
        Instant createdAt,
        Instant updatedAt
) {
}
