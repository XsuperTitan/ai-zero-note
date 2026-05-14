<script setup lang="ts">
import { reactive, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import { register } from "../../api/user";

const router = useRouter();

const formState = reactive({
  userAccount: "",
  userPassword: "",
  checkPassword: "",
  inviteCode: ""
});

const errorMessage = ref("");
const loading = ref(false);

async function handleSubmit() {
  errorMessage.value = "";
  if (formState.userPassword !== formState.checkPassword) {
    errorMessage.value = "两次输入的密码不一致";
    return;
  }
  loading.value = true;
  try {
    await register(
      formState.userAccount.trim(),
      formState.userPassword,
      formState.checkPassword,
      formState.inviteCode.trim()
    );
    await router.replace("/user/login");
  } catch (e) {
    errorMessage.value = e instanceof Error ? e.message : "注册失败";
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div id="userRegisterPage" class="cyber-auth-page">
    <h2 class="title cyber-display">注册</h2>
    <div class="desc">需要邀请码才能完成注册</div>
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
        autocomplete="new-password"
        required
        minlength="8"
      />

      <label class="cyber-field-label">确认密码</label>
      <input
        v-model="formState.checkPassword"
        class="cyber-field"
        type="password"
        autocomplete="new-password"
        required
        minlength="8"
      />

      <label class="cyber-field-label">邀请码</label>
      <input
        v-model="formState.inviteCode"
        class="cyber-field"
        type="text"
        autocomplete="off"
        required
        placeholder="ninifun"
      />

      <div class="tips">
        已有账号？
        <RouterLink class="cyber-link" to="/user/login">去登录</RouterLink>
      </div>

      <button type="submit" class="cyber-btn-primary" :disabled="loading">
        {{ loading ? "请稍候…" : "注册" }}
      </button>
      <p v-if="errorMessage" class="cyber-error">{{ errorMessage }}</p>
    </form>
  </div>
</template>
