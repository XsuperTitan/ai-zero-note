import { apiFetch, API_ORIGIN } from "./client";
export async function enqueueImageNoteJob(sourceText) {
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
    return body;
}
export async function getImageNoteJob(jobId) {
    const response = await apiFetch(`${API_ORIGIN}/api/notes/image-note-jobs/${encodeURIComponent(jobId)}`, {
        method: "GET"
    });
    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
        throw new Error(typeof body.error === "string" ? body.error : "Failed to load image job status");
    }
    return body;
}
