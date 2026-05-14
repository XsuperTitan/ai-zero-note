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
        await register(formState.userAccount.trim(), formState.userPassword, formState.checkPassword, formState.inviteCode.trim());
        await router.replace("/user/login");
    }
    catch (e) {
        errorMessage.value = e instanceof Error ? e.message : "注册失败";
    }
    finally {
        loading.value = false;
    }
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    id: "userRegisterPage",
    ...{ class: "cyber-auth-page" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({
    ...{ class: "title cyber-display" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "desc" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.form, __VLS_intrinsicElements.form)({
    ...{ onSubmit: (__VLS_ctx.handleSubmit) },
    ...{ class: "cyber-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "cyber-field-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    value: (__VLS_ctx.formState.userAccount),
    ...{ class: "cyber-field" },
    type: "text",
    autocomplete: "username",
    required: true,
    minlength: "4",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "cyber-field-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ class: "cyber-field" },
    type: "password",
    autocomplete: "new-password",
    required: true,
    minlength: "8",
});
(__VLS_ctx.formState.userPassword);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "cyber-field-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ class: "cyber-field" },
    type: "password",
    autocomplete: "new-password",
    required: true,
    minlength: "8",
});
(__VLS_ctx.formState.checkPassword);
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "cyber-field-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    value: (__VLS_ctx.formState.inviteCode),
    ...{ class: "cyber-field" },
    type: "text",
    autocomplete: "off",
    required: true,
    placeholder: "ninifun",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "tips" },
});
const __VLS_0 = {}.RouterLink;
/** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    ...{ class: "cyber-link" },
    to: "/user/login",
}));
const __VLS_2 = __VLS_1({
    ...{ class: "cyber-link" },
    to: "/user/login",
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
__VLS_3.slots.default;
var __VLS_3;
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    type: "submit",
    ...{ class: "cyber-btn-primary" },
    disabled: (__VLS_ctx.loading),
});
(__VLS_ctx.loading ? "请稍候…" : "注册");
if (__VLS_ctx.errorMessage) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "cyber-error" },
    });
    (__VLS_ctx.errorMessage);
}
/** @type {__VLS_StyleScopedClasses['cyber-auth-page']} */ ;
/** @type {__VLS_StyleScopedClasses['title']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-display']} */ ;
/** @type {__VLS_StyleScopedClasses['desc']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-card']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field-label']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field-label']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field-label']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field-label']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field']} */ ;
/** @type {__VLS_StyleScopedClasses['tips']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-link']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-btn-primary']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-error']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            RouterLink: RouterLink,
            formState: formState,
            errorMessage: errorMessage,
            loading: loading,
            handleSubmit: handleSubmit,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
