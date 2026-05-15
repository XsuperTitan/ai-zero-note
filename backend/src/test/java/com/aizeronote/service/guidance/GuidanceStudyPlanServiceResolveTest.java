package com.aizeronote.service.guidance;

import com.aizeronote.model.entity.GuidanceSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GuidanceStudyPlanServiceResolveTest {

    @Test
    void prefersPinnedVideoWhenValid() {
        GuidanceSession session = new GuidanceSession();
        session.setProgressVideoId("v-2");
        StudyPlanPayload payload = new StudyPlanPayload(
                "x",
                List.of(),
                List.of(),
                List.of(
                        new StudyPlanVideoPayload("v-1", "a", "P", "", "", 1, "S"),
                        new StudyPlanVideoPayload("v-2", "b", "P", "", "", 2, "S")
                ),
                "v-1"
        );
        assertThat(GuidanceStudyPlanService.resolveEffectiveCurrentVideoId(session, payload)).isEqualTo("v-2");
    }

    @Test
    void fallsBackWhenPinnedInvalid() {
        GuidanceSession session = new GuidanceSession();
        session.setProgressVideoId("missing");
        StudyPlanPayload payload = new StudyPlanPayload(
                "x",
                List.of(),
                List.of(),
                List.of(new StudyPlanVideoPayload("v-1", "a", "P", "", "", 1, "S")),
                "v-1"
        );
        assertThat(GuidanceStudyPlanService.resolveEffectiveCurrentVideoId(session, payload)).isEqualTo("v-1");
    }
}
