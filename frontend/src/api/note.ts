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

export async function processAudio(file: File): Promise<NoteResult> {
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch("http://localhost:8080/api/notes/process", {
    method: "POST",
    body: formData
  });

  if (!response.ok) {
    const errorBody = await response.json().catch(() => ({ error: "Upload failed" }));
    throw new Error(errorBody.error ?? "Upload failed");
  }

  return (await response.json()) as NoteResult;
}
