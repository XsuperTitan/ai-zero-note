import { ref } from "vue";
import { useRouter } from "vue-router";
import { login, register } from "../api/user";
const router = useRouter();
const mode = ref("login");
const userAccount = ref("");
const userPassword = ref("");
const checkPassword = ref("");
const errorMessage = ref("");
const loading = ref(false);
async function onSubmit() {
    errorMessage.value = "";
    loading.value = true;
    try {
        if (mode.value === "register") {
            await register(userAccount.value.trim(), userPassword.value, checkPassword.value);
        }
        await login(userAccount.value.trim(), userPassword.value);
        await router.push("/");
    }
    catch (e) {
        errorMessage.value = e instanceof Error ? e.message : "Request failed";
    }
    finally {
        loading.value = false;
    }
}
function toggleMode() {
    errorMessage.value = "";
    mode.value = mode.value === "login" ? "register" : "login";
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.main, __VLS_intrinsicElements.main)({
    ...{ class: "wrap" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h1, __VLS_intrinsicElements.h1)({});
(__VLS_ctx.mode === "login" ? "Login" : "Register");
__VLS_asFunctionalElement(__VLS_intrinsicElements.form, __VLS_intrinsicElements.form)({
    ...{ onSubmit: (__VLS_ctx.onSubmit) },
    ...{ class: "card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    value: (__VLS_ctx.userAccount),
    type: "text",
    autocomplete: "username",
    required: true,
    minlength: "4",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    type: "password",
    autocomplete: "current-password",
    required: true,
    minlength: "8",
});
(__VLS_ctx.userPassword);
if (__VLS_ctx.mode === 'register') {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        type: "password",
        autocomplete: "new-password",
        required: true,
        minlength: "8",
    });
    (__VLS_ctx.checkPassword);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    type: "submit",
    disabled: (__VLS_ctx.loading),
});
(__VLS_ctx.loading ? "Please wait…" : __VLS_ctx.mode === "login" ? "Login" : "Register & login");
if (__VLS_ctx.errorMessage) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "err" },
    });
    (__VLS_ctx.errorMessage);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.toggleMode) },
    type: "button",
    ...{ class: "ghost" },
});
(__VLS_ctx.mode === "login" ? "Need an account? Register" : "Have an account? Login");
/** @type {__VLS_StyleScopedClasses['wrap']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['err']} */ ;
/** @type {__VLS_StyleScopedClasses['ghost']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            mode: mode,
            userAccount: userAccount,
            userPassword: userPassword,
            checkPassword: checkPassword,
            errorMessage: errorMessage,
            loading: loading,
            onSubmit: onSubmit,
            toggleMode: toggleMode,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
