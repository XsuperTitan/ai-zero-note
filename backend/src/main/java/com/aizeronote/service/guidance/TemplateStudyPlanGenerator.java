package com.aizeronote.service.guidance;

import com.aizeronote.model.dto.guidance.LearningQuestionnaireSubmitRequest;
import com.aizeronote.model.guidance.ContentPreference;
import com.aizeronote.model.guidance.LearningUrgency;
import com.aizeronote.model.guidance.TutorPersona;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class TemplateStudyPlanGenerator {

    public StudyPlanPayload generate(LearningQuestionnaireSubmitRequest q, String reportSummary) {
        String keyword = extractSearchKeyword(q.getSubjectOrTopic());
        boolean preferBili = prefersBilibili(q.getPreferredPlatforms() + " " + q.getUsualSites());
        String primarySearchBase = preferBili
                ? "https://search.bilibili.com/all?keyword="
                : "https://www.youtube.com/results?search_query=";

        String outline = buildOutline(q, reportSummary);
        List<String> suggestions = buildSuggestions(q);
        List<String> priorities = buildPriorities(q);

        List<StudyPlanVideoPayload> videos = new ArrayList<>();
        videos.add(new StudyPlanVideoPayload(
                "v-1",
                "主线入门：" + trunc(q.getSubjectOrTopic(), 48),
                preferBili ? "BILIBILI" : "YOUTUBE",
                primarySearchBase + URLEncoder.encode(keyword + " 入门", StandardCharsets.UTF_8),
                "先建立整体框架与术语共识；链接为平台内搜索结果占位，请自行点选可信稿件。",
                1,
                "SEARCH_PLACEHOLDER"
        ));
        videos.add(new StudyPlanVideoPayload(
                "v-2",
                "关键难点拆解：" + trunc(q.getSubjectOrTopic(), 40),
                preferBili ? "BILIBILI" : "YOUTUBE",
                primarySearchBase + URLEncoder.encode(keyword + " 详解", StandardCharsets.UTF_8),
                "对照你的节奏与教材，补齐「卡住就会全线崩」的短板。",
                2,
                "SEARCH_PLACEHOLDER"
        ));
        videos.add(new StudyPlanVideoPayload(
                "v-3",
                "练习与复盘",
                preferBili ? "BILIBILI" : "YOUTUBE",
                primarySearchBase + URLEncoder.encode(keyword + " 习题 复盘", StandardCharsets.UTF_8),
                "用小题频反馈验证理解；可把稿主的讲义当清单而非唯一来源。",
                3,
                "SEARCH_PLACEHOLDER"
        ));

        return new StudyPlanPayload(outline, suggestions, priorities, videos, "v-1");
    }

    private static String buildOutline(LearningQuestionnaireSubmitRequest q, String reportSummary) {
        StringBuilder md = new StringBuilder();
        md.append("# 定制学习路径 · ").append(trimLine(q.getSubjectOrTopic())).append("\n\n");
        md.append("## 目标对齐\n");
        md.append("- 紧迫度：**").append(urgencyLabel(q.getUrgency())).append("**\n");
        md.append("- 形式偏好：**").append(contentLabel(q.getContentPreference())).append("**\n");
        md.append("- 学习节奏：").append(trimLine(q.getStudyRhythm())).append("\n\n");
        md.append("## 知识地图（大纲）\n");
        md.append("1. **概念层**：核心定义、符号与语境。\n");
        md.append("2. **过程层**：典型流程 / 推导 / 算法步骤。\n");
        md.append("3. **应用层**：例题、常见坑与迁移场景。\n");
        md.append("4. **复盘层**：错题模式、自检清单。\n\n");
        md.append("## 与问卷快照的关系\n");
        md.append("以下为画像报告摘要（便于分享或回查）：\n\n");
        md.append(StringUtils.hasText(reportSummary) ? reportSummary : "（暂无报告摘要）");
        md.append("\n");
        return md.toString();
    }

    private static List<String> buildSuggestions(LearningQuestionnaireSubmitRequest q) {
        List<String> lines = new ArrayList<>();
        switch (q.getTutorPersona()) {
            case SILVER_WOLF ->
                    lines.add("骇客银狼模式：每周只做「一条主线视频 + 一页自测」，避免信息堆到爆栈。");
            case STREAM_VETERAN -> lines.add("流萤老兵模式：固定复习节律（例如 1-3-7 天），主线视频只做标记点。");
            case NAMELESS_SELF -> lines.add("无名客模式：把大纲拆成可独立完成的块，少同步、多异步。");
        }
        if (q.getContentPreference() != ContentPreference.VIDEO) {
            lines.add("你更偏图文：每个视频节点尽量补一段文字笔记或官方文档链接（自行收藏）。");
        } else {
            lines.add("你更偏视频：控制单次观看时长，切「章节节点」而不是一口气刷完。");
        }
        lines.add("平台习惯：" + trimLine(q.getPreferredPlatforms()) + " —— 优先用你已熟悉的入口检索，减少冷启动成本。");
        lines.add("若链接为搜索占位：点开结果后自选 **弹幕/评论区口碑** 尚可的稿件，并记录 BV/链接到你的笔记里。");
        if (StringUtils.hasText(q.getExtraNotes())) {
            lines.add("你的补充目标：" + q.getExtraNotes().trim());
        }
        return lines;
    }

    private static List<String> buildPriorities(LearningQuestionnaireSubmitRequest q) {
        List<String> p = new ArrayList<>();
        switch (q.getUrgency()) {
            case HIGH -> {
                p.add("P0：最小必会集（考试/面试硬卡点）");
                p.add("P1：一条主线串讲 + 一份错题回炉");
                p.add("P2：拓展阅读（可整体延后）");
            }
            case MEDIUM -> {
                p.add("P0：概念定义 + 代表性例题");
                p.add("P1：综合题与易混点对照");
                p.add("P2：背景拓展与社区优质解析");
            }
            case LOW -> {
                p.add("P0：建立兴趣锚点与长期知识树");
                p.add("P1：系统课或权威书籍章节对齐");
                p.add("P2：项目式实践（按需）");
            }
        }
        return p;
    }

    private static boolean prefersBilibili(String haystack) {
        if (!StringUtils.hasText(haystack)) {
            return true;
        }
        String h = haystack.toLowerCase();
        return h.contains("bili") || h.contains("哔哩") || h.contains("bilibili");
    }

    private static String extractSearchKeyword(String subject) {
        if (!StringUtils.hasText(subject)) {
            return "学习目标";
        }
        String line = subject.split("\\R")[0].trim();
        return line.length() > 80 ? line.substring(0, 80) : line;
    }

    private static String trunc(String s, int max) {
        if (!StringUtils.hasText(s)) {
            return "";
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }

    private static String trimLine(String s) {
        return StringUtils.hasText(s) ? s.trim() : "（未填写）";
    }

    private static String urgencyLabel(LearningUrgency u) {
        return switch (u) {
            case LOW -> "低 —— 可从容铺厚基础";
            case MEDIUM -> "中 —— 主干优先、细节迭代";
            case HIGH -> "高 —— 抢最小可用集与高频得分点";
        };
    }

    private static String contentLabel(ContentPreference c) {
        return switch (c) {
            case VIDEO -> "视频为主";
            case ARTICLE -> "图文为主";
            case MIXED -> "视频 + 图文混合";
        };
    }
}
