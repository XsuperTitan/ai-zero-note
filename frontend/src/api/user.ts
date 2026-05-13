import { apiFetch, API_ORIGIN } from "./client";

export interface ApiEnvelope<T> {
  code: number;
  data: T;
  message: string;
}

export interface LoginUserVO {
  id: number;
  userAccount: string;
  userName: string;
  userRole: string;
  permissions?: string | null;
}

async function parseEnvelope<T>(response: Response): Promise<T> {
  const body = (await response.json()) as
    | ApiEnvelope<T>
    | { error?: string; code?: number; message?: string };
  if (!response.ok) {
    const msg =
      typeof body === "object" &&
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
    const typed = body as ApiEnvelope<T>;
    if (typed.code !== 0) {
      throw new Error(typed.message?.trim() ? typed.message : "Request failed");
    }
    return typed.data;
  }
  throw new Error("Invalid response");
}

export async function register(
  userAccount: string,
  userPassword: string,
  checkPassword: string,
  inviteCode: string
): Promise<number> {
  const response = await apiFetch(`${API_ORIGIN}/user/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userAccount, userPassword, checkPassword, inviteCode })
  });
  return parseEnvelope<number>(response);
}

export async function login(userAccount: string, userPassword: string): Promise<LoginUserVO> {
  const response = await apiFetch(`${API_ORIGIN}/user/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userAccount, userPassword })
  });
  return parseEnvelope<LoginUserVO>(response);
}

export async function logout(): Promise<boolean> {
  const response = await apiFetch(`${API_ORIGIN}/user/logout`, { method: "POST" });
  return parseEnvelope<boolean>(response);
}

export async function getLoginUser(): Promise<LoginUserVO | null> {
  const response = await apiFetch(`${API_ORIGIN}/user/get/login`);
  if (response.status === 401) {
    return null;
  }
  if (!response.ok) {
    const payload = await response.json().catch(() => ({}));
    const msg = typeof payload === "object" && payload && "error" in payload && typeof payload.error === "string" ? payload.error : "";
    throw new Error(msg || `HTTP ${response.status}`);
  }
  const body = (await response.json()) as ApiEnvelope<LoginUserVO>;
  if (body.code !== 0) {
    return null;
  }
  return body.data;
}
