import { ref } from "vue";
import { getActiveGuidanceProgress, type GuidanceActiveProgressResponse } from "../api/guidance";

/**
 * Shared across App shell and HomeView so「导学进行中」不会只在部分路由可见。
 */
export const activeGuidance = ref<GuidanceActiveProgressResponse | null>(null);

export async function refreshActiveGuidance(loginUser: unknown): Promise<void> {
  if (
    typeof loginUser !== "object" ||
    loginUser === null ||
    !("id" in loginUser) ||
    (loginUser as { id?: number }).id == null
  ) {
    activeGuidance.value = null;
    return;
  }
  try {
    activeGuidance.value = await getActiveGuidanceProgress();
  } catch {
    activeGuidance.value = null;
  }
}
