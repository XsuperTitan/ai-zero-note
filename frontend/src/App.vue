<script setup lang="ts">
import { ref } from "vue";
import { processMixedInput, type NoteResult } from "./api/note";
import VideoLinkSection from "./components/VideoLinkSection.vue";
import { insertAtCaret } from "./utils/insertAtCaret";

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
  <main class="container">
    <h1>AI Zero Notes</h1>
    <p>Upload MP3 and/or text content to generate final markdown notes.</p>

    <section class="card">
      <label class="field-label">Audio (optional, .mp3)</label>
      <input type="file" accept=".mp3,audio/mpeg" @change="onSelectAudioFile" />

      <label class="field-label">Text file (optional, .txt/.md)</label>
      <input type="file" accept=".txt,.md,text/plain,text/markdown" @change="onSelectTextFile" />

      <label class="field-label">Text content (optional)</label>
      <textarea
        ref="textAreaRef"
        v-model="textContent"
        rows="6"
        placeholder="Paste transcript, outline, or learning notes here..."
      />

      <VideoLinkSection
        @insert-markdown="onInsertVideoMarkdown"
        @insert-text-content="onInsertVideoTextContent"
      />

      <button :disabled="loading" @click="onSubmit">
        {{ loading ? "Processing..." : "Generate Notes" }}
      </button>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    </section>

    <section v-if="result" class="card">
      <h2>{{ result.title }}</h2>
      <p><strong>Source:</strong> {{ result.sourceFilename }}</p>
      <p>{{ result.abstractText }}</p>
      <a :href="`http://localhost:8080${result.downloadUrl}`" target="_blank">Download Markdown</a>
      <pre>{{ result.markdownPreview }}</pre>
    </section>
  </main>
</template>

<style scoped>
.container {
  margin: 0 auto;
  max-width: 900px;
  padding: 2rem 1rem;
  font-family: Arial, sans-serif;
}

.card {
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 12px;
  margin-top: 1rem;
  padding: 1rem;
}

button {
  margin-top: 0.8rem;
  padding: 0.5rem 0.8rem;
}

.field-label {
  display: block;
  font-weight: 600;
  margin-bottom: 0.3rem;
  margin-top: 0.8rem;
}

textarea {
  box-sizing: border-box;
  font-family: inherit;
  padding: 0.6rem;
  width: 100%;
}

.error {
  color: #c00;
}

pre {
  background: #0d1117;
  color: #c9d1d9;
  margin-top: 1rem;
  overflow-x: auto;
  padding: 1rem;
  white-space: pre-wrap;
}
</style>
