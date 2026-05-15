package com.aizeronote.model.dto.guidance;

import java.time.Instant;

public record GuidanceCheckInResponse(
        long checkInId,
        long sessionId,
        String remark,
        String videoUrl,
        boolean hasTranscriptText,
        String consumedNoteId,
        Instant createdAt,
        Instant updatedAt
) {
}
