package com.aizeronote.model.dto.guidance;

import jakarta.validation.constraints.NotBlank;

public class GuidanceCurrentVideoUpdateRequest {

    @NotBlank
    private String currentVideoId;

    public String getCurrentVideoId() {
        return currentVideoId;
    }

    public void setCurrentVideoId(String currentVideoId) {
        this.currentVideoId = currentVideoId;
    }
}
