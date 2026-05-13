<script setup lang="ts">
import { RouterLink, RouterView, useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import { logout as logoutApi } from "./api/user";
import { useLoginUserStore } from "./stores/loginUser";

const router = useRouter();
const loginUserStore = useLoginUserStore();
const { loginUser } = storeToRefs(loginUserStore);

async function onLogout() {
  try {
    await logoutApi();
  } catch {
    /* ignore */
  }
  loginUserStore.clearLoginUser();
  await router.replace("/user/login");
}
</script>

<template>
  <div class="app-shell">
    <header class="cyber-nav">
      <RouterLink to="/">Notes</RouterLink>
      <span class="spacer" />
      <template v-if="'id' in loginUser && loginUser.id != null">
        <span class="who">{{ loginUser.userName }} ({{ loginUser.userAccount }})</span>
        <button type="button" class="linklike" @click="onLogout">退出登录</button>
      </template>
      <RouterLink v-else to="/user/login">登录</RouterLink>
    </header>
    <div class="app-shell__main">
      <RouterView />
    </div>
  </div>
</template>
