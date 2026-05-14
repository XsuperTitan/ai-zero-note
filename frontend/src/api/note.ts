import { apiFetch, API_ORIGIN } from "./client";

export type NoteStyle = "LEARNING" | "DETAILED" | "MIND_MAP";

export type OutputLanguage = "ZH" | "EN" | "BILINGUAL" | "AUTO";

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
  noteStyle?: NoteStyle;
  outputLanguage?: OutputLanguage;
  mindMapJson?: string | null;
}

interface MixedInput {
  audioFile?: File | null;
  textFile?: File | null;
  textContent?: string;
  noteStyle?: NoteStyle;
  outputLanguage?: OutputLanguage;
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
  formData.append("noteStyle", input.noteStyle ?? "LEARNING");
  formData.append("outputLanguage", input.outputLanguage ?? "AUTO");

  const response = await apiFetch(`${API_ORIGIN}/api/notes/process-mixed`, {
    method: "POST",
    body: formData
  });

  const body = await response.json().catch(() => ({}));

  if (!response.ok) {
    throw new Error(typeof body.error === "string" ? body.error : "Upload failed");
  }

  return body as NoteResult;
}
