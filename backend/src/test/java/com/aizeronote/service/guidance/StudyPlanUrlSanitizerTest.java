package com.aizeronote.service.guidance;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StudyPlanUrlSanitizerTest {

    @Test
    void keepsWhitelistedHost() {
        assertThat(
                StudyPlanUrlSanitizer.sanitizeOrBlank("https://search.bilibili.com/all?keyword=test")
        ).isEqualTo("https://search.bilibili.com/all?keyword=test");
    }

    @Test
    void stripsUnknownHost() {
        assertThat(StudyPlanUrlSanitizer.sanitizeOrBlank("https://evil.example/video")).isEmpty();
    }
}
