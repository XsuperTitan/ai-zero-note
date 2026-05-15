import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { getLatestLearningProfile, submitLearningProfile } from "../../api/guidance";
import { getLoginUser } from "../../api/user";
const router = useRouter();
const tutorPersona = ref("SILVER_WOLF");
const subjectOrTopic = ref("");
const urgency = ref("MEDIUM");
const preferredPlatforms = ref("");
const usualSites = ref("");
const studyRhythm = ref("");
const contentPreference = ref("MIXED");
const extraNotes = ref("");
const loading = ref(false);
const bootLoading = ref(true);
const errorMessage = ref("");
const latest = ref(null);
onMounted(async () => {
    try {
        const user = await getLoginUser();
        if (!user) {
            await router.replace("/user/login");
            return;
        }
        try {
            latest.value = await getLatestLearningProfile();
        }
        catch {
            latest.value = null;
        }
    }
    finally {
        bootLoading.value = false;
    }
});
async function onSubmit() {
    if (loading.value) {
        return;
    }
    errorMessage.value = "";
    loading.value = true;
    try {
        const user = await getLoginUser();
        if (!user) {
            await router.replace("/user/login");
            return;
        }
        latest.value = await submitLearningProfile({
            tutorPersona: tutorPersona.value,
            subjectOrTopic: subjectOrTopic.value.trim(),
            urgency: urgency.value,
            preferredPlatforms: preferredPlatforms.value.trim(),
            usualSites: usualSites.value.trim(),
            studyRhythm: studyRhythm.value.trim(),
            contentPreference: contentPreference.value,
            extraNotes: extraNotes.value.trim() || undefined
        });
    }
    catch (e) {
        errorMessage.value = e instanceof Error ? e.message : "提交失败";
    }
    finally {
        loading.value = false;
    }
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['page-guidance']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.main, __VLS_intrinsicElements.main)({
    ...{ class: "page-guidance" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h1, __VLS_intrinsicElements.h1)({
    ...{ class: "cyber-display" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "cyber-muted page-lead" },
});
if (__VLS_ctx.bootLoading) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "cyber-muted" },
    });
}
else {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
        ...{ class: "cyber-card" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
        ...{ class: "cyber-field-label" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
        value: (__VLS_ctx.tutorPersona),
        ...{ class: "cyber-field" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        value: "SILVER_WOLF",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        value: "STREAM_VETERAN",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        value: "NAMELESS_SELF",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
        ...{ class: "cyber-field-label" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        value: (__VLS_ctx.subjectOrTopic),
        ...{ class: "cyber-field" },
        type: "text",
        placeholder: "例如：线性代数 — 特征值",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
        ...{ class: "cyber-field-label" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
        value: (__VLS_ctx.urgency),
        ...{ class: "cyber-field" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        value: "LOW",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        value: "MEDIUM",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        value: "HIGH",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
        ...{ class: "cyber-field-label" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        value: (__VLS_ctx.preferredPlatforms),
        ...{ class: "cyber-field" },
        type: "text",
        placeholder: "例如：哔哩哔哩、Coursera、教材 PDF…",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
        ...{ class: "cyber-field-label" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        value: (__VLS_ctx.usualSites),
        ...{ class: "cyber-field" },
        type: "text",
        placeholder: "例如：某个 UP 主页、课程目录链接…",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
        ...{ class: "cyber-field-label" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
        value: (__VLS_ctx.studyRhythm),
        ...{ class: "cyber-field" },
        type: "text",
        placeholder: "例如：工作日 1h / 周末集中 4h",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
        ...{ class: "cyber-field-label" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
        value: (__VLS_ctx.contentPreference),
        ...{ class: "cyber-field" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        value: "VIDEO",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        value: "ARTICLE",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        value: "MIXED",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
        ...{ class: "cyber-field-label" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.textarea)({
        value: (__VLS_ctx.extraNotes),
        ...{ class: "cyber-field" },
        rows: "4",
        placeholder: "学习目标、考试日期、禁忌等",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.onSubmit) },
        type: "button",
        ...{ class: "cyber-btn-primary" },
        disabled: (__VLS_ctx.loading),
    });
    (__VLS_ctx.loading ? "提交中…" : "提交并生成报告");
    if (__VLS_ctx.errorMessage) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "cyber-error" },
        });
        (__VLS_ctx.errorMessage);
    }
    if (__VLS_ctx.latest) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
            ...{ class: "cyber-card report-card" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "cyber-muted" },
        });
        (__VLS_ctx.latest.sessionId);
        (__VLS_ctx.latest.status);
        (__VLS_ctx.latest.tutorPersona);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.pre, __VLS_intrinsicElements.pre)({
            ...{ class: "cyber-pre report-pre" },
        });
        (__VLS_ctx.latest.reportSummary);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({
            ...{ class: "section-mini-title" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.pre, __VLS_intrinsicElements.pre)({
            ...{ class: "cyber-pre constraints-pre" },
        });
        (__VLS_ctx.latest.llmPromptConstraints);
    }
}
/** @type {__VLS_StyleScopedClasses['page-guidance']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-display']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-muted']} */ ;
/** @type {__VLS_StyleScopedClasses['page-lead']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-muted']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-card']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field-label']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field-label']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field-label']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field-label']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field-label']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field-label']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field-label']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field-label']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-btn-primary']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-error']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-card']} */ ;
/** @type {__VLS_StyleScopedClasses['report-card']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-muted']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-pre']} */ ;
/** @type {__VLS_StyleScopedClasses['report-pre']} */ ;
/** @type {__VLS_StyleScopedClasses['section-mini-title']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-pre']} */ ;
/** @type {__VLS_StyleScopedClasses['constraints-pre']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            tutorPersona: tutorPersona,
            subjectOrTopic: subjectOrTopic,
            urgency: urgency,
            preferredPlatforms: preferredPlatforms,
            usualSites: usualSites,
            studyRhythm: studyRhythm,
            contentPreference: contentPreference,
            extraNotes: extraNotes,
            loading: loading,
            bootLoading: bootLoading,
            errorMessage: errorMessage,
            latest: latest,
            onSubmit: onSubmit,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
