import { ref } from "vue";
import { defineStore } from "pinia";
import { getLoginUser } from "../api/user";
export const useLoginUserStore = defineStore("loginUser", () => {
    const loginUser = ref({ userName: "未登录" });
    async function fetchLoginUser() {
        try {
            const data = await getLoginUser();
            if (data?.id != null) {
                loginUser.value = data;
            }
            else {
                loginUser.value = { userName: "未登录" };
            }
        }
        catch {
            loginUser.value = { userName: "未登录" };
        }
    }
    function clearLoginUser() {
        loginUser.value = { userName: "未登录" };
    }
    return { loginUser, fetchLoginUser, clearLoginUser };
});
