package com.aizeronote.model.guidance;

public final class GuidanceSessionStatus {

    /** Questionnaire submitted (profile / 画像就绪). */
    public static final String PROFILED = "PROFILED";

    /** Study plan generated; learner has not started execution yet. */
    public static final String PLAN_READY = "PLAN_READY";

    /** Learner is following the plan (in-session progress). */
    public static final String IN_PROGRESS = "IN_PROGRESS";

    /** Learner marked this guidance run complete. */
    public static final String COMPLETED = "COMPLETED";

    private GuidanceSessionStatus() {
    }
}
