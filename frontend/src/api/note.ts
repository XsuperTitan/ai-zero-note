import { apiFetch, API_ORIGIN } from "./client";

export interface NoteResult {
  noteId: string;
  sourceFilename: string;
  transcription: string;
  title: string;
  abstractText: string;
  keyPoints: string[];
  codeSnippets: string[];
  todos: string[];
  markdownPreview: string;
  downloadUrl: string;
}

interface MixedInput {
  audioFile?: File | null;
  textFile?: File | null;
  textContent?: string;
}

export async function processMixedInput(input: MixedInput): Promise<NoteResult> {
  const formData = new FormData();
  if (input.audioFile) {
    formData.append("file", input.audioFile);
  }
  if (input.textFile) {
    formData.append("textFile", input.textFile);
  }
  if (input.textContent && input.textContent.trim().length > 0) {
    formData.append("textContent", input.textContent.trim());
  }

  const response = await apiFetch(`${API_ORIGIN}/api/notes/process-mixed`, {
    method: "POST",
    body: formData
  });

  if (!response.ok) {
    const errorBody = await response.json().catch(() => ({ error: "Upload failed" }));
    throw new Error(errorBody.error ?? "Upload failed");
  }

  return (await response.json()) as NoteResult;
}
