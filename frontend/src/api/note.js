import { apiFetch, API_ORIGIN } from "./client";
export async function processMixedInput(input) {
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
    return (await response.json());
}
