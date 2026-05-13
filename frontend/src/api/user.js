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
export async function register(userAccount, userPassword, checkPassword, inviteCode) {
    const response = await apiFetch(`${API_ORIGIN}/user/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ userAccount, userPassword, checkPassword, inviteCode })
    });
    return parseEnvelope(response);
}
export async function login(userAccount, userPassword) {
    const response = await apiFetch(`${API_ORIGIN}/user/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ userAccount, userPassword })
    });
    return parseEnvelope(response);
}
export async function logout() {
    const response = await apiFetch(`${API_ORIGIN}/user/logout`, { method: "POST" });
    return parseEnvelope(response);
}
export async function getLoginUser() {
    const response = await apiFetch(`${API_ORIGIN}/user/get/login`);
    if (response.status === 401) {
        return null;
    }
    if (!response.ok) {
        const payload = await response.json().catch(() => ({}));
        const msg = typeof payload === "object" && payload && "error" in payload && typeof payload.error === "string" ? payload.error : "";
        throw new Error(msg || `HTTP ${response.status}`);
    }
    const body = (await response.json());
    if (body.code !== 0) {
        return null;
    }
    return body.data;
}
