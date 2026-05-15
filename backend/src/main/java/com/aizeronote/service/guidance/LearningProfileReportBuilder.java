package com.aizeronote.service.guidance;

import com.aizeronote.model.dto.guidance.LearningQuestionnaireSubmitRequest;
import com.aizeronote.model.guidance.ContentPreference;
import com.aizeronote.model.guidance.TutorPersona;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class LearningProfileReportBuilder {

    private LearningProfileReportBuilder() {
    }

    public static String buildReportSummary(LearningQuestionnaireSubmitRequest req) {
        Objects.requireNonNull(req);
        String personaLine = switch (req.getTutorPersona()) {
            case SILVER_WOLF -> "导学助手：骇客银狼（偏好高效路径、资源直链与可验证要点）";
            case STREAM_VETERAN -> "导学助手：流萤老兵（偏好稳扎稳打、复习节奏与清单化）";
            case NAMELESS_SELF -> "导学助手：无名客 / 自主学习（偏好少打扰、自控节奏）";
        };
        String urgencyLine = switch (req.getUrgency()) {
            case LOW -> "紧迫度：较低 —— 可按周计划分段推进。";
            case MEDIUM -> "紧迫度：中等 —— 建议先抓主干知识，再补细节。";
            case HIGH -> "紧迫度：较高 —— 优先最小可用知识集与高频考点，压缩拓展。";
        };
        String prefLine = switch (req.getContentPreference()) {
            case VIDEO -> "内容偏好：更依赖视频讲解与演示。";
            case ARTICLE -> "内容偏好：更依赖图文、文档与可检索文字。";
            case MIXED -> "内容偏好：视频 + 图文混合，视主题切换。";
        };

        StringBuilder body = new StringBuilder();
        body.append("## 学习习惯分析报告（阶段一 · 问卷快照）\n\n");
        body.append(personaLine).append("\n\n");
        body.append("### 学习焦点\n");
        body.append("- 主题：").append(trimOrPlaceholder(req.getSubjectOrTopic())).append("\n");
        body.append("- ").append(urgencyLine).append("\n\n");
        body.append("### 环境与节奏\n");
        body.append("- 主要学习平台：").append(trimOrPlaceholder(req.getPreferredPlatforms())).append("\n");
        body.append("- 常用网站 / 入口：").append(trimOrPlaceholder(req.getUsualSites())).append("\n");
        body.append("- 学习节奏：").append(trimOrPlaceholder(req.getStudyRhythm())).append("\n\n");
        body.append("### 形式偏好\n");
        body.append("- ").append(prefLine).append("\n\n");
        if (StringUtils.hasText(req.getExtraNotes())) {
            body.append("### 补充说明\n");
            body.append(req.getExtraNotes().trim()).append("\n\n");
        }
        body.append("---\n");
        body.append("*本报告由问卷即时生成，用作后续导学策略与 LLM 约束条件的依据。*\n");
        return body.toString();
    }

    public static String buildLlmPromptConstraints(LearningQuestionnaireSubmitRequest req) {
        Objects.requireNonNull(req);
        List<String> lines = new ArrayList<>();
        lines.add("You are a learning-guide copilot. Respect the learner profile below when suggesting resources, pacing, and note structure.");
        lines.add("Tutor persona (fixed for this session): " + req.getTutorPersona().name() + ".");
        lines.add("Learner topic: " + trimOrPlaceholder(req.getSubjectOrTopic()) + ".");
        lines.add("Urgency: " + req.getUrgency().name() + " — align depth vs. speed accordingly.");
        lines.add("Preferred platforms (plain text from user): " + trimOrPlaceholder(req.getPreferredPlatforms()) + ".");
        lines.add("Usual sites: " + trimOrPlaceholder(req.getUsualSites()) + ".");
        lines.add("Study rhythm: " + trimOrPlaceholder(req.getStudyRhythm()) + ".");
        lines.add("Content preference: " + req.getContentPreference().name() + ".");
        if (req.getContentPreference() == ContentPreference.VIDEO || req.getContentPreference() == ContentPreference.MIXED) {
            lines.add("When recommending videos, prefer the learner's stated platforms; label platform and avoid fabricated URLs.");
        }
        if (req.getTutorPersona() == TutorPersona.SILVER_WOLF) {
            lines.add("Tone: concise, direct, slightly playful 'hacker' mentor; prioritize actionable steps and checkable outcomes.");
        }
        if (StringUtils.hasText(req.getExtraNotes())) {
            lines.add("Learner notes: " + req.getExtraNotes().trim());
        }
        return String.join("\n", lines);
    }

    private static String trimOrPlaceholder(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "（未填写）";
        }
        return raw.trim();
    }
}
