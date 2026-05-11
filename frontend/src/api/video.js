const API_BASE = "http://localhost:8080/api/video";
function toErrorMessage(defaultMessage, payload) {
    if (payload && typeof payload === "object" && "error" in payload) {
        const value = payload.error;
        if (typeof value === "string" && value.trim().length > 0) {
            return value;
        }
    }
    return defaultMessage;
}
async function request(url, defaultError) {
    const response = await fetch(url);
    if (!response.ok) {
        const errorBody = await response.json().catch(() => ({}));
        throw new Error(toErrorMessage(defaultError, errorBody));
    }
    return (await response.json());
}
export async function fetchVideoMeta(url) {
    const params = new URLSearchParams({ url });
    return request(`${API_BASE}/meta?${params.toString()}`, "视频信息解析失败");
}
export async function fetchVideoText(url) {
    const params = new URLSearchParams({ url });
    return request(`${API_BASE}/text?${params.toString()}`, "视频文本提取失败");
}
export async function fetchVideoFrames(query) {
    const params = new URLSearchParams({ url: query.url });
    return request(`${API_BASE}/frames?${params.toString()}`, "关键截图生成失败");
}
export async function fetchVideoVisionText(url, taskId, fileNames, targetLanguage) {
    const params = new URLSearchParams({ url, taskId, targetLanguage });
    fileNames.forEach((fileName) => params.append("fileName", fileName));
    return request(`${API_BASE}/vision-text?${params.toString()}`, "图生文提取失败");
}
export function resolveVideoAssetUrl(path) {
    if (path.startsWith("http://") || path.startsWith("https://")) {
        return path;
    }
    return `http://localhost:8080${path}`;
}
