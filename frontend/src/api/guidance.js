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
export async function generateStudyPlan(sessionId, mode = "TEMPLATE") {
    const response = await apiFetch(`${API_ORIGIN}/api/guidance/plan/generate`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ sessionId, mode })
    });
    return parseEnvelope(response);
}
export async function getLatestStudyPlan() {
    const response = await apiFetch(`${API_ORIGIN}/api/guidance/plan/latest`);
    return parseEnvelope(response);
}
export async function getStudyPlanBySessionId(sessionId) {
    const response = await apiFetch(`${API_ORIGIN}/api/guidance/plan/session/${sessionId}`);
    return parseEnvelope(response);
}
export async function getActiveGuidanceProgress() {
    const response = await apiFetch(`${API_ORIGIN}/api/guidance/progress/active`);
    return parseEnvelope(response);
}
export async function enterGuidanceSessionInProgress(sessionId) {
    const response = await apiFetch(`${API_ORIGIN}/api/guidance/session/${sessionId}/enter-in-progress`, {
        method: "POST"
    });
    return parseEnvelope(response);
}
export async function updateGuidanceCurrentVideo(sessionId, currentVideoId) {
    const response = await apiFetch(`${API_ORIGIN}/api/guidance/session/${sessionId}/current-video`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ currentVideoId })
    });
    return parseEnvelope(response);
}
export async function completeGuidanceSession(sessionId) {
    const response = await apiFetch(`${API_ORIGIN}/api/guidance/session/${sessionId}/complete`, {
        method: "POST"
    });
    return parseEnvelope(response);
}
export async function createGuidanceCheckIn(sessionId, remark) {
    const body = { sessionId };
    if (remark != null && remark.trim().length > 0) {
        body.remark = remark.trim();
    }
    const response = await apiFetch(`${API_ORIGIN}/api/guidance/check-in`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
    });
    return parseEnvelope(response);
}
export async function supplementGuidanceCheckIn(checkInId, payload) {
    const body = {};
    if (payload.videoUrl != null && payload.videoUrl.trim().length > 0) {
        body.videoUrl = payload.videoUrl.trim();
    }
    if (payload.transcriptText != null && payload.transcriptText.trim().length > 0) {
        body.transcriptText = payload.transcriptText.trim();
    }
    const response = await apiFetch(`${API_ORIGIN}/api/guidance/check-in/${checkInId}/supplement`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
    });
    return parseEnvelope(response);
}
