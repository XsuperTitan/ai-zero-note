import { computed, nextTick, onMounted, ref } from "vue";
import { useRoute, useRouter, RouterLink } from "vue-router";
import { completeGuidanceSession, createGuidanceCheckIn, enterGuidanceSessionInProgress, generateStudyPlan, getLatestStudyPlan, getStudyPlanBySessionId, supplementGuidanceCheckIn, updateGuidanceCurrentVideo } from "../../api/guidance";
import { getLoginUser } from "../../api/user";
const route = useRoute();
const router = useRouter();
const bootLoading = ref(true);
const loading = ref(false);
const errorMessage = ref("");
const plan = ref(null);
/** When URL has sessionId but plan not generated yet */
const pendingSessionId = ref(null);
const generationMode = ref("TEMPLATE");
const checkInRemark = ref("");
const checkInVideoUrl = ref("");
const checkInTranscript = ref("");
const lastCheckInId = ref(null);
const checkInLoading = ref(false);
const checkInMessage = ref("");
const sessionIdQuery = computed(() => {
    const raw = route.query.sessionId;
    if (raw == null || Array.isArray(raw)) {
        return null;
    }
    const s = String(raw).trim();
    if (!/^\d+$/.test(s)) {
        return null;
    }
    const n = Number(s);
    return Number.isSafeInteger(n) ? n : null;
});
const sharePath = computed(() => {
    if (!plan.value) {
        return "";
    }
    return `${window.location.origin}/guidance/plan?sessionId=${String(plan.value.sessionId)}`;
});
onMounted(async () => {
    try {
        const user = await getLoginUser();
        if (!user) {
            await router.replace("/user/login");
            return;
        }
        await loadPlan();
    }
    finally {
        bootLoading.value = false;
    }
});
function isNoPlanMessage(msg) {
    return msg.includes("暂无学习方案") || msg.includes("请先生成");
}
async function loadPlan() {
    errorMessage.value = "";
    const sid = sessionIdQuery.value;
    if (sid != null) {
        try {
            plan.value = await getStudyPlanBySessionId(sid);
            pendingSessionId.value = null;
            await afterPlanLoaded();
            return;
        }
        catch (e) {
            const msg = e instanceof Error ? e.message : String(e);
            if (isNoPlanMessage(msg)) {
                plan.value = null;
                pendingSessionId.value = sid;
                return;
            }
            errorMessage.value = msg;
            return;
        }
    }
    try {
        plan.value = await getLatestStudyPlan();
        pendingSessionId.value = null;
        await afterPlanLoaded();
    }
    catch (e) {
        plan.value = null;
        pendingSessionId.value = null;
        errorMessage.value =
            e instanceof Error ? e.message : "暂无学习方案：请先在问卷页提交，再通过「学习方案」进入并生成。";
    }
}
async function onGenerate() {
    const sid = pendingSessionId.value ?? plan.value?.sessionId ?? sessionIdQuery.value;
    if (sid == null) {
        errorMessage.value = "缺少 sessionId：请从问卷页进入。";
        return;
    }
    if (loading.value) {
        return;
    }
    loading.value = true;
    errorMessage.value = "";
    try {
        plan.value = await generateStudyPlan(sid, generationMode.value);
        pendingSessionId.value = null;
        await router.replace({ path: "/guidance/plan", query: { sessionId: String(sid) } });
        await afterPlanLoaded();
    }
    catch (e) {
        errorMessage.value = e instanceof Error ? e.message : "生成失败";
    }
    finally {
        loading.value = false;
    }
}
async function copyShareLink() {
    if (!sharePath.value) {
        return;
    }
    try {
        await navigator.clipboard.writeText(sharePath.value);
    }
    catch {
        errorMessage.value = "复制失败，请手动复制地址栏链接。";
    }
}
async function copyOutlineText() {
    if (!plan.value) {
        return;
    }
    const lines = [];
    lines.push(plan.value.outlineMarkdown.trim());
    lines.push("");
    lines.push("## 建议");
    for (const s of plan.value.suggestions) {
        lines.push(`- ${s}`);
    }
    lines.push("");
    lines.push("## 优先级");
    for (const p of plan.value.priorities) {
        lines.push(`- ${p}`);
    }
    lines.push("");
    lines.push("## 视频路线");
    for (const v of plan.value.videos) {
        const mark = v.id === plan.value.currentVideoId ? "【当前先看】" : "";
        lines.push(`${mark}${v.sortOrder}. ${v.title} (${v.platform})`);
        if (v.url) {
            lines.push(`   ${v.url}`);
        }
        lines.push(`   ${v.rationale}`);
    }
    try {
        await navigator.clipboard.writeText(lines.join("\n"));
    }
    catch {
        errorMessage.value = "复制失败。";
    }
}
const advanceLoading = ref(false);
const hasNextVideo = computed(() => {
    const p = plan.value;
    if (!p?.videos?.length) {
        return false;
    }
    const sorted = [...p.videos].sort((a, b) => a.sortOrder - b.sortOrder);
    const idx = sorted.findIndex((v) => v.id === p.currentVideoId);
    return idx >= 0 && idx < sorted.length - 1;
});
async function afterPlanLoaded() {
    const p = plan.value;
    if (!p?.sessionId) {
        return;
    }
    if (p.sessionStatus === "PLAN_READY") {
        try {
            await enterGuidanceSessionInProgress(p.sessionId);
            plan.value = await getStudyPlanBySessionId(p.sessionId);
        }
        catch {
            /* ignore */
        }
    }
    await scrollToCurrentVideo();
}
async function scrollToCurrentVideo() {
    const id = plan.value?.currentVideoId;
    if (!id) {
        return;
    }
    await nextTick();
    document.getElementById(`guid-video-${id}`)?.scrollIntoView({ behavior: "smooth", block: "center" });
}
async function onNextVideo() {
    const p = plan.value;
    if (!p || advanceLoading.value) {
        return;
    }
    const sorted = [...p.videos].sort((a, b) => a.sortOrder - b.sortOrder);
    const idx = sorted.findIndex((v) => v.id === p.currentVideoId);
    const next = sorted[idx + 1];
    if (!next) {
        return;
    }
    advanceLoading.value = true;
    errorMessage.value = "";
    try {
        await updateGuidanceCurrentVideo(p.sessionId, next.id);
        plan.value = await getStudyPlanBySessionId(p.sessionId);
        await scrollToCurrentVideo();
    }
    catch (e) {
        errorMessage.value = e instanceof Error ? e.message : "更新失败";
    }
    finally {
        advanceLoading.value = false;
    }
}
async function onCompleteGuidance() {
    const p = plan.value;
    if (!p || advanceLoading.value) {
        return;
    }
    advanceLoading.value = true;
    errorMessage.value = "";
    try {
        await completeGuidanceSession(p.sessionId);
        plan.value = await getStudyPlanBySessionId(p.sessionId);
    }
    catch (e) {
        errorMessage.value = e instanceof Error ? e.message : "操作失败";
    }
    finally {
        advanceLoading.value = false;
    }
}
async function onSubmitCheckIn() {
    const p = plan.value;
    if (!p || checkInLoading.value) {
        return;
    }
    checkInLoading.value = true;
    checkInMessage.value = "";
    try {
        const r = await createGuidanceCheckIn(p.sessionId, checkInRemark.value.trim() || undefined);
        lastCheckInId.value = r.checkInId;
        checkInMessage.value = `打卡已保存。可补充链接/转写后点「更新补充素材」，再「去首页生成笔记」。`;
    }
    catch (e) {
        checkInMessage.value = e instanceof Error ? e.message : "打卡失败";
    }
    finally {
        checkInLoading.value = false;
    }
}
async function onSupplementCheckIn() {
    if (lastCheckInId.value == null || checkInLoading.value) {
        return;
    }
    checkInLoading.value = true;
    checkInMessage.value = "";
    try {
        const r = await supplementGuidanceCheckIn(lastCheckInId.value, {
            videoUrl: checkInVideoUrl.value.trim() || undefined,
            transcriptText: checkInTranscript.value.trim() || undefined
        });
        checkInMessage.value = `补充已更新（打卡 #${r.checkInId}）。`;
    }
    catch (e) {
        checkInMessage.value = e instanceof Error ? e.message : "更新失败";
    }
    finally {
        checkInLoading.value = false;
    }
}
function goNotesWithCheckIn() {
    if (lastCheckInId.value == null) {
        return;
    }
    void router.push({ path: "/", query: { guidanceCheckInId: String(lastCheckInId.value) } });
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['page-plan']} */ ;
/** @type {__VLS_StyleScopedClasses['page-lead']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-list']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.main, __VLS_intrinsicElements.main)({
    ...{ class: "page-plan" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h1, __VLS_intrinsicElements.h1)({
    ...{ class: "cyber-display" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "cyber-muted page-lead" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.code, __VLS_intrinsicElements.code)({});
if (__VLS_ctx.bootLoading) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "cyber-muted" },
    });
}
else {
    if (__VLS_ctx.pendingSessionId != null && __VLS_ctx.plan == null) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
            ...{ class: "cyber-card" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "cyber-muted" },
        });
        (__VLS_ctx.pendingSessionId);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
            ...{ class: "cyber-field-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
            value: (__VLS_ctx.generationMode),
            ...{ class: "cyber-field" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
            value: "TEMPLATE",
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
            value: "LLM",
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (__VLS_ctx.onGenerate) },
            type: "button",
            ...{ class: "cyber-btn-primary" },
            disabled: (__VLS_ctx.loading),
        });
        (__VLS_ctx.loading ? "生成中…" : "生成学习方案");
        if (__VLS_ctx.errorMessage) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
                ...{ class: "cyber-error" },
            });
            (__VLS_ctx.errorMessage);
        }
    }
    else if (__VLS_ctx.plan) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
            ...{ class: "cyber-card" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "plan-toolbar" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "cyber-muted" },
        });
        (__VLS_ctx.plan.sessionId);
        (__VLS_ctx.plan.sessionStatus ?? "—");
        (__VLS_ctx.plan.generationSource);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.code, __VLS_intrinsicElements.code)({});
        (__VLS_ctx.plan.currentVideoId);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "toolbar-actions" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (__VLS_ctx.copyShareLink) },
            type: "button",
            ...{ class: "cyber-btn-ghost" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (__VLS_ctx.copyOutlineText) },
            type: "button",
            ...{ class: "cyber-btn-ghost" },
        });
        if (__VLS_ctx.plan.sessionStatus !== 'COMPLETED') {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                ...{ onClick: (__VLS_ctx.onNextVideo) },
                type: "button",
                ...{ class: "cyber-btn-ghost" },
                disabled: (__VLS_ctx.advanceLoading || !__VLS_ctx.hasNextVideo),
            });
            (__VLS_ctx.advanceLoading ? "…" : "下一条视频");
        }
        if (__VLS_ctx.plan.sessionStatus !== 'COMPLETED') {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                ...{ onClick: (__VLS_ctx.onCompleteGuidance) },
                type: "button",
                ...{ class: "cyber-btn-ghost" },
                disabled: (__VLS_ctx.advanceLoading),
            });
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (__VLS_ctx.onGenerate) },
            type: "button",
            ...{ class: "cyber-btn-ghost" },
            disabled: (__VLS_ctx.loading),
        });
        (__VLS_ctx.loading ? "…" : "重新生成");
        __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
            ...{ class: "cyber-field-label inline-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
            value: (__VLS_ctx.generationMode),
            ...{ class: "cyber-field narrow-select" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
            value: "TEMPLATE",
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
            value: "LLM",
        });
        if (__VLS_ctx.plan.sessionStatus === 'COMPLETED') {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
                ...{ class: "cyber-muted" },
            });
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.pre, __VLS_intrinsicElements.pre)({
            ...{ class: "cyber-pre outline-pre" },
        });
        (__VLS_ctx.plan.outlineMarkdown);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.ul, __VLS_intrinsicElements.ul)({
            ...{ class: "cyber-list" },
        });
        for (const [s, i] of __VLS_getVForSourceType((__VLS_ctx.plan.suggestions))) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.li, __VLS_intrinsicElements.li)({
                key: ('s' + i),
            });
            (s);
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.ol, __VLS_intrinsicElements.ol)({
            ...{ class: "cyber-list ordered" },
        });
        for (const [p, i] of __VLS_getVForSourceType((__VLS_ctx.plan.priorities))) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.li, __VLS_intrinsicElements.li)({
                key: ('p' + i),
            });
            (p);
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.ul, __VLS_intrinsicElements.ul)({
            ...{ class: "video-list" },
        });
        for (const [v] of __VLS_getVForSourceType((__VLS_ctx.plan.videos))) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.li, __VLS_intrinsicElements.li)({
                id: ('guid-video-' + v.id),
                key: (v.id),
                ...{ class: "video-item" },
                ...{ class: ({ 'video-item--current': v.id === __VLS_ctx.plan.currentVideoId }) },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
                ...{ class: "video-head" },
            });
            if (v.id === __VLS_ctx.plan.currentVideoId) {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                    ...{ class: "badge-current" },
                });
            }
            __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
            (v.sortOrder);
            (v.title);
            __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
                ...{ class: "cyber-muted" },
            });
            (v.platform);
            (v.linkKind);
            __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
                ...{ class: "video-rationale" },
            });
            (v.rationale);
            if (v.url) {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.a, __VLS_intrinsicElements.a)({
                    ...{ class: "cyber-link" },
                    href: (v.url),
                    target: "_blank",
                    rel: "noopener noreferrer",
                });
            }
            else {
                __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
                    ...{ class: "cyber-muted" },
                });
            }
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({
            ...{ class: "section-checkin" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "cyber-muted" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.code, __VLS_intrinsicElements.code)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
            ...{ class: "cyber-field-label" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.textarea)({
            value: (__VLS_ctx.checkInRemark),
            ...{ class: "cyber-field" },
            rows: "2",
            placeholder: "例如：今天看到第 3 节，疑点在 …",
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (__VLS_ctx.onSubmitCheckIn) },
            type: "button",
            ...{ class: "cyber-btn-primary" },
            disabled: (__VLS_ctx.checkInLoading),
        });
        (__VLS_ctx.checkInLoading ? "…" : "提交打卡");
        if (__VLS_ctx.lastCheckInId != null) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
                ...{ class: "cyber-muted" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
            (__VLS_ctx.lastCheckInId);
        }
        if (__VLS_ctx.lastCheckInId != null) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
                ...{ class: "cyber-field-label" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
                ...{ class: "cyber-field" },
                type: "url",
                placeholder: "https://...",
            });
            (__VLS_ctx.checkInVideoUrl);
            __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
                ...{ class: "cyber-field-label" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.textarea)({
                value: (__VLS_ctx.checkInTranscript),
                ...{ class: "cyber-field" },
                rows: "4",
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
                ...{ class: "checkin-actions" },
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                ...{ onClick: (__VLS_ctx.onSupplementCheckIn) },
                type: "button",
                ...{ class: "cyber-btn-ghost" },
                disabled: (__VLS_ctx.checkInLoading),
            });
            __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
                ...{ onClick: (__VLS_ctx.goNotesWithCheckIn) },
                type: "button",
                ...{ class: "cyber-btn-ghost" },
            });
        }
        if (__VLS_ctx.checkInMessage) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
                ...{ class: "cyber-muted" },
            });
            (__VLS_ctx.checkInMessage);
        }
        if (__VLS_ctx.errorMessage) {
            __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
                ...{ class: "cyber-error" },
            });
            (__VLS_ctx.errorMessage);
        }
    }
    else {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
            ...{ class: "cyber-card" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "cyber-error" },
        });
        (__VLS_ctx.errorMessage || "暂无数据");
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "cyber-muted" },
        });
        const __VLS_0 = {}.RouterLink;
        /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
        // @ts-ignore
        const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
            to: "/guidance/profile",
        }));
        const __VLS_2 = __VLS_1({
            to: "/guidance/profile",
        }, ...__VLS_functionalComponentArgsRest(__VLS_1));
        __VLS_3.slots.default;
        var __VLS_3;
        __VLS_asFunctionalElement(__VLS_intrinsicElements.code, __VLS_intrinsicElements.code)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
            ...{ onClick: (__VLS_ctx.loadPlan) },
            type: "button",
            ...{ class: "cyber-btn-primary" },
        });
    }
}
/** @type {__VLS_StyleScopedClasses['page-plan']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-display']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-muted']} */ ;
/** @type {__VLS_StyleScopedClasses['page-lead']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-muted']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-card']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-muted']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field-label']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-btn-primary']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-error']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-card']} */ ;
/** @type {__VLS_StyleScopedClasses['plan-toolbar']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-muted']} */ ;
/** @type {__VLS_StyleScopedClasses['toolbar-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-btn-ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-btn-ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-btn-ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-btn-ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-btn-ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field-label']} */ ;
/** @type {__VLS_StyleScopedClasses['inline-label']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field']} */ ;
/** @type {__VLS_StyleScopedClasses['narrow-select']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-muted']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-pre']} */ ;
/** @type {__VLS_StyleScopedClasses['outline-pre']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-list']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-list']} */ ;
/** @type {__VLS_StyleScopedClasses['ordered']} */ ;
/** @type {__VLS_StyleScopedClasses['video-list']} */ ;
/** @type {__VLS_StyleScopedClasses['video-item']} */ ;
/** @type {__VLS_StyleScopedClasses['video-head']} */ ;
/** @type {__VLS_StyleScopedClasses['badge-current']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-muted']} */ ;
/** @type {__VLS_StyleScopedClasses['video-rationale']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-link']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-muted']} */ ;
/** @type {__VLS_StyleScopedClasses['section-checkin']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-muted']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field-label']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-btn-primary']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-muted']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field-label']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field-label']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field']} */ ;
/** @type {__VLS_StyleScopedClasses['checkin-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-btn-ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-btn-ghost']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-muted']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-error']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-card']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-error']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-muted']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-btn-primary']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            RouterLink: RouterLink,
            bootLoading: bootLoading,
            loading: loading,
            errorMessage: errorMessage,
            plan: plan,
            pendingSessionId: pendingSessionId,
            generationMode: generationMode,
            checkInRemark: checkInRemark,
            checkInVideoUrl: checkInVideoUrl,
            checkInTranscript: checkInTranscript,
            lastCheckInId: lastCheckInId,
            checkInLoading: checkInLoading,
            checkInMessage: checkInMessage,
            loadPlan: loadPlan,
            onGenerate: onGenerate,
            copyShareLink: copyShareLink,
            copyOutlineText: copyOutlineText,
            advanceLoading: advanceLoading,
            hasNextVideo: hasNextVideo,
            onNextVideo: onNextVideo,
            onCompleteGuidance: onCompleteGuidance,
            onSubmitCheckIn: onSubmitCheckIn,
            onSupplementCheckIn: onSupplementCheckIn,
            goNotesWithCheckIn: goNotesWithCheckIn,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
