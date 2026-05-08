import { ref } from "vue";
import { processAudio } from "./api/note";
const selectedFile = ref(null);
const loading = ref(false);
const errorMessage = ref("");
const result = ref(null);
function onSelectFile(event) {
    const target = event.target;
    const file = target.files?.[0] ?? null;
    errorMessage.value = "";
    result.value = null;
    if (!file) {
        selectedFile.value = null;
        return;
    }
    if (!file.name.toLowerCase().endsWith(".mp3")) {
        errorMessage.value = "Only MP3 files are supported in this MVP.";
        selectedFile.value = null;
        return;
    }
    const maxSize = 25 * 1024 * 1024;
    if (file.size > maxSize) {
        errorMessage.value = "File exceeds 25MB size limit.";
        selectedFile.value = null;
        return;
    }
    selectedFile.value = file;
}
async function onSubmit() {
    if (!selectedFile.value || loading.value) {
        return;
    }
    loading.value = true;
    errorMessage.value = "";
    result.value = null;
    try {
        result.value = await processAudio(selectedFile.value);
    }
    catch (error) {
        errorMessage.value = error instanceof Error ? error.message : "Processing failed";
    }
    finally {
        loading.value = false;
    }
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.main, __VLS_intrinsicElements.main)({
    ...{ class: "container" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h1, __VLS_intrinsicElements.h1)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ onChange: (__VLS_ctx.onSelectFile) },
    type: "file",
    accept: ".mp3,audio/mpeg",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.onSubmit) },
    disabled: (!__VLS_ctx.selectedFile || __VLS_ctx.loading),
});
(__VLS_ctx.loading ? "Processing..." : "Generate Notes");
if (__VLS_ctx.errorMessage) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "error" },
    });
    (__VLS_ctx.errorMessage);
}
if (__VLS_ctx.result) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
        ...{ class: "card" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({});
    (__VLS_ctx.result.title);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    (__VLS_ctx.result.sourceFilename);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    (__VLS_ctx.result.abstractText);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.a, __VLS_intrinsicElements.a)({
        href: (`http://localhost:8080${__VLS_ctx.result.downloadUrl}`),
        target: "_blank",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.pre, __VLS_intrinsicElements.pre)({});
    (__VLS_ctx.result.markdownPreview);
}
/** @type {__VLS_StyleScopedClasses['container']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
/** @type {__VLS_StyleScopedClasses['error']} */ ;
/** @type {__VLS_StyleScopedClasses['card']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            selectedFile: selectedFile,
            loading: loading,
            errorMessage: errorMessage,
            result: result,
            onSelectFile: onSelectFile,
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
