package com.aizeronote.model.dto.guidance;

import java.time.Instant;
import java.util.List;

public record StudyPlanResponse(
        long sessionId,
        String generationSource,
        String outlineMarkdown,
        List<String> suggestions,
        List<String> priorities,
        List<StudyPlanVideoDto> videos,
        String currentVideoId,
        Instant createdAt,
        Instant updatedAt
) {
}
