<script setup lang="ts">
import { computed, onUnmounted, ref } from "vue";
import { API_ORIGIN } from "../api/client";
import { enqueueImageNoteJob, getImageNoteJob } from "../api/imageNote";
import { processMixedInput, type NoteResult, type NoteStyle, type OutputLanguage } from "../api/note";
import MindMapMermaid from "../components/MindMapMermaid.vue";
import VideoLinkSection from "../components/VideoLinkSection.vue";
import { insertAtCaret } from "../utils/insertAtCaret";

const selectedAudioFile = ref<File | null>(null);
const selectedTextFile = ref<File | null>(null);
const textContent = ref("");
const noteStyle = ref<NoteStyle>("LEARNING");
const outputLanguage = ref<OutputLanguage>("AUTO");
const loading = ref(false);
const errorMessage = ref("");
const result = ref<NoteResult | null>(null);
const textAreaRef = ref<HTMLTextAreaElement | null>(null);

const showLanguageSelect = computed(() => noteStyle.value !== "LEARNING");

let imagePollHandle: ReturnType<typeof setInterval> | null = null;
const imageJobLoading = ref(false);
const imageJobError = ref("");
const imageJobStatus = ref<string | null>(null);
const imageJobId = ref<string | null>(null);
const imageDownloadUrl = ref<string | null>(null);

const showMindMapPanel = computed(
  () => Boolean(result.value?.mindMapJson && result.value!.mindMapJson!.trim().length > 0)
);

function composedSourceForImage(): string {
  if (textContent.value.trim()) {
    return textContent.value.trim();
  }
  if (result.value?.markdownPreview?.trim()) {
    return result.value.markdownPreview.trim();
  }
  return "";
}

function resetImagePolling() {
  if (imagePollHandle) {
    clearInterval(imagePollHandle);
    imagePollHandle = null;
  }
}

async function pollImageOnce(jobId: string) {
  try {
    const st = await getImageNoteJob(jobId);
    imageJobStatus.value = st.status;
    if (st.status === "SUCCEEDED") {
      resetImagePolling();
      imageDownloadUrl.value = st.downloadUrl ? `${API_ORIGIN}${st.downloadUrl}` : "";
      imageJobLoading.value = false;
    } else if (st.status === "FAILED") {
      resetImagePolling();
      imageJobError.value = st.errorMessage || "Image generation failed.";
      imageJobLoading.value = false;
    }
  } catch (e) {
    resetImagePolling();
    imageJobError.value = e instanceof Error ? e.message : "Image job polling failed";
    imageJobLoading.value = false;
  }
}

async function onStartImageNote() {
  const src = composedSourceForImage();
  if (!src) {
    imageJobError.value = "Add text content or generate notes before requesting a picture note.";
    return;
  }
  if (imageJobLoading.value) {
    return;
  }
  resetImagePolling();
  imageJobError.value = "";
  imageJobLoading.value = true;
  imageJobStatus.value = null;
  imageJobId.value = null;
  imageDownloadUrl.value = null;
  try {
    const queued = await enqueueImageNoteJob(src);
    imageJobId.value = queued.jobId;
    imageJobStatus.value = queued.status;
    imagePollHandle = setInterval(() => {
      void pollImageOnce(queued.jobId);
    }, 2200);
    void pollImageOnce(queued.jobId);
  } catch (e) {
    imageJobError.value = e instanceof Error ? e.message : "Failed to enqueue image job";
    imageJobLoading.value = false;
  }
}

onUnmounted(() => resetImagePolling());

function onSelectAudioFile(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0] ?? null;
  errorMessage.value = "";
  result.value = null;

  if (!file) {
    selectedAudioFile.value = null;
    return;
  }

  if (!file.name.toLowerCase().endsWith(".mp3")) {
    errorMessage.value = "Only MP3 files are supported in this MVP.";
    selectedAudioFile.value = null;
    return;
  }

  const maxSize = 25 * 1024 * 1024;
  if (file.size > maxSize) {
    errorMessage.value = "File exceeds 25MB size limit.";
    selectedAudioFile.value = null;
    return;
  }

  selectedAudioFile.value = file;
}

function onSelectTextFile(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0] ?? null;
  errorMessage.value = "";
  result.value = null;

  if (!file) {
    selectedTextFile.value = null;
    return;
  }

  const lower = file.name.toLowerCase();
  if (!lower.endsWith(".txt") && !lower.endsWith(".md")) {
    errorMessage.value = "Text file must be .txt or .md.";
    selectedTextFile.value = null;
    return;
  }

  const maxSize = 25 * 1024 * 1024;
  if (file.size > maxSize) {
    errorMessage.value = "Text file exceeds 25MB size limit.";
    selectedTextFile.value = null;
    return;
  }

  selectedTextFile.value = file;
}

async function onSubmit() {
  const hasTextContent = textContent.value.trim().length > 0;
  if (loading.value) {
    return;
  }
  if (!selectedAudioFile.value && !selectedTextFile.value && !hasTextContent) {
    errorMessage.value = "Please provide audio file, text file, or text content.";
    return;
  }

  loading.value = true;
  errorMessage.value = "";
  result.value = null;

  try {
    result.value = await processMixedInput({
      audioFile: selectedAudioFile.value,
      textFile: selectedTextFile.value,
      textContent: textContent.value,
      noteStyle: noteStyle.value,
      outputLanguage: outputLanguage.value
    });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "Processing failed";
  } finally {
    loading.value = false;
  }
}

function insertIntoTextContent(rawText: string) {
  const normalized = rawText.trim();
  if (!normalized) {
    return;
  }
  const textArea = textAreaRef.value;
  const insertion = textContent.value.trim().length > 0 ? `\n\n${normalized}` : normalized;
  if (!textArea) {
    textContent.value = `${textContent.value}${insertion}`;
    return;
  }
  const { nextValue, nextCaretPosition } = insertAtCaret(textArea, textContent.value, insertion);
  textContent.value = nextValue;
  requestAnimationFrame(() => {
    textArea.focus();
    textArea.setSelectionRange(nextCaretPosition, nextCaretPosition);
  });
}

function onInsertVideoMarkdown(markdown: string) {
  insertIntoTextContent(markdown);
}

function onInsertVideoTextContent(text: string) {
  insertIntoTextContent(text);
}
</script>

<template>
  <main class="page-home">
    <h1 class="cyber-display cyber-title-glitch">AI Zero Notes</h1>
    <p class="cyber-muted page-lead">
      Upload MP3 and/or text content to generate Markdown notes — pick a note style before generating.
    </p>

    <section class="cyber-card">
      <label class="cyber-field-label">Audio (optional, .mp3)</label>
      <input class="cyber-field" type="file" accept=".mp3,audio/mpeg" @change="onSelectAudioFile" />

      <label class="cyber-field-label">Text file (optional, .txt/.md)</label>
      <input class="cyber-field" type="file" accept=".txt,.md,text/plain,text/markdown" @change="onSelectTextFile" />

      <label class="cyber-field-label">Text content (optional)</label>
      <textarea
        ref="textAreaRef"
        v-model="textContent"
        class="cyber-field"
        rows="6"
        placeholder="Paste transcript, outline, or learning notes here..."
      />

      <label class="cyber-field-label">Note style</label>
      <select v-model="noteStyle" class="cyber-field">
        <option value="LEARNING">Learning checklist (default, exam-prep oriented)</option>
        <option value="DETAILED">Detailed notes (multi-language prompts)</option>
        <option value="MIND_MAP">Keyword mind map</option>
      </select>

      <template v-if="showLanguageSelect">
        <label class="cyber-field-label">Output language</label>
        <select v-model="outputLanguage" class="cyber-field">
          <option value="AUTO">Auto (DETAILED/MIND_MAP default: bilingual)</option>
          <option value="ZH">简体中文</option>
          <option value="EN">English</option>
          <option value="BILINGUAL">中英对照</option>
        </select>
      </template>

      <VideoLinkSection
        @insert-markdown="onInsertVideoMarkdown"
        @insert-text-content="onInsertVideoTextContent"
      />

      <button type="button" class="cyber-btn-primary" :disabled="loading" @click="onSubmit">
        {{ loading ? "Processing..." : "Generate Notes" }}
      </button>
      <p v-if="errorMessage" class="cyber-error">{{ errorMessage }}</p>
    </section>

    <section v-if="result" class="cyber-card">
      <h2>{{ result.title }}</h2>
      <template v-if="showMindMapPanel">
        <h3 class="section-mini-title">Mind map preview</h3>
        <MindMapMermaid :mind-map-json="result.mindMapJson!" />
      </template>
      <p v-if="result.noteStyle"><strong>Note style:</strong> {{ result.noteStyle }}</p>
      <p v-if="result.outputLanguage && result.noteStyle !== 'LEARNING'">
        <strong>Language:</strong> {{ result.outputLanguage }}
      </p>
      <p><strong>Source:</strong> {{ result.sourceFilename }}</p>
      <p>{{ result.abstractText }}</p>
      <a class="cyber-link cyber-download" :href="`${API_ORIGIN}${result.downloadUrl}`" target="_blank" rel="noopener">
        Download Markdown
      </a>

      <h3 class="section-mini-title">Picture note (async)</h3>
      <p class="cyber-muted">
        Builds a sketch-note style PNG from your text buffer or preview. Backend: enable with
        <code>IMAGE_NOTES_ENABLED=true</code>. Either OpenAI <code>/images/generations</code> (<code>IMAGE_NOTES_PROVIDER=openai</code>), or Alibaba
        万相同步 DashScope (<code>IMAGE_NOTES_PROVIDER=wanx</code>). API key picks up Vision key if unset (<code>VISION_API_KEY</code> fallback in YAML).
      </p>
      <button type="button" class="cyber-btn-primary" :disabled="imageJobLoading" @click="onStartImageNote">
        {{
          imageJobLoading
            ? imageJobStatus
              ? `Image job: ${imageJobStatus}`
              : "Enqueueing..."
            : "Generate picture note"
        }}
      </button>
      <p v-if="imageJobId" class="cyber-muted">Job id: {{ imageJobId }}</p>
      <p v-if="imageJobError" class="cyber-error">{{ imageJobError }}</p>
      <a
        v-if="imageDownloadUrl"
        class="cyber-link cyber-download"
        :href="imageDownloadUrl"
        target="_blank"
        rel="noopener"
      >
        Download PNG
      </a>
      <img v-if="imageDownloadUrl" class="image-note-preview" :src="imageDownloadUrl" alt="Generated picture note" />

      <pre class="cyber-pre">{{ result.markdownPreview }}</pre>
    </section>
  </main>
</template>

<style scoped>
.page-home {
  margin: 0 auto;
  max-width: 900px;
  padding: 2rem 1rem 3rem;
}

.page-home h1 {
  font-size: clamp(1.35rem, 4vw, 1.85rem);
  margin: 0 0 0.65rem;
}

.page-lead {
  margin: 0 0 0.25rem;
}

.page-home .cyber-card :deep(h2) {
  font-size: 1.05rem;
}

.page-home .cyber-card p {
  margin: 0.45rem 0;
}

.section-mini-title {
  margin: 1rem 0 0.35rem;
  font-size: 0.92rem;
  font-weight: 600;
}

.image-note-preview {
  display: block;
  margin-top: 0.65rem;
  max-width: 100%;
  border-radius: var(--corner-radius-inner, 4px);
  border: 1px solid var(--border-strong, rgba(255, 255, 255, 0.12));
}
</style>
