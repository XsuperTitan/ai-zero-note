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

export type PlanGenerationMode = "TEMPLATE" | "LLM";

export type VideoLinkKind = string;

export interface StudyPlanVideoDto {
  id: string;
  title: string;
  platform: string;
  url: string;
  rationale: string;
  sortOrder: number;
  linkKind: VideoLinkKind;
}

export interface StudyPlanResponse {
  sessionId: number;
  generationSource: string;
  outlineMarkdown: string;
  suggestions: string[];
  priorities: string[];
  videos: StudyPlanVideoDto[];
  /** Id of the video the learner should watch first (`current_video_id` equiv). */
  currentVideoId: string;
  createdAt: string;
  updatedAt: string;
  sessionStatus: string;
}

export async function generateStudyPlan(
  sessionId: number,
  mode: PlanGenerationMode = "TEMPLATE"
): Promise<StudyPlanResponse> {
  const response = await apiFetch(`${API_ORIGIN}/api/guidance/plan/generate`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ sessionId, mode })
  });
  return parseEnvelope<StudyPlanResponse>(response);
}

export async function getLatestStudyPlan(): Promise<StudyPlanResponse> {
  const response = await apiFetch(`${API_ORIGIN}/api/guidance/plan/latest`);
  return parseEnvelope<StudyPlanResponse>(response);
}

export async function getStudyPlanBySessionId(sessionId: number): Promise<StudyPlanResponse> {
  const response = await apiFetch(`${API_ORIGIN}/api/guidance/plan/session/${sessionId}`);
  return parseEnvelope<StudyPlanResponse>(response);
}

export interface GuidanceActiveProgressResponse {
  sessionId: number;
  status: string;
  tutorPersona: string;
  subjectOrTopic: string;
  currentVideoId: string;
  currentVideoTitle: string;
  studyPlanPath: string;
}

export async function getActiveGuidanceProgress(): Promise<GuidanceActiveProgressResponse | null> {
  const response = await apiFetch(`${API_ORIGIN}/api/guidance/progress/active`);
  return parseEnvelope<GuidanceActiveProgressResponse | null>(response);
}

export async function enterGuidanceSessionInProgress(sessionId: number): Promise<boolean> {
  const response = await apiFetch(`${API_ORIGIN}/api/guidance/session/${sessionId}/enter-in-progress`, {
    method: "POST"
  });
  return parseEnvelope<boolean>(response);
}

export async function updateGuidanceCurrentVideo(sessionId: number, currentVideoId: string): Promise<boolean> {
  const response = await apiFetch(`${API_ORIGIN}/api/guidance/session/${sessionId}/current-video`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ currentVideoId })
  });
  return parseEnvelope<boolean>(response);
}

export async function completeGuidanceSession(sessionId: number): Promise<boolean> {
  const response = await apiFetch(`${API_ORIGIN}/api/guidance/session/${sessionId}/complete`, {
    method: "POST"
  });
  return parseEnvelope<boolean>(response);
}
