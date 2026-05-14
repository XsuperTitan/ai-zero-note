package com.aizeronote.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ImageNoteStartRequest(
        @NotBlank @Size(max = 120000) String sourceText
) {
}
