package com.aizeronote.model.dto.guidance;

import java.util.List;

public record StudyPlanVideoDto(
        String id,
        String title,
        String platform,
        String url,
        String rationale,
        int sortOrder,
        String linkKind
) {
}
