<script setup lang="ts">
import { ref } from "vue";
import { API_ORIGIN } from "../api/client";
import { processMixedInput, type NoteResult } from "../api/note";
import VideoLinkSection from "../components/VideoLinkSection.vue";
import { insertAtCaret } from "../utils/insertAtCaret";

const selectedAudioFile = ref<File | null>(null);
const selectedTextFile = ref<File | null>(null);
const textContent = ref("");
const loading = ref(false);
const errorMessage = ref("");
const result = ref<NoteResult | null>(null);
const textAreaRef = ref<HTMLTextAreaElement | null>(null);

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
      textContent: textContent.value
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
    <p class="cyber-muted page-lead">Upload MP3 and/or text content to generate final markdown notes.</p>

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
      <p><strong>Source:</strong> {{ result.sourceFilename }}</p>
      <p>{{ result.abstractText }}</p>
      <a class="cyber-link cyber-download" :href="`${API_ORIGIN}${result.downloadUrl}`" target="_blank" rel="noopener">
        Download Markdown
      </a>
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
</style>
