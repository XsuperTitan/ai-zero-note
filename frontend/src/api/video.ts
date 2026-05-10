const API_BASE = "http://localhost:8080/api/video";

export interface VideoMetaResult {
  sourceUrl: string;
  title: string;
  durationSeconds: number;
  durationText: string;
  uploader: string;
  thumbnailUrl: string;
}

export interface VideoFrameItem {
  fileName: string;
  imageUrl: string;
  presentationTimestamp: number;
}

export interface VideoFrameResult {
  taskId: string;
  meta: VideoMetaResult;
  frames: VideoFrameItem[];
}

export interface VideoTextResult {
  meta: VideoMetaResult;
  subtitleText: string;
  textContent: string;
}

interface FrameQuery {
  url: string;
}

function toErrorMessage(defaultMessage: string, payload: unknown): string {
  if (payload && typeof payload === "object" && "error" in payload) {
    const value = (payload as { error?: unknown }).error;
    if (typeof value === "string" && value.trim().length > 0) {
      return value;
    }
  }
  return defaultMessage;
}

async function request<T>(url: string, defaultError: string): Promise<T> {
  const response = await fetch(url);
  if (!response.ok) {
    const errorBody = await response.json().catch(() => ({}));
    throw new Error(toErrorMessage(defaultError, errorBody));
  }
  return (await response.json()) as T;
}

export async function fetchVideoMeta(url: string): Promise<VideoMetaResult> {
  const params = new URLSearchParams({ url });
  return request<VideoMetaResult>(`${API_BASE}/meta?${params.toString()}`, "视频信息解析失败");
}

export async function fetchVideoText(url: string): Promise<VideoTextResult> {
  const params = new URLSearchParams({ url });
  return request<VideoTextResult>(`${API_BASE}/text?${params.toString()}`, "视频文本提取失败");
}

export async function fetchVideoFrames(query: FrameQuery): Promise<VideoFrameResult> {
  const params = new URLSearchParams({ url: query.url });
  return request<VideoFrameResult>(`${API_BASE}/frames?${params.toString()}`, "关键截图生成失败");
}

export function resolveVideoAssetUrl(path: string): string {
  if (path.startsWith("http://") || path.startsWith("https://")) {
    return path;
  }
  return `http://localhost:8080${path}`;
}
