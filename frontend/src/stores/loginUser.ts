import { ref } from "vue";
import { defineStore } from "pinia";
import { getLoginUser, type LoginUserVO } from "../api/user";

type GuestUser = { userName: string };

export const useLoginUserStore = defineStore("loginUser", () => {
  const loginUser = ref<LoginUserVO | GuestUser>({ userName: "未登录" });

  async function fetchLoginUser() {
    try {
      const data = await getLoginUser();
      if (data?.id != null) {
        loginUser.value = data;
      } else {
        loginUser.value = { userName: "未登录" };
      }
    } catch {
      loginUser.value = { userName: "未登录" };
    }
  }

  function clearLoginUser() {
    loginUser.value = { userName: "未登录" };
  }

  return { loginUser, fetchLoginUser, clearLoginUser };
});
