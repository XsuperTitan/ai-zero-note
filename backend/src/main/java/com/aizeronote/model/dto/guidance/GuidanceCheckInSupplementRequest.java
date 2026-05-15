package com.aizeronote.model.dto.guidance;

import jakarta.validation.constraints.Size;

public class GuidanceCheckInSupplementRequest {

    @Size(max = 2048)
    private String videoUrl;

    @Size(max = 120000)
    private String transcriptText;

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getTranscriptText() {
        return transcriptText;
    }

    public void setTranscriptText(String transcriptText) {
        this.transcriptText = transcriptText;
    }
}
