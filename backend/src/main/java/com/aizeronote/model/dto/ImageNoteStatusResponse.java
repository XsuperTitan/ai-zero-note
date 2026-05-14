package com.aizeronote.model.dto;

public record ImageNoteStatusResponse(String jobId, String status, String errorMessage, String downloadUrl) {
}
