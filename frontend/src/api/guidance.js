import { apiFetch, API_ORIGIN } from "./client";
async function parseEnvelope(response) {
    const body = (await response.json());
    if (!response.ok) {
        const msg = typeof body === "object" &&
            body &&
            "error" in body &&
            typeof body.error === "string" &&
            body.error.trim().length > 0
            ? body.error
            : typeof body === "object" && body && "message" in body && typeof body.message === "string"
                ? body.message
                : `HTTP ${response.status}`;
        throw new Error(msg);
    }
    if ("code" in body && body !== null && typeof body === "object") {
        const typed = body;
        if (typed.code !== 0) {
            throw new Error(typed.message?.trim() ? typed.message : "Request failed");
        }
        return typed.data;
    }
    throw new Error("Invalid response");
}
export async function submitLearningProfile(payload) {
    const response = await apiFetch(`${API_ORIGIN}/api/guidance/profile/submit`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
    });
    return parseEnvelope(response);
}
export async function getLatestLearningProfile() {
    const response = await apiFetch(`${API_ORIGIN}/api/guidance/profile/latest`);
    return parseEnvelope(response);
}
export async function getLearningProfileBySessionId(sessionId) {
    const response = await apiFetch(`${API_ORIGIN}/api/guidance/profile/${sessionId}`);
    return parseEnvelope(response);
}
