package com.aizeronote.service.guidance;

import com.aizeronote.model.entity.GuidanceCheckIn;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GuidanceCheckInPromptsTest {

    @Test
    void emptyWhenNoFields() {
        GuidanceCheckIn row = new GuidanceCheckIn();
        assertThat(GuidanceCheckInPrompts.formatCheckInSupplement(row)).isEmpty();
    }

    @Test
    void includesRemarkAndUrl() {
        GuidanceCheckIn row = new GuidanceCheckIn();
        row.setRemark("看完了第三章");
        row.setVideoUrl("https://search.bilibili.com/all?keyword=test");
        String s = GuidanceCheckInPrompts.formatCheckInSupplement(row);
        assertThat(s).contains("导学打卡").contains("看完了第三章").contains("bilibili");
    }
}
