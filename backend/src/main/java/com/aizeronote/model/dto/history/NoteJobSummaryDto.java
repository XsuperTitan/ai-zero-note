package com.aizeronote.model.dto.history;

import java.time.Instant;

public record NoteJobSummaryDto(
        String noteId,
        String sourceLabel,
        String title,
        String abstractExcerpt,
        String downloadUrl,
        Instant createdAt
) {}
