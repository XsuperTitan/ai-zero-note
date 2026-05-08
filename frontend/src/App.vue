<script setup lang="ts">
import { ref } from "vue";
import { processAudio, type NoteResult } from "./api/note";

const selectedFile = ref<File | null>(null);
const loading = ref(false);
const errorMessage = ref("");
const result = ref<NoteResult | null>(null);

function onSelectFile(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0] ?? null;
  errorMessage.value = "";
  result.value = null;

  if (!file) {
    selectedFile.value = null;
    return;
  }

  if (!file.name.toLowerCase().endsWith(".mp3")) {
    errorMessage.value = "Only MP3 files are supported in this MVP.";
    selectedFile.value = null;
    return;
  }

  const maxSize = 25 * 1024 * 1024;
  if (file.size > maxSize) {
    errorMessage.value = "File exceeds 25MB size limit.";
    selectedFile.value = null;
    return;
  }

  selectedFile.value = file;
}

async function onSubmit() {
  if (!selectedFile.value || loading.value) {
    return;
  }

  loading.value = true;
  errorMessage.value = "";
  result.value = null;

  try {
    result.value = await processAudio(selectedFile.value);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "Processing failed";
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <main class="container">
    <h1>AI Zero Notes</h1>
    <p>Upload an MP3 file and generate structured markdown notes.</p>

    <section class="card">
      <input type="file" accept=".mp3,audio/mpeg" @change="onSelectFile" />
      <button :disabled="!selectedFile || loading" @click="onSubmit">
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
  margin-left: 0.8rem;
  padding: 0.5rem 0.8rem;
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
