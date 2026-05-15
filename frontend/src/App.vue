<script setup lang="ts">
import { RouterLink, RouterView, useRoute, useRouter } from "vue-router";
import { onMounted, watch } from "vue";
import { storeToRefs } from "pinia";
import { activeGuidance, refreshActiveGuidance } from "./guidance/activeGuidanceState";
import { logout as logoutApi } from "./api/user";
import { useLoginUserStore } from "./stores/loginUser";

const router = useRouter();
const route = useRoute();
const loginUserStore = useLoginUserStore();
const { loginUser } = storeToRefs(loginUserStore);

onMounted(async () => {
  await loginUserStore.fetchLoginUser();
  await refreshActiveGuidance(loginUser.value);
});

watch(
  () => ("id" in loginUser.value ? loginUser.value.id : null),
  () => {
    void refreshActiveGuidance(loginUser.value);
  }
);

watch(
  () => route.fullPath,
  () => {
    void refreshActiveGuidance(loginUser.value);
  }
);

async function onLogout() {
  try {
    await logoutApi();
  } catch {
    /* ignore */
  }
  loginUserStore.clearLoginUser();
  activeGuidance.value = null;
  await router.replace("/user/login");
}
</script>

<template>
  <div class="app-shell">
    <header class="cyber-nav">
      <RouterLink to="/">Notes</RouterLink>
      <RouterLink to="/guidance/profile">沉浸导学</RouterLink>
      <RouterLink to="/guidance/plan">学习方案</RouterLink>
      <span class="spacer" />
      <template v-if="'id' in loginUser && loginUser.id != null">
        <span class="who">{{ loginUser.userName }} ({{ loginUser.userAccount }})</span>
        <button type="button" class="linklike" @click="onLogout">退出登录</button>
      </template>
      <RouterLink v-else to="/user/login">登录</RouterLink>
    </header>
    <div v-if="activeGuidance" class="guidance-banner">
      <span class="guidance-banner__text">
        导学进行中 · {{ activeGuidance.subjectOrTopic }} — 当前：{{
          activeGuidance.currentVideoTitle || activeGuidance.currentVideoId
        }}
        <span class="guidance-banner__meta">（{{ activeGuidance.status }}）</span>
      </span>
      <RouterLink class="guidance-banner__link" :to="activeGuidance.studyPlanPath">打开方案 / 视频条目</RouterLink>
      <span class="guidance-banner__hint">结束：学习方案页底部「标记本阶段完成」</span>
    </div>
    <div class="app-shell__main">
      <RouterView />
    </div>
  </div>
</template>

<style scoped>
.guidance-banner {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem 1rem;
  padding: 0.5rem 1rem;
  font-size: 0.88rem;
  border-bottom: 1px solid var(--border-strong, rgba(255, 255, 255, 0.12));
  background: rgba(0, 255, 200, 0.06);
}

.guidance-banner__meta {
  opacity: 0.75;
}

.guidance-banner__hint {
  flex-basis: 100%;
  font-size: 0.8rem;
  opacity: 0.8;
}

.guidance-banner__link {
  font-weight: 600;
  white-space: nowrap;
}
</style>
