<script setup lang="ts">
import { reactive, ref } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import { login } from "../../api/user";
import { useLoginUserStore } from "../../stores/loginUser";

const router = useRouter();
const route = useRoute();
const loginUserStore = useLoginUserStore();

const formState = reactive({
  userAccount: "",
  userPassword: ""
});

const errorMessage = ref("");
const loading = ref(false);

function safeRedirect(raw: unknown): string {
  if (typeof raw !== "string" || raw.length === 0) {
    return "/";
  }
  if (!raw.startsWith("/") || raw.startsWith("//")) {
    return "/";
  }
  return raw;
}

async function handleSubmit() {
  errorMessage.value = "";
  loading.value = true;
  try {
    await login(formState.userAccount.trim(), formState.userPassword);
    await loginUserStore.fetchLoginUser();
    const target = safeRedirect(route.query.redirect);
    await router.replace(target);
  } catch (e) {
    errorMessage.value = e instanceof Error ? e.message : "登录失败";
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div id="userLoginPage" class="cyber-auth-page">
    <h2 class="title cyber-display">登录</h2>
    <div class="desc">登录后可使用笔记与视频解析等功能</div>
    <form class="cyber-card" @submit.prevent="handleSubmit">
      <label class="cyber-field-label">账号</label>
      <input
        v-model="formState.userAccount"
        class="cyber-field"
        type="text"
        autocomplete="username"
        required
        minlength="4"
      />

      <label class="cyber-field-label">密码</label>
      <input
        v-model="formState.userPassword"
        class="cyber-field"
        type="password"
        autocomplete="current-password"
        required
        minlength="8"
      />

      <div class="tips">
        没有账号？
        <RouterLink class="cyber-link" to="/user/register">去注册</RouterLink>
      </div>

      <button type="submit" class="cyber-btn-primary" :disabled="loading">
        {{ loading ? "请稍候…" : "登录" }}
      </button>
      <p v-if="errorMessage" class="cyber-error">{{ errorMessage }}</p>
    </form>
  </div>
</template>
