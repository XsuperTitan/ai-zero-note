package com.aizeronote.model.dto.guidance;

public record GuidanceActiveProgressResponse(
        long sessionId,
        String status,
        String tutorPersona,
        String subjectOrTopic,
        String currentVideoId,
        String currentVideoTitle,
        String studyPlanPath
) {
}
