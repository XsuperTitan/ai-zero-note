package com.aizeronote.service.guidance;

import java.util.List;

public record StudyPlanPayload(
        String outlineMarkdown,
        List<String> suggestions,
        List<String> priorities,
        List<StudyPlanVideoPayload> videos,
        String currentVideoId
) {
}
