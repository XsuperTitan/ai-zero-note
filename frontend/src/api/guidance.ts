import { apiFetch, API_ORIGIN } from "./client";
import type { ApiEnvelope } from "./user";

export type TutorPersona = "SILVER_WOLF" | "STREAM_VETERAN" | "NAMELESS_SELF";

export type LearningUrgency = "LOW" | "MEDIUM" | "HIGH";

export type ContentPreference = "VIDEO" | "ARTICLE" | "MIXED";

export interface LearningQuestionnaireSubmitRequest {
  tutorPersona: TutorPersona;
  subjectOrTopic: string;
  urgency: LearningUrgency;
  preferredPlatforms: string;
  usualSites: string;
  studyRhythm: string;
  contentPreference: ContentPreference;
  extraNotes?: string;
}

export interface GuidanceProfileResponse {
  sessionId: number;
  tutorPersona: string;
  status: string;
  reportSummary: string;
  llmPromptConstraints: string;
  createdAt: string;
  updatedAt: string;
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

export async function submitLearningProfile(
  payload: LearningQuestionnaireSubmitRequest
): Promise<GuidanceProfileResponse> {
  const response = await apiFetch(`${API_ORIGIN}/api/guidance/profile/submit`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  return parseEnvelope<GuidanceProfileResponse>(response);
}

export async function getLatestLearningProfile(): Promise<GuidanceProfileResponse> {
  const response = await apiFetch(`${API_ORIGIN}/api/guidance/profile/latest`);
  return parseEnvelope<GuidanceProfileResponse>(response);
}

export async function getLearningProfileBySessionId(sessionId: number): Promise<GuidanceProfileResponse> {
  const response = await apiFetch(`${API_ORIGIN}/api/guidance/profile/${sessionId}`);
  return parseEnvelope<GuidanceProfileResponse>(response);
}
