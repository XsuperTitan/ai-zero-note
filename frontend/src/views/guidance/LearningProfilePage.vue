<script setup lang="ts">
import { onMounted, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import {
  generateStudyPlan,
  getLatestLearningProfile,
  submitLearningProfile,
  type ContentPreference,
  type GuidanceProfileResponse,
  type LearningUrgency,
  type TutorPersona
} from "../../api/guidance";
import { getLoginUser } from "../../api/user";

const router = useRouter();

const tutorPersona = ref<TutorPersona>("SILVER_WOLF");
const subjectOrTopic = ref("");
const urgency = ref<LearningUrgency>("MEDIUM");
const preferredPlatforms = ref("");
const usualSites = ref("");
const studyRhythm = ref("");
const contentPreference = ref<ContentPreference>("MIXED");
const extraNotes = ref("");

const loading = ref(false);
const planLoading = ref(false);
const bootLoading = ref(true);
const errorMessage = ref("");
const latest = ref<GuidanceProfileResponse | null>(null);

onMounted(async () => {
  try {
    const user = await getLoginUser();
    if (!user) {
      await router.replace("/user/login");
      return;
    }
    try {
      latest.value = await getLatestLearningProfile();
    } catch {
      latest.value = null;
    }
  } finally {
    bootLoading.value = false;
  }
});

async function onSubmit() {
  if (loading.value) {
    return;
  }
  errorMessage.value = "";
  loading.value = true;
  try {
    const user = await getLoginUser();
    if (!user) {
      await router.replace("/user/login");
      return;
    }
    latest.value = await submitLearningProfile({
      tutorPersona: tutorPersona.value,
      subjectOrTopic: subjectOrTopic.value.trim(),
      urgency: urgency.value,
      preferredPlatforms: preferredPlatforms.value.trim(),
      usualSites: usualSites.value.trim(),
      studyRhythm: studyRhythm.value.trim(),
      contentPreference: contentPreference.value,
      extraNotes: extraNotes.value.trim() || undefined
    });
  } catch (e) {
    errorMessage.value = e instanceof Error ? e.message : "提交失败";
  } finally {
    loading.value = false;
  }
}

async function generatePlanAndOpen() {
  if (!latest.value || planLoading.value) {
    return;
  }
  planLoading.value = true;
  errorMessage.value = "";
  try {
    await generateStudyPlan(latest.value.sessionId, "TEMPLATE");
    await router.push({ path: "/guidance/plan", query: { sessionId: String(latest.value.sessionId) } });
  } catch (e) {
    errorMessage.value = e instanceof Error ? e.message : "生成方案失败";
  } finally {
    planLoading.value = false;
  }
}
</script>

<template>
  <main class="page-guidance">
    <h1 class="cyber-display">沉浸导学 · 学习习惯问卷</h1>
    <p class="cyber-muted page-lead">
      阶段一：提交问卷后，后端会生成可复用的「学习习惯分析报告」与 LLM 约束文本，供后续定制学习方案使用。
    </p>

    <p v-if="bootLoading" class="cyber-muted">加载中…</p>

    <template v-else>
      <section class="cyber-card">
        <h2>问卷</h2>

        <label class="cyber-field-label">导学助手</label>
        <select v-model="tutorPersona" class="cyber-field">
          <option value="SILVER_WOLF">骇客银狼（高效路径 / 资源导向）</option>
          <option value="STREAM_VETERAN">流萤老兵（节奏 / 复习清单）</option>
          <option value="NAMELESS_SELF">无名客（自主学习、少打扰）</option>
        </select>

        <label class="cyber-field-label">正在学的内容（主题或科目）</label>
        <input v-model="subjectOrTopic" class="cyber-field" type="text" placeholder="例如：线性代数 — 特征值" />

        <label class="cyber-field-label">紧迫程度</label>
        <select v-model="urgency" class="cyber-field">
          <option value="LOW">低 —— 可按周推进</option>
          <option value="MEDIUM">中 —— 需要先抓主干</option>
          <option value="HIGH">高 —— 优先最小可用集</option>
        </select>

        <label class="cyber-field-label">主要学习平台</label>
        <input
          v-model="preferredPlatforms"
          class="cyber-field"
          type="text"
          placeholder="例如：哔哩哔哩、Coursera、教材 PDF…"
        />

        <label class="cyber-field-label">常用网站 / 收藏入口</label>
        <input v-model="usualSites" class="cyber-field" type="text" placeholder="例如：某个 UP 主页、课程目录链接…" />

        <label class="cyber-field-label">学习节奏</label>
        <input v-model="studyRhythm" class="cyber-field" type="text" placeholder="例如：工作日 1h / 周末集中 4h" />

        <label class="cyber-field-label">内容形式偏好</label>
        <select v-model="contentPreference" class="cyber-field">
          <option value="VIDEO">更偏视频</option>
          <option value="ARTICLE">更偏图文</option>
          <option value="MIXED">混合</option>
        </select>

        <label class="cyber-field-label">补充说明（可选）</label>
        <textarea v-model="extraNotes" class="cyber-field" rows="4" placeholder="学习目标、考试日期、禁忌等" />

        <button type="button" class="cyber-btn-primary" :disabled="loading" @click="onSubmit">
          {{ loading ? "提交中…" : "提交并生成报告" }}
        </button>
        <p v-if="errorMessage" class="cyber-error">{{ errorMessage }}</p>
      </section>

      <section v-if="latest" class="cyber-card report-card">
        <h2>最新报告</h2>
        <p class="cyber-muted">
          sessionId: {{ latest.sessionId }} · status: {{ latest.status }} · persona: {{ latest.tutorPersona }}
        </p>
        <pre class="cyber-pre report-pre">{{ latest.reportSummary }}</pre>

        <h3 class="section-mini-title">LLM 约束（供后续导学 / 笔记管线引用）</h3>
        <pre class="cyber-pre constraints-pre">{{ latest.llmPromptConstraints }}</pre>

        <p class="plan-actions">
          <RouterLink
            class="cyber-link"
            :to="{ path: '/guidance/plan', query: { sessionId: String(latest.sessionId) } }"
          >查看学习方案</RouterLink>
          <button type="button" class="cyber-btn-primary plan-btn" :disabled="planLoading" @click="generatePlanAndOpen">
            {{ planLoading ? "生成中…" : "生成并打开方案（模板）" }}
          </button>
        </p>
      </section>
    </template>
  </main>
</template>

<style scoped>
.page-guidance {
  margin: 0 auto;
  max-width: 900px;
  padding: 2rem 1rem 3rem;
}

.page-guidance h1 {
  font-size: clamp(1.25rem, 4vw, 1.75rem);
  margin: 0 0 0.65rem;
}

.page-lead {
  margin: 0 0 1rem;
}

.section-mini-title {
  margin: 1rem 0 0.35rem;
  font-size: 0.92rem;
  font-weight: 600;
}

.report-pre {
  white-space: pre-wrap;
  word-break: break-word;
}

.constraints-pre {
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 320px;
  overflow: auto;
}

.plan-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem 1rem;
  margin-top: 1rem;
}

.plan-btn {
  margin: 0;
}
</style>
