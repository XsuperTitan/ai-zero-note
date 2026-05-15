<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from "vue";
import { useRoute, useRouter, RouterLink } from "vue-router";
import {
  completeGuidanceSession,
  createGuidanceCheckIn,
  enterGuidanceSessionInProgress,
  generateStudyPlan,
  getLatestStudyPlan,
  getStudyPlanBySessionId,
  supplementGuidanceCheckIn,
  updateGuidanceCurrentVideo,
  type PlanGenerationMode,
  type StudyPlanResponse
} from "../../api/guidance";
import { getLoginUser } from "../../api/user";

const route = useRoute();
const router = useRouter();

const bootLoading = ref(true);
const loading = ref(false);
const errorMessage = ref("");
const plan = ref<StudyPlanResponse | null>(null);
/** When URL has sessionId but plan not generated yet */
const pendingSessionId = ref<number | null>(null);
const generationMode = ref<PlanGenerationMode>("TEMPLATE");

const checkInRemark = ref("");
const checkInVideoUrl = ref("");
const checkInTranscript = ref("");
const lastCheckInId = ref<number | null>(null);
const checkInLoading = ref(false);
const checkInMessage = ref("");

const sessionIdQuery = computed(() => {
  const raw = route.query.sessionId;
  if (raw == null || Array.isArray(raw)) {
    return null;
  }
  const s = String(raw).trim();
  if (!/^\d+$/.test(s)) {
    return null;
  }
  const n = Number(s);
  return Number.isSafeInteger(n) ? n : null;
});

const sharePath = computed(() => {
  if (!plan.value) {
    return "";
  }
  return `${window.location.origin}/guidance/plan?sessionId=${String(plan.value.sessionId)}`;
});

onMounted(async () => {
  try {
    const user = await getLoginUser();
    if (!user) {
      await router.replace("/user/login");
      return;
    }
    await loadPlan();
  } finally {
    bootLoading.value = false;
  }
});

function isNoPlanMessage(msg: string): boolean {
  return msg.includes("暂无学习方案") || msg.includes("请先生成");
}

async function loadPlan() {
  errorMessage.value = "";
  const sid = sessionIdQuery.value;
  if (sid != null) {
    try {
      plan.value = await getStudyPlanBySessionId(sid);
      pendingSessionId.value = null;
      await afterPlanLoaded();
      return;
    } catch (e) {
      const msg = e instanceof Error ? e.message : String(e);
      if (isNoPlanMessage(msg)) {
        plan.value = null;
        pendingSessionId.value = sid;
        return;
      }
      errorMessage.value = msg;
      return;
    }
  }
  try {
    plan.value = await getLatestStudyPlan();
    pendingSessionId.value = null;
    await afterPlanLoaded();
  } catch (e) {
    plan.value = null;
    pendingSessionId.value = null;
    errorMessage.value =
      e instanceof Error ? e.message : "暂无学习方案：请先在问卷页提交，再通过「学习方案」进入并生成。";
  }
}

async function onGenerate() {
  const sid = pendingSessionId.value ?? plan.value?.sessionId ?? sessionIdQuery.value;
  if (sid == null) {
    errorMessage.value = "缺少 sessionId：请从问卷页进入。";
    return;
  }
  if (loading.value) {
    return;
  }
  loading.value = true;
  errorMessage.value = "";
  try {
    plan.value = await generateStudyPlan(sid, generationMode.value);
    pendingSessionId.value = null;
    await router.replace({ path: "/guidance/plan", query: { sessionId: String(sid) } });
    await afterPlanLoaded();
  } catch (e) {
    errorMessage.value = e instanceof Error ? e.message : "生成失败";
  } finally {
    loading.value = false;
  }
}

async function copyShareLink() {
  if (!sharePath.value) {
    return;
  }
  try {
    await navigator.clipboard.writeText(sharePath.value);
  } catch {
    errorMessage.value = "复制失败，请手动复制地址栏链接。";
  }
}

async function copyOutlineText() {
  if (!plan.value) {
    return;
  }
  const lines: string[] = [];
  lines.push(plan.value.outlineMarkdown.trim());
  lines.push("");
  lines.push("## 建议");
  for (const s of plan.value.suggestions) {
    lines.push(`- ${s}`);
  }
  lines.push("");
  lines.push("## 优先级");
  for (const p of plan.value.priorities) {
    lines.push(`- ${p}`);
  }
  lines.push("");
  lines.push("## 视频路线");
  for (const v of plan.value.videos) {
    const mark = v.id === plan.value.currentVideoId ? "【当前先看】" : "";
    lines.push(`${mark}${v.sortOrder}. ${v.title} (${v.platform})`);
    if (v.url) {
      lines.push(`   ${v.url}`);
    }
    lines.push(`   ${v.rationale}`);
  }
  try {
    await navigator.clipboard.writeText(lines.join("\n"));
  } catch {
    errorMessage.value = "复制失败。";
  }
}

const advanceLoading = ref(false);

const hasNextVideo = computed(() => {
  const p = plan.value;
  if (!p?.videos?.length) {
    return false;
  }
  const sorted = [...p.videos].sort((a, b) => a.sortOrder - b.sortOrder);
  const idx = sorted.findIndex((v) => v.id === p.currentVideoId);
  return idx >= 0 && idx < sorted.length - 1;
});

async function afterPlanLoaded() {
  const p = plan.value;
  if (!p?.sessionId) {
    return;
  }
  if (p.sessionStatus === "PLAN_READY") {
    try {
      await enterGuidanceSessionInProgress(p.sessionId);
      plan.value = await getStudyPlanBySessionId(p.sessionId);
    } catch {
      /* ignore */
    }
  }
  await scrollToCurrentVideo();
}

async function scrollToCurrentVideo() {
  const id = plan.value?.currentVideoId;
  if (!id) {
    return;
  }
  await nextTick();
  document.getElementById(`guid-video-${id}`)?.scrollIntoView({ behavior: "smooth", block: "center" });
}

async function onNextVideo() {
  const p = plan.value;
  if (!p || advanceLoading.value) {
    return;
  }
  const sorted = [...p.videos].sort((a, b) => a.sortOrder - b.sortOrder);
  const idx = sorted.findIndex((v) => v.id === p.currentVideoId);
  const next = sorted[idx + 1];
  if (!next) {
    return;
  }
  advanceLoading.value = true;
  errorMessage.value = "";
  try {
    await updateGuidanceCurrentVideo(p.sessionId, next.id);
    plan.value = await getStudyPlanBySessionId(p.sessionId);
    await scrollToCurrentVideo();
  } catch (e) {
    errorMessage.value = e instanceof Error ? e.message : "更新失败";
  } finally {
    advanceLoading.value = false;
  }
}

async function onCompleteGuidance() {
  const p = plan.value;
  if (!p || advanceLoading.value) {
    return;
  }
  advanceLoading.value = true;
  errorMessage.value = "";
  try {
    await completeGuidanceSession(p.sessionId);
    plan.value = await getStudyPlanBySessionId(p.sessionId);
  } catch (e) {
    errorMessage.value = e instanceof Error ? e.message : "操作失败";
  } finally {
    advanceLoading.value = false;
  }
}

async function onSubmitCheckIn() {
  const p = plan.value;
  if (!p || checkInLoading.value) {
    return;
  }
  checkInLoading.value = true;
  checkInMessage.value = "";
  try {
    const r = await createGuidanceCheckIn(p.sessionId, checkInRemark.value.trim() || undefined);
    lastCheckInId.value = r.checkInId;
    checkInMessage.value = `打卡已保存。可补充链接/转写后点「更新补充素材」，再「去首页生成笔记」。`;
  } catch (e) {
    checkInMessage.value = e instanceof Error ? e.message : "打卡失败";
  } finally {
    checkInLoading.value = false;
  }
}

async function onSupplementCheckIn() {
  if (lastCheckInId.value == null || checkInLoading.value) {
    return;
  }
  checkInLoading.value = true;
  checkInMessage.value = "";
  try {
    const r = await supplementGuidanceCheckIn(lastCheckInId.value, {
      videoUrl: checkInVideoUrl.value.trim() || undefined,
      transcriptText: checkInTranscript.value.trim() || undefined
    });
    checkInMessage.value = `补充已更新（打卡 #${r.checkInId}）。`;
  } catch (e) {
    checkInMessage.value = e instanceof Error ? e.message : "更新失败";
  } finally {
    checkInLoading.value = false;
  }
}

function goNotesWithCheckIn() {
  if (lastCheckInId.value == null) {
    return;
  }
  void router.push({ path: "/", query: { guidanceCheckInId: String(lastCheckInId.value) } });
}
</script>

<template>
  <main class="page-plan">
    <h1 class="cyber-display">定制学习方案</h1>
    <p class="cyber-muted page-lead">
      展示知识大纲、学习建议、优先级与推荐视频；「当前先看」对应后端的
      <code>currentVideoId</code>。视频链接第一期以平台搜索占位为主，避免杜撰具体稿件。
    </p>

    <p v-if="bootLoading" class="cyber-muted">加载中…</p>

    <template v-else>
      <section v-if="pendingSessionId != null && plan == null" class="cyber-card">
        <h2>尚未生成方案</h2>
        <p class="cyber-muted">会话 ID：{{ pendingSessionId }}</p>
        <label class="cyber-field-label">生成方式</label>
        <select v-model="generationMode" class="cyber-field">
          <option value="TEMPLATE">模板（默认，无需 LLM）</option>
          <option value="LLM">LLM（失败则自动回退模板）</option>
        </select>
        <button type="button" class="cyber-btn-primary" :disabled="loading" @click="onGenerate">
          {{ loading ? "生成中…" : "生成学习方案" }}
        </button>
        <p v-if="errorMessage" class="cyber-error">{{ errorMessage }}</p>
      </section>

      <section v-else-if="plan" class="cyber-card">
        <div class="plan-toolbar">
          <p class="cyber-muted">
            sessionId {{ plan.sessionId }} · 状态 {{ plan.sessionStatus ?? "—" }} · source {{ plan.generationSource }} ·
            当前 <code>{{ plan.currentVideoId }}</code>
          </p>
          <div class="toolbar-actions">
            <button type="button" class="cyber-btn-ghost" @click="copyShareLink">复制分享链接</button>
            <button type="button" class="cyber-btn-ghost" @click="copyOutlineText">复制全文（大纲+列表）</button>
            <button
              v-if="plan.sessionStatus !== 'COMPLETED'"
              type="button"
              class="cyber-btn-ghost"
              :disabled="advanceLoading || !hasNextVideo"
              @click="onNextVideo"
            >
              {{ advanceLoading ? "…" : "下一条视频" }}
            </button>
            <button
              v-if="plan.sessionStatus !== 'COMPLETED'"
              type="button"
              class="cyber-btn-ghost"
              :disabled="advanceLoading"
              @click="onCompleteGuidance"
            >
              标记本阶段完成
            </button>
            <button type="button" class="cyber-btn-ghost" :disabled="loading" @click="onGenerate">
              {{ loading ? "…" : "重新生成" }}
            </button>
          </div>
        </div>
        <label class="cyber-field-label inline-label">重新生成模式</label>
        <select v-model="generationMode" class="cyber-field narrow-select">
          <option value="TEMPLATE">模板</option>
          <option value="LLM">LLM</option>
        </select>

        <p v-if="plan.sessionStatus === 'COMPLETED'" class="cyber-muted">
          本会话已标记完成。仍可查看大纲与复制全文；重新生成将覆盖现有方案。
        </p>

        <h2>大纲</h2>
        <pre class="cyber-pre outline-pre">{{ plan.outlineMarkdown }}</pre>

        <h2>学习建议</h2>
        <ul class="cyber-list">
          <li v-for="(s, i) in plan.suggestions" :key="'s' + i">{{ s }}</li>
        </ul>

        <h2>优先级</h2>
        <ol class="cyber-list ordered">
          <li v-for="(p, i) in plan.priorities" :key="'p' + i">{{ p }}</li>
        </ol>

        <h2>推荐视频</h2>
        <ul class="video-list">
          <li
            v-for="v in plan.videos"
            :id="'guid-video-' + v.id"
            :key="v.id"
            class="video-item"
            :class="{ 'video-item--current': v.id === plan.currentVideoId }"
          >
            <div class="video-head">
              <span v-if="v.id === plan.currentVideoId" class="badge-current">当前先看</span>
              <strong>{{ v.sortOrder }}. {{ v.title }}</strong>
              <span class="cyber-muted">{{ v.platform }} · {{ v.linkKind }}</span>
            </div>
            <p class="video-rationale">{{ v.rationale }}</p>
            <a
              v-if="v.url"
              class="cyber-link"
              :href="v.url"
              target="_blank"
              rel="noopener noreferrer"
            >打开链接</a>
            <p v-else class="cyber-muted">无直链（请按标题在平台内搜索）</p>
          </li>
        </ul>

        <h2 class="section-checkin">学后打卡 · 素材回流 ZERO NOTE</h2>
        <p class="cyber-muted">
          学完可打卡；补充内容会随首页「生成笔记」请求合并进 AI
          补充文本（字段 <code>guidanceCheckInId</code>）。仅打卡无补充时，请至少填写备注/链接/转写之一再生成；也可与 MP3、正文同时提交。
        </p>
        <label class="cyber-field-label">打卡备注（可选）</label>
        <textarea
          v-model="checkInRemark"
          class="cyber-field"
          rows="2"
          placeholder="例如：今天看到第 3 节，疑点在 …"
        />
        <button type="button" class="cyber-btn-primary" :disabled="checkInLoading" @click="onSubmitCheckIn">
          {{ checkInLoading ? "…" : "提交打卡" }}
        </button>
        <p v-if="lastCheckInId != null" class="cyber-muted">当前打卡 ID：<strong>{{ lastCheckInId }}</strong></p>
        <template v-if="lastCheckInId != null">
          <label class="cyber-field-label">视频链接（可选）</label>
          <input v-model="checkInVideoUrl" class="cyber-field" type="url" placeholder="https://..." />
          <label class="cyber-field-label">转写 / 摘录（可选）</label>
          <textarea v-model="checkInTranscript" class="cyber-field" rows="4" />
          <p class="checkin-actions">
            <button type="button" class="cyber-btn-ghost" :disabled="checkInLoading" @click="onSupplementCheckIn">
              更新补充素材
            </button>
            <button type="button" class="cyber-btn-ghost" @click="goNotesWithCheckIn">去首页生成笔记</button>
          </p>
        </template>
        <p v-if="checkInMessage" class="cyber-muted">{{ checkInMessage }}</p>

        <p v-if="errorMessage" class="cyber-error">{{ errorMessage }}</p>
      </section>

      <section v-else class="cyber-card">
        <p class="cyber-error">{{ errorMessage || "暂无数据" }}</p>
        <p class="cyber-muted">
          请前往 <RouterLink to="/guidance/profile">沉浸导学 · 问卷</RouterLink> 提交画像，再带
          <code>sessionId</code> 访问本页；或直接尝试拉取最新方案。
        </p>
        <button type="button" class="cyber-btn-primary" @click="loadPlan">重试加载最新</button>
      </section>
    </template>
  </main>
</template>

<style scoped>
.page-plan {
  margin: 0 auto;
  max-width: 900px;
  padding: 2rem 1rem 3rem;
}

.page-plan h1 {
  font-size: clamp(1.2rem, 4vw, 1.7rem);
  margin: 0 0 0.65rem;
}

.page-lead {
  margin: 0 0 1rem;
}

.page-lead code {
  font-size: 0.85em;
}

.plan-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.75rem;
}

.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.cyber-btn-ghost {
  background: transparent;
  border: 1px solid var(--border-strong, rgba(255, 255, 255, 0.2));
  color: inherit;
  padding: 0.35rem 0.65rem;
  border-radius: var(--corner-radius-inner, 4px);
  cursor: pointer;
  font-size: 0.85rem;
}

.inline-label {
  display: inline-block;
  margin-right: 0.5rem;
  margin-top: 0.5rem;
}

.narrow-select {
  max-width: 220px;
  margin-bottom: 1rem;
}

.outline-pre {
  white-space: pre-wrap;
  word-break: break-word;
}

.cyber-list {
  margin: 0.35rem 0 0;
  padding-left: 1.2rem;
}

.cyber-list.ordered {
  list-style: decimal;
}

.video-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.video-item {
  border: 1px solid var(--border-strong, rgba(255, 255, 255, 0.12));
  border-radius: var(--corner-radius-inner, 4px);
  padding: 0.75rem 1rem;
  margin-bottom: 0.65rem;
}

.video-item--current {
  border-color: rgba(0, 255, 200, 0.35);
  background: rgba(0, 255, 200, 0.06);
}

.badge-current {
  display: inline-block;
  font-size: 0.72rem;
  font-weight: 600;
  margin-right: 0.35rem;
  padding: 0.12rem 0.4rem;
  border-radius: 999px;
  background: rgba(0, 255, 200, 0.2);
}

.video-head {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem 0.75rem;
  align-items: baseline;
}

.video-rationale {
  margin: 0.4rem 0 0.35rem;
  font-size: 0.92rem;
}

.section-checkin {
  margin: 1.5rem 0 0.5rem;
  font-size: 1rem;
  font-weight: 600;
}

.checkin-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 0.5rem;
}
</style>
