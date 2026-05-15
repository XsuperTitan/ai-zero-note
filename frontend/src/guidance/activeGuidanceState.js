import { ref } from "vue";
import { getActiveGuidanceProgress } from "../api/guidance";
/**
 * Shared across App shell and HomeView so「导学进行中」不会只在部分路由可见。
 */
export const activeGuidance = ref(null);
export async function refreshActiveGuidance(loginUser) {
    if (typeof loginUser !== "object" ||
        loginUser === null ||
        !("id" in loginUser) ||
        loginUser.id == null) {
        activeGuidance.value = null;
        return;
    }
    try {
        activeGuidance.value = await getActiveGuidanceProgress();
    }
    catch {
        activeGuidance.value = null;
    }
}
