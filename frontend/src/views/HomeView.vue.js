import { ref } from "vue";
import { API_ORIGIN } from "../api/client";
import { processMixedInput } from "../api/note";
import VideoLinkSection from "../components/VideoLinkSection.vue";
import { insertAtCaret } from "../utils/insertAtCaret";
const selectedAudioFile = ref(null);
const selectedTextFile = ref(null);
const textContent = ref("");
const loading = ref(false);
const errorMessage = ref("");
const result = ref(null);
const textAreaRef = ref(null);
function onSelectAudioFile(event) {
    const target = event.target;
    const file = target.files?.[0] ?? null;
    errorMessage.value = "";
    result.value = null;
    if (!file) {
        selectedAudioFile.value = null;
        return;
    }
    if (!file.name.toLowerCase().endsWith(".mp3")) {
        errorMessage.value = "Only MP3 files are supported in this MVP.";
        selectedAudioFile.value = null;
        return;
    }
    const maxSize = 25 * 1024 * 1024;
    if (file.size > maxSize) {
        errorMessage.value = "File exceeds 25MB size limit.";
        selectedAudioFile.value = null;
        return;
    }
    selectedAudioFile.value = file;
}
function onSelectTextFile(event) {
    const target = event.target;
    const file = target.files?.[0] ?? null;
    errorMessage.value = "";
    result.value = null;
    if (!file) {
        selectedTextFile.value = null;
        return;
    }
    const lower = file.name.toLowerCase();
    if (!lower.endsWith(".txt") && !lower.endsWith(".md")) {
        errorMessage.value = "Text file must be .txt or .md.";
        selectedTextFile.value = null;
        return;
    }
    const maxSize = 25 * 1024 * 1024;
    if (file.size > maxSize) {
        errorMessage.value = "Text file exceeds 25MB size limit.";
        selectedTextFile.value = null;
        return;
    }
    selectedTextFile.value = file;
}
async function onSubmit() {
    const hasTextContent = textContent.value.trim().length > 0;
    if (loading.value) {
        return;
    }
    if (!selectedAudioFile.value && !selectedTextFile.value && !hasTextContent) {
        errorMessage.value = "Please provide audio file, text file, or text content.";
        return;
    }
    loading.value = true;
    errorMessage.value = "";
    result.value = null;
    try {
        result.value = await processMixedInput({
            audioFile: selectedAudioFile.value,
            textFile: selectedTextFile.value,
            textContent: textContent.value
        });
    }
    catch (error) {
        errorMessage.value = error instanceof Error ? error.message : "Processing failed";
    }
    finally {
        loading.value = false;
    }
}
function insertIntoTextContent(rawText) {
    const normalized = rawText.trim();
    if (!normalized) {
        return;
    }
    const textArea = textAreaRef.value;
    const insertion = textContent.value.trim().length > 0 ? `\n\n${normalized}` : normalized;
    if (!textArea) {
        textContent.value = `${textContent.value}${insertion}`;
        return;
    }
    const { nextValue, nextCaretPosition } = insertAtCaret(textArea, textContent.value, insertion);
    textContent.value = nextValue;
    requestAnimationFrame(() => {
        textArea.focus();
        textArea.setSelectionRange(nextCaretPosition, nextCaretPosition);
    });
}
function onInsertVideoMarkdown(markdown) {
    insertIntoTextContent(markdown);
}
function onInsertVideoTextContent(text) {
    insertIntoTextContent(text);
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['page-home']} */ ;
/** @type {__VLS_StyleScopedClasses['page-home']} */ ;
/** @type {__VLS_StyleScopedClasses['page-home']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-card']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.main, __VLS_intrinsicElements.main)({
    ...{ class: "page-home" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h1, __VLS_intrinsicElements.h1)({
    ...{ class: "cyber-display cyber-title-glitch" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "cyber-muted page-lead" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "cyber-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "cyber-field-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ onChange: (__VLS_ctx.onSelectAudioFile) },
    ...{ class: "cyber-field" },
    type: "file",
    accept: ".mp3,audio/mpeg",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "cyber-field-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ onChange: (__VLS_ctx.onSelectTextFile) },
    ...{ class: "cyber-field" },
    type: "file",
    accept: ".txt,.md,text/plain,text/markdown",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "cyber-field-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.textarea)({
    ref: "textAreaRef",
    value: (__VLS_ctx.textContent),
    ...{ class: "cyber-field" },
    rows: "6",
    placeholder: "Paste transcript, outline, or learning notes here...",
});
/** @type {typeof __VLS_ctx.textAreaRef} */ ;
/** @type {[typeof VideoLinkSection, ]} */ ;
// @ts-ignore
const __VLS_0 = __VLS_asFunctionalComponent(VideoLinkSection, new VideoLinkSection({
    ...{ 'onInsertMarkdown': {} },
    ...{ 'onInsertTextContent': {} },
}));
const __VLS_1 = __VLS_0({
    ...{ 'onInsertMarkdown': {} },
    ...{ 'onInsertTextContent': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_0));
let __VLS_3;
let __VLS_4;
let __VLS_5;
const __VLS_6 = {
    onInsertMarkdown: (__VLS_ctx.onInsertVideoMarkdown)
};
const __VLS_7 = {
    onInsertTextContent: (__VLS_ctx.onInsertVideoTextContent)
};
var __VLS_2;
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.onSubmit) },
    type: "button",
    ...{ class: "cyber-btn-primary" },
    disabled: (__VLS_ctx.loading),
});
(__VLS_ctx.loading ? "Processing..." : "Generate Notes");
if (__VLS_ctx.errorMessage) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "cyber-error" },
    });
    (__VLS_ctx.errorMessage);
}
if (__VLS_ctx.result) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
        ...{ class: "cyber-card" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({});
    (__VLS_ctx.result.title);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    (__VLS_ctx.result.sourceFilename);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    (__VLS_ctx.result.abstractText);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.a, __VLS_intrinsicElements.a)({
        ...{ class: "cyber-link cyber-download" },
        href: (`${__VLS_ctx.API_ORIGIN}${__VLS_ctx.result.downloadUrl}`),
        target: "_blank",
        rel: "noopener",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.pre, __VLS_intrinsicElements.pre)({
        ...{ class: "cyber-pre" },
    });
    (__VLS_ctx.result.markdownPreview);
}
/** @type {__VLS_StyleScopedClasses['page-home']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-display']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-title-glitch']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-muted']} */ ;
/** @type {__VLS_StyleScopedClasses['page-lead']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-card']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field-label']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field-label']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field-label']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-btn-primary']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-error']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-card']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-link']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-download']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-pre']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            API_ORIGIN: API_ORIGIN,
            VideoLinkSection: VideoLinkSection,
            textContent: textContent,
            loading: loading,
            errorMessage: errorMessage,
            result: result,
            textAreaRef: textAreaRef,
            onSelectAudioFile: onSelectAudioFile,
            onSelectTextFile: onSelectTextFile,
            onSubmit: onSubmit,
            onInsertVideoMarkdown: onInsertVideoMarkdown,
            onInsertVideoTextContent: onInsertVideoTextContent,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
