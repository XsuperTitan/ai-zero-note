package com.aizeronote.model.dto.history;

import java.time.Instant;

public record NoteJobDetailDto(
        String noteId,
        String sourceLabel,
        String title,
        String abstractExcerpt,
        String markdownFileName,
        String downloadUrl,
        Instant createdAt
) {}
