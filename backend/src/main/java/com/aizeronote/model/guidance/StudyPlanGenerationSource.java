package com.aizeronote.model.guidance;

public final class StudyPlanGenerationSource {

    public static final String TEMPLATE = "TEMPLATE";

    public static final String LLM = "LLM";

    /** LLM call failed or returned invalid JSON; template plan was used instead. */
    public static final String LLM_FALLBACK = "LLM_FALLBACK";

    private StudyPlanGenerationSource() {
    }
}
