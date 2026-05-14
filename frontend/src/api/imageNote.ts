import { apiFetch, API_ORIGIN } from "./client";

export interface ImageNoteEnqueueResponse {
  jobId: string;
  status: string;
}

export interface ImageNoteStatusResponse {
  jobId: string;
  status: string;
  errorMessage: string;
  downloadUrl: string;
}

export async function enqueueImageNoteJob(sourceText: string): Promise<ImageNoteEnqueueResponse> {
  const response = await apiFetch(`${API_ORIGIN}/api/notes/image-note-jobs`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ sourceText })
  });

  const body = await response.json().catch(() => ({}));

  if (!response.ok) {
    throw new Error(typeof body.error === "string" ? body.error : "Failed to enqueue image note job");
  }

  return body as ImageNoteEnqueueResponse;
}

export async function getImageNoteJob(jobId: string): Promise<ImageNoteStatusResponse> {
  const response = await apiFetch(`${API_ORIGIN}/api/notes/image-note-jobs/${encodeURIComponent(jobId)}`, {
    method: "GET"
  });

  const body = await response.json().catch(() => ({}));

  if (!response.ok) {
    throw new Error(typeof body.error === "string" ? body.error : "Failed to load image job status");
  }

  return body as ImageNoteStatusResponse;
}
