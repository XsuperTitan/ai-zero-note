package com.aizeronote.model.guidance;

public final class GuidanceSessionStatus {

    public static final String PROFILE_READY = "PROFILE_READY";

    /** Questionnaire done and a study plan has been generated for this session. */
    public static final String PLAN_READY = "PLAN_READY";

    private GuidanceSessionStatus() {
    }
}
