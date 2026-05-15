package com.aizeronote.service.guidance;

import com.aizeronote.model.dto.guidance.LearningQuestionnaireSubmitRequest;
import com.aizeronote.model.guidance.ContentPreference;
import com.aizeronote.model.guidance.LearningUrgency;
import com.aizeronote.model.guidance.TutorPersona;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateStudyPlanGeneratorTest {

    @Test
    void generatesVideosWithCurrentPointer() {
        LearningQuestionnaireSubmitRequest q = new LearningQuestionnaireSubmitRequest();
        q.setTutorPersona(TutorPersona.SILVER_WOLF);
        q.setSubjectOrTopic("树状数组");
        q.setUrgency(LearningUrgency.MEDIUM);
        q.setPreferredPlatforms("哔哩哔哩");
        q.setUsualSites("收藏夹");
        q.setStudyRhythm("每晚 1h");
        q.setContentPreference(ContentPreference.VIDEO);

        TemplateStudyPlanGenerator gen = new TemplateStudyPlanGenerator();
        StudyPlanPayload plan = gen.generate(q, "报告摘要");

        assertThat(plan.currentVideoId()).isEqualTo("v-1");
        assertThat(plan.videos()).hasSize(3);
        assertThat(plan.videos().get(0).url()).contains("search.bilibili.com");
        assertThat(plan.outlineMarkdown()).contains("树状数组");
    }
}
