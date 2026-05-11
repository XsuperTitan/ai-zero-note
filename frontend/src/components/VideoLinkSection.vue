<script setup lang="ts">
import { computed, ref } from "vue";
import {
  fetchVideoFrames,
  fetchVideoText,
  fetchVideoVisionText,
  resolveVideoAssetUrl,
  type VideoFrameItem,
  type VideoMetaResult
} from "../api/video";

const emit = defineEmits<{
  (event: "insert-markdown", value: string): void;
  (event: "insert-text-content", value: string): void;
}>();

const videoUrl = ref("");
const loadingMeta = ref(false);
const loadingFrames = ref(false);
const loadingVision = ref(false);
const errorMessage = ref("");
const videoMeta = ref<VideoMetaResult | null>(null);
const frames = ref<VideoFrameItem[]>([]);
const selectedFrameNames = ref<Set<string>>(new Set());
const currentTaskId = ref("");
const currentFrameUrl = ref("");
const visionTargetLanguage = ref<"auto" | "zh" | "en">("auto");

const selectedFrames = computed(() =>
  frames.value.filter((frame) => selectedFrameNames.value.has(frame.fileName))
);

function clearError() {
  errorMessage.value = "";
}

function validateUrl(): string {
  const value = videoUrl.value.trim();
  if (!value) {
    throw new Error("请先输入视频链接。");
  }
  if (!value.startsWith("http://") && !value.startsWith("https://")) {
    throw new Error("视频链接必须以 http:// 或 https:// 开头。");
  }
  return value;
}

async function onParseMeta() {
  if (loadingMeta.value || loadingFrames.value || loadingVision.value) {
    return;
  }
  clearError();
  try {
    const url = validateUrl();
    loadingMeta.value = true;
    const result = await fetchVideoText(url);
    videoMeta.value = result.meta;
    emit("insert-text-content", result.textContent);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "视频文本提取失败";
  } finally {
    loadingMeta.value = false;
  }
}

async function onGenerateFrames() {
  if (loadingFrames.value || loadingMeta.value || loadingVision.value) {
    return;
  }
  clearError();
  try {
    const url = validateUrl();
    loadingFrames.value = true;
    const result = await fetchVideoFrames({ url });
    videoMeta.value = result.meta;
    frames.value = result.frames;
    currentTaskId.value = result.taskId;
    currentFrameUrl.value = url;
    selectedFrameNames.value = new Set(result.frames.map((frame) => frame.fileName));
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "关键截图生成失败";
  } finally {
    loadingFrames.value = false;
  }
}

async function onGenerateVisionText() {
  if (loadingFrames.value || loadingMeta.value || loadingVision.value) {
    return;
  }
  if (selectedFrames.value.length === 0) {
    errorMessage.value = "请至少选择一张截图。";
    return;
  }
  if (!currentTaskId.value) {
    errorMessage.value = "请先生成关键截图。";
    return;
  }
  clearError();
  try {
    loadingVision.value = true;
    const result = await fetchVideoVisionText(
      currentFrameUrl.value,
      currentTaskId.value,
      selectedFrames.value.map((frame) => frame.fileName),
      visionTargetLanguage.value
    );
    emit("insert-text-content", result.textContent);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "图生文提取失败";
  } finally {
    loadingVision.value = false;
  }
}

function toggleFrame(fileName: string) {
  const nextSet = new Set(selectedFrameNames.value);
  if (nextSet.has(fileName)) {
    nextSet.delete(fileName);
  } else {
    nextSet.add(fileName);
  }
  selectedFrameNames.value = nextSet;
}

function selectAllFrames() {
  selectedFrameNames.value = new Set(frames.value.map((frame) => frame.fileName));
}

function clearSelectedFrames() {
  selectedFrameNames.value = new Set();
}

function buildMarkdown(meta: VideoMetaResult, chosenFrames: VideoFrameItem[]): string {
  const authorText = meta.uploader ? ` | 🎬 ${meta.uploader}` : "";
  const lines = [
    `> **视频标题**：[${meta.title}](${meta.sourceUrl}) | ⏱ 时长：${meta.durationText}${authorText}`
  ];
  chosenFrames.forEach((frame, index) => {
    lines.push(`![截图${index + 1}](${resolveVideoAssetUrl(frame.imageUrl)})`);
  });
  return `${lines.join("\n")}\n`;
}

function onInsertMarkdown() {
  if (!videoMeta.value) {
    errorMessage.value = "请先完成视频解析。";
    return;
  }
  if (selectedFrames.value.length === 0) {
    errorMessage.value = "请至少选择一张截图。";
    return;
  }
  clearError();
  emit("insert-markdown", buildMarkdown(videoMeta.value, selectedFrames.value));
}
</script>

<template>
  <section class="video-card">
    <h3>视频链接解析与截图</h3>

    <label class="field-label">视频链接</label>
    <input
      v-model.trim="videoUrl"
      type="url"
      placeholder="https://www.bilibili.com/... 或 https://www.youtube.com/..."
    />

    <div class="actions">
      <button type="button" :disabled="loadingMeta || loadingFrames || loadingVision" @click="onParseMeta">
        {{ loadingMeta ? "提取中..." : "解析视频信息并注入Text content" }}
      </button>
      <button type="button" :disabled="loadingMeta || loadingFrames || loadingVision" @click="onGenerateFrames">
        {{ loadingFrames ? "生成中..." : "生成关键截图" }}
      </button>
    </div>

    <div v-if="videoMeta" class="meta">
      <p><strong>标题：</strong>{{ videoMeta.title }}</p>
      <p><strong>时长：</strong>{{ videoMeta.durationText }}</p>
      <p v-if="videoMeta.uploader"><strong>作者：</strong>{{ videoMeta.uploader }}</p>
    </div>

    <div v-if="frames.length > 0" class="grid-toolbar">
      <button type="button" @click="selectAllFrames">全选</button>
      <button type="button" @click="clearSelectedFrames">清空</button>
      <label class="language-label">
        输出语言
        <select v-model="visionTargetLanguage">
          <option value="auto">自动</option>
          <option value="zh">中文</option>
          <option value="en">English</option>
        </select>
      </label>
      <span>已选 {{ selectedFrames.length }} / {{ frames.length }}</span>
    </div>

    <div v-if="frames.length > 0" class="frame-grid">
      <label
        v-for="frame in frames"
        :key="frame.fileName"
        class="frame-item"
        :class="{ selected: selectedFrameNames.has(frame.fileName) }"
      >
        <input
          type="checkbox"
          :checked="selectedFrameNames.has(frame.fileName)"
          @change="toggleFrame(frame.fileName)"
        />
        <img :src="resolveVideoAssetUrl(frame.imageUrl)" :alt="frame.fileName" loading="lazy" />
      </label>
    </div>

    <div v-if="frames.length > 0" class="insert-actions">
      <button
        type="button"
        class="insert-btn"
        :disabled="selectedFrames.length === 0 || loadingVision"
        @click="onInsertMarkdown"
      >
        插入选中截图 Markdown
      </button>
      <button
        type="button"
        class="insert-btn"
        :disabled="selectedFrames.length === 0 || loadingVision"
        @click="onGenerateVisionText"
      >
        {{ loadingVision ? "图生文提取中..." : "图生文提取并注入Text content" }}
      </button>
    </div>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
  </section>
</template>

<style scoped>
.video-card {
  border-top: 1px solid #eee;
  margin-top: 1rem;
  padding-top: 1rem;
}

.field-label {
  display: block;
  font-weight: 600;
  margin-bottom: 0.3rem;
}

input[type="url"] {
  box-sizing: border-box;
  padding: 0.5rem;
  width: 100%;
}

.actions {
  display: flex;
  gap: 0.6rem;
  margin-top: 0.8rem;
}

.meta {
  margin-top: 0.8rem;
}

.meta p {
  margin: 0.2rem 0;
}

.grid-toolbar {
  align-items: center;
  display: flex;
  gap: 0.6rem;
  margin: 0.8rem 0;
}

.language-label {
  align-items: center;
  display: inline-flex;
  gap: 0.3rem;
}

.frame-grid {
  display: grid;
  gap: 0.6rem;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
}

.frame-item {
  border: 1px solid #ddd;
  border-radius: 8px;
  cursor: pointer;
  overflow: hidden;
  padding: 0.3rem;
}

.frame-item.selected {
  border-color: #2f81f7;
}

.frame-item input {
  margin-bottom: 0.3rem;
}

.frame-item img {
  display: block;
  height: 100px;
  object-fit: cover;
  width: 100%;
}

.insert-btn {
  margin-top: 0;
}

.insert-actions {
  display: flex;
  gap: 0.6rem;
  margin-top: 0.8rem;
}

.error {
  color: #c00;
  margin-top: 0.8rem;
}
</style>
