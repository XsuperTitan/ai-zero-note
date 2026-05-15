package com.aizeronote.service.guidance;

import com.aizeronote.model.entity.GuidanceCheckIn;
import org.springframework.util.StringUtils;

public final class GuidanceCheckInPrompts {

    private GuidanceCheckInPrompts() {
    }

    /**
     * Text merged into {@link com.aizeronote.service.SummaryService#summarizeWithSupplemental} as supplemental material.
     */
    public static String formatCheckInSupplement(GuidanceCheckIn row) {
        if (row == null) {
            return "";
        }
        boolean any = StringUtils.hasText(row.getRemark())
                || StringUtils.hasText(row.getVideoUrl())
                || StringUtils.hasText(row.getTranscriptText());
        if (!any) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("【导学打卡素材 — 用户学后补充，生成笔记时请结合以下内容】\n");
        if (StringUtils.hasText(row.getRemark())) {
            sb.append("打卡备注：").append(row.getRemark().trim()).append("\n");
        }
        if (StringUtils.hasText(row.getVideoUrl())) {
            sb.append("相关视频链接：").append(row.getVideoUrl().trim()).append("\n");
        }
        if (StringUtils.hasText(row.getTranscriptText())) {
            sb.append("摘录/转写文本：\n").append(row.getTranscriptText().trim()).append("\n");
        }
        return sb.toString().trim();
    }
}
