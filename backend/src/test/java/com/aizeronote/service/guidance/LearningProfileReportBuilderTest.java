package com.aizeronote.service.guidance;

import com.aizeronote.model.dto.guidance.LearningQuestionnaireSubmitRequest;
import com.aizeronote.model.guidance.ContentPreference;
import com.aizeronote.model.guidance.LearningUrgency;
import com.aizeronote.model.guidance.TutorPersona;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LearningProfileReportBuilderTest {

    @Test
    void buildsSummaryAndConstraintsForSilverWolf() {
        LearningQuestionnaireSubmitRequest req = new LearningQuestionnaireSubmitRequest();
        req.setTutorPersona(TutorPersona.SILVER_WOLF);
        req.setSubjectOrTopic("Rust ownership");
        req.setUrgency(LearningUrgency.HIGH);
        req.setPreferredPlatforms("Bilibili、YouTube");
        req.setUsualSites("课程直达收藏夹");
        req.setStudyRhythm("工作日每晚 1 小时");
        req.setContentPreference(ContentPreference.MIXED);
        req.setExtraNotes("希望例子贴近 CLI 工具");

        String summary = LearningProfileReportBuilder.buildReportSummary(req);
        String constraints = LearningProfileReportBuilder.buildLlmPromptConstraints(req);

        assertThat(summary).contains("骇客银狼").contains("Rust ownership").contains("较高");
        assertThat(constraints)
                .contains("SILVER_WOLF")
                .contains("Rust ownership")
                .contains("MIXED")
                .contains("hacker");
    }
}
