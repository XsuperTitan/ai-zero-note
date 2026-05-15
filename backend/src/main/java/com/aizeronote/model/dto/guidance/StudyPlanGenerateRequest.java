package com.aizeronote.model.dto.guidance;

import com.aizeronote.model.guidance.StudyPlanGenerationMode;
import jakarta.validation.constraints.NotNull;

public class StudyPlanGenerateRequest {

    @NotNull
    private Long sessionId;

    /** When null, defaults to {@link StudyPlanGenerationMode#TEMPLATE}. */
    private StudyPlanGenerationMode mode;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public StudyPlanGenerationMode getMode() {
        return mode;
    }

    public void setMode(StudyPlanGenerationMode mode) {
        this.mode = mode;
    }
}
