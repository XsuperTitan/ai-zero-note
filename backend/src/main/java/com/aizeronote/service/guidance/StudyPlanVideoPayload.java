package com.aizeronote.service.guidance;

import java.util.List;

public record StudyPlanVideoPayload(
        String id,
        String title,
        String platform,
        String url,
        String rationale,
        int sortOrder,
        String linkKind
) {
}
