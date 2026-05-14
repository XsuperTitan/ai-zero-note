import { computed, onUnmounted, ref } from "vue";
import { API_ORIGIN } from "../api/client";
import { enqueueImageNoteJob, getImageNoteJob } from "../api/imageNote";
import { processMixedInput } from "../api/note";
import MindMapMermaid from "../components/MindMapMermaid.vue";
import VideoLinkSection from "../components/VideoLinkSection.vue";
import { insertAtCaret } from "../utils/insertAtCaret";
const selectedAudioFile = ref(null);
const selectedTextFile = ref(null);
const textContent = ref("");
const noteStyle = ref("LEARNING");
const outputLanguage = ref("AUTO");
const loading = ref(false);
const errorMessage = ref("");
const result = ref(null);
const textAreaRef = ref(null);
const showLanguageSelect = computed(() => noteStyle.value !== "LEARNING");
let imagePollHandle = null;
const imageJobLoading = ref(false);
const imageJobError = ref("");
const imageJobStatus = ref(null);
const imageJobId = ref(null);
const imageDownloadUrl = ref(null);
const showMindMapPanel = computed(() => Boolean(result.value?.mindMapJson && result.value.mindMapJson.trim().length > 0));
function composedSourceForImage() {
    if (textContent.value.trim()) {
        return textContent.value.trim();
    }
    if (result.value?.markdownPreview?.trim()) {
        return result.value.markdownPreview.trim();
    }
    return "";
}
function resetImagePolling() {
    if (imagePollHandle) {
        clearInterval(imagePollHandle);
        imagePollHandle = null;
    }
}
async function pollImageOnce(jobId) {
    try {
        const st = await getImageNoteJob(jobId);
        imageJobStatus.value = st.status;
        if (st.status === "SUCCEEDED") {
            resetImagePolling();
            imageDownloadUrl.value = st.downloadUrl ? `${API_ORIGIN}${st.downloadUrl}` : "";
            imageJobLoading.value = false;
        }
        else if (st.status === "FAILED") {
            resetImagePolling();
            imageJobError.value = st.errorMessage || "Image generation failed.";
            imageJobLoading.value = false;
        }
    }
    catch (e) {
        resetImagePolling();
        imageJobError.value = e instanceof Error ? e.message : "Image job polling failed";
        imageJobLoading.value = false;
    }
}
async function onStartImageNote() {
    const src = composedSourceForImage();
    if (!src) {
        imageJobError.value = "Add text content or generate notes before requesting a picture note.";
        return;
    }
    if (imageJobLoading.value) {
        return;
    }
    resetImagePolling();
    imageJobError.value = "";
    imageJobLoading.value = true;
    imageJobStatus.value = null;
    imageJobId.value = null;
    imageDownloadUrl.value = null;
    try {
        const queued = await enqueueImageNoteJob(src);
        imageJobId.value = queued.jobId;
        imageJobStatus.value = queued.status;
        imagePollHandle = setInterval(() => {
            void pollImageOnce(queued.jobId);
        }, 2200);
        void pollImageOnce(queued.jobId);
    }
    catch (e) {
        imageJobError.value = e instanceof Error ? e.message : "Failed to enqueue image job";
        imageJobLoading.value = false;
    }
}
onUnmounted(() => resetImagePolling());
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
            textContent: textContent.value,
            noteStyle: noteStyle.value,
            outputLanguage: outputLanguage.value
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
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "cyber-field-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
    value: (__VLS_ctx.noteStyle),
    ...{ class: "cyber-field" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: "LEARNING",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: "DETAILED",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
    value: "MIND_MAP",
});
if (__VLS_ctx.showLanguageSelect) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
        ...{ class: "cyber-field-label" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
        value: (__VLS_ctx.outputLanguage),
        ...{ class: "cyber-field" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        value: "AUTO",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        value: "ZH",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        value: "EN",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        value: "BILINGUAL",
    });
}
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
    if (__VLS_ctx.showMindMapPanel) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({
            ...{ class: "section-mini-title" },
        });
        /** @type {[typeof MindMapMermaid, ]} */ ;
        // @ts-ignore
        const __VLS_8 = __VLS_asFunctionalComponent(MindMapMermaid, new MindMapMermaid({
            mindMapJson: (__VLS_ctx.result.mindMapJson),
        }));
        const __VLS_9 = __VLS_8({
            mindMapJson: (__VLS_ctx.result.mindMapJson),
        }, ...__VLS_functionalComponentArgsRest(__VLS_8));
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({});
    (__VLS_ctx.result.title);
    if (__VLS_ctx.result.noteStyle) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
        (__VLS_ctx.result.noteStyle);
    }
    if (__VLS_ctx.result.outputLanguage && __VLS_ctx.result.noteStyle !== 'LEARNING') {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
        (__VLS_ctx.result.outputLanguage);
    }
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
    __VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({
        ...{ class: "section-mini-title" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "cyber-muted" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.code, __VLS_intrinsicElements.code)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.onStartImageNote) },
        type: "button",
        ...{ class: "cyber-btn-primary" },
        disabled: (__VLS_ctx.imageJobLoading),
    });
    (__VLS_ctx.imageJobLoading
        ? __VLS_ctx.imageJobStatus
            ? `Image job: ${__VLS_ctx.imageJobStatus}`
            : "Enqueueing..."
        : "Generate picture note");
    if (__VLS_ctx.imageJobId) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "cyber-muted" },
        });
        (__VLS_ctx.imageJobId);
    }
    if (__VLS_ctx.imageJobError) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "cyber-error" },
        });
        (__VLS_ctx.imageJobError);
    }
    if (__VLS_ctx.imageDownloadUrl) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.a, __VLS_intrinsicElements.a)({
            ...{ class: "cyber-link cyber-download" },
            href: (__VLS_ctx.imageDownloadUrl),
            target: "_blank",
            rel: "noopener",
        });
    }
    if (__VLS_ctx.imageDownloadUrl) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.img)({
            ...{ class: "image-note-preview" },
            src: (__VLS_ctx.imageDownloadUrl),
            alt: "Generated picture note",
        });
    }
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
/** @type {__VLS_StyleScopedClasses['cyber-field-label']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field-label']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-field']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-btn-primary']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-error']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-card']} */ ;
/** @type {__VLS_StyleScopedClasses['section-mini-title']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-link']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-download']} */ ;
/** @type {__VLS_StyleScopedClasses['section-mini-title']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-muted']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-btn-primary']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-muted']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-error']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-link']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-download']} */ ;
/** @type {__VLS_StyleScopedClasses['image-note-preview']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-pre']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            API_ORIGIN: API_ORIGIN,
            MindMapMermaid: MindMapMermaid,
            VideoLinkSection: VideoLinkSection,
            textContent: textContent,
            noteStyle: noteStyle,
            outputLanguage: outputLanguage,
            loading: loading,
            errorMessage: errorMessage,
            result: result,
            textAreaRef: textAreaRef,
            showLanguageSelect: showLanguageSelect,
            imageJobLoading: imageJobLoading,
            imageJobError: imageJobError,
            imageJobStatus: imageJobStatus,
            imageJobId: imageJobId,
            imageDownloadUrl: imageDownloadUrl,
            showMindMapPanel: showMindMapPanel,
            onStartImageNote: onStartImageNote,
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
