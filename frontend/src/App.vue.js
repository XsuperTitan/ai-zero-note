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
    }
    catch {
        /* ignore */
    }
    loginUserStore.clearLoginUser();
    await router.replace("/user/login");
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "app-shell" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.header, __VLS_intrinsicElements.header)({
    ...{ class: "cyber-nav" },
});
const __VLS_0 = {}.RouterLink;
/** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    to: "/",
}));
const __VLS_2 = __VLS_1({
    to: "/",
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
__VLS_3.slots.default;
var __VLS_3;
const __VLS_4 = {}.RouterLink;
/** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
// @ts-ignore
const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
    to: "/guidance/profile",
}));
const __VLS_6 = __VLS_5({
    to: "/guidance/profile",
}, ...__VLS_functionalComponentArgsRest(__VLS_5));
__VLS_7.slots.default;
var __VLS_7;
__VLS_asFunctionalElement(__VLS_intrinsicElements.span)({
    ...{ class: "spacer" },
});
if ('id' in __VLS_ctx.loginUser && __VLS_ctx.loginUser.id != null) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
        ...{ class: "who" },
    });
    (__VLS_ctx.loginUser.userName);
    (__VLS_ctx.loginUser.userAccount);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.onLogout) },
        type: "button",
        ...{ class: "linklike" },
    });
}
else {
    const __VLS_8 = {}.RouterLink;
    /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
    // @ts-ignore
    const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({
        to: "/user/login",
    }));
    const __VLS_10 = __VLS_9({
        to: "/user/login",
    }, ...__VLS_functionalComponentArgsRest(__VLS_9));
    __VLS_11.slots.default;
    var __VLS_11;
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "app-shell__main" },
});
const __VLS_12 = {}.RouterView;
/** @type {[typeof __VLS_components.RouterView, ]} */ ;
// @ts-ignore
const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({}));
const __VLS_14 = __VLS_13({}, ...__VLS_functionalComponentArgsRest(__VLS_13));
/** @type {__VLS_StyleScopedClasses['app-shell']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-nav']} */ ;
/** @type {__VLS_StyleScopedClasses['spacer']} */ ;
/** @type {__VLS_StyleScopedClasses['who']} */ ;
/** @type {__VLS_StyleScopedClasses['linklike']} */ ;
/** @type {__VLS_StyleScopedClasses['app-shell__main']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            RouterLink: RouterLink,
            RouterView: RouterView,
            loginUser: loginUser,
            onLogout: onLogout,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
