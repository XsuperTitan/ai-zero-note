import { computed, ref } from "vue";
import { fetchVideoFrames, fetchVideoText, fetchVideoVisionText, resolveVideoAssetUrl } from "../api/video";
const emit = defineEmits();
const videoUrl = ref("");
const loadingMeta = ref(false);
const loadingFrames = ref(false);
const loadingVision = ref(false);
const errorMessage = ref("");
const videoMeta = ref(null);
const frames = ref([]);
const selectedFrameNames = ref(new Set());
const currentTaskId = ref("");
const currentFrameUrl = ref("");
const visionTargetLanguage = ref("auto");
const selectedFrames = computed(() => frames.value.filter((frame) => selectedFrameNames.value.has(frame.fileName)));
function clearError() {
    errorMessage.value = "";
}
function validateUrl() {
    const value = videoUrl.value.trim();
    if (!value) {
        throw new Error("请先输入视频链接。");
    }
    if (!value.startsWith("http://") && !value.startsWith("https://")) {
        throw new Error("视频链接必须以 http:// 或 https:// 开头。");
    }
    return value;
}
async function onParseMeta() {
    if (loadingMeta.value || loadingFrames.value || loadingVision.value) {
        return;
    }
    clearError();
    try {
        const url = validateUrl();
        loadingMeta.value = true;
        const result = await fetchVideoText(url);
        videoMeta.value = result.meta;
        emit("insert-text-content", result.textContent);
    }
    catch (error) {
        errorMessage.value = error instanceof Error ? error.message : "视频文本提取失败";
    }
    finally {
        loadingMeta.value = false;
    }
}
async function onGenerateFrames() {
    if (loadingFrames.value || loadingMeta.value || loadingVision.value) {
        return;
    }
    clearError();
    try {
        const url = validateUrl();
        loadingFrames.value = true;
        const result = await fetchVideoFrames({ url });
        videoMeta.value = result.meta;
        frames.value = result.frames;
        currentTaskId.value = result.taskId;
        currentFrameUrl.value = url;
        selectedFrameNames.value = new Set(result.frames.map((frame) => frame.fileName));
    }
    catch (error) {
        errorMessage.value = error instanceof Error ? error.message : "关键截图生成失败";
    }
    finally {
        loadingFrames.value = false;
    }
}
async function onGenerateVisionText() {
    if (loadingFrames.value || loadingMeta.value || loadingVision.value) {
        return;
    }
    if (selectedFrames.value.length === 0) {
        errorMessage.value = "请至少选择一张截图。";
        return;
    }
    if (!currentTaskId.value) {
        errorMessage.value = "请先生成关键截图。";
        return;
    }
    clearError();
    try {
        loadingVision.value = true;
        const result = await fetchVideoVisionText(currentFrameUrl.value, currentTaskId.value, selectedFrames.value.map((frame) => frame.fileName), visionTargetLanguage.value);
        emit("insert-text-content", result.textContent);
    }
    catch (error) {
        errorMessage.value = error instanceof Error ? error.message : "图生文提取失败";
    }
    finally {
        loadingVision.value = false;
    }
}
function toggleFrame(fileName) {
    const nextSet = new Set(selectedFrameNames.value);
    if (nextSet.has(fileName)) {
        nextSet.delete(fileName);
    }
    else {
        nextSet.add(fileName);
    }
    selectedFrameNames.value = nextSet;
}
function selectAllFrames() {
    selectedFrameNames.value = new Set(frames.value.map((frame) => frame.fileName));
}
function clearSelectedFrames() {
    selectedFrameNames.value = new Set();
}
function buildMarkdown(meta, chosenFrames) {
    const authorText = meta.uploader ? ` | 🎬 ${meta.uploader}` : "";
    const lines = [
        `> **视频标题**：[${meta.title}](${meta.sourceUrl}) | ⏱ 时长：${meta.durationText}${authorText}`
    ];
    chosenFrames.forEach((frame, index) => {
        lines.push(`![截图${index + 1}](${resolveVideoAssetUrl(frame.imageUrl)})`);
    });
    return `${lines.join("\n")}\n`;
}
function onInsertMarkdown() {
    if (!videoMeta.value) {
        errorMessage.value = "请先完成视频解析。";
        return;
    }
    if (selectedFrames.value.length === 0) {
        errorMessage.value = "请至少选择一张截图。";
        return;
    }
    clearError();
    emit("insert-markdown", buildMarkdown(videoMeta.value, selectedFrames.value));
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['meta']} */ ;
/** @type {__VLS_StyleScopedClasses['frame-item']} */ ;
/** @type {__VLS_StyleScopedClasses['frame-item']} */ ;
/** @type {__VLS_StyleScopedClasses['frame-item']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "video-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "field-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    type: "url",
    placeholder: "https://www.bilibili.com/... 或 https://www.youtube.com/...",
});
(__VLS_ctx.videoUrl);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "actions" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.onParseMeta) },
    type: "button",
    disabled: (__VLS_ctx.loadingMeta || __VLS_ctx.loadingFrames || __VLS_ctx.loadingVision),
});
(__VLS_ctx.loadingMeta ? "提取中..." : "解析视频信息并注入Text content");
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.onGenerateFrames) },
    type: "button",
    disabled: (__VLS_ctx.loadingMeta || __VLS_ctx.loadingFrames || __VLS_ctx.loadingVision),
});
(__VLS_ctx.loadingFrames ? "生成中..." : "生成关键截图");
if (__VLS_ctx.videoMeta) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "meta" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    (__VLS_ctx.videoMeta.title);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    (__VLS_ctx.videoMeta.durationText);
    if (__VLS_ctx.videoMeta.uploader) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({});
        __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
        (__VLS_ctx.videoMeta.uploader);
    }
}
if (__VLS_ctx.frames.length > 0) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "grid-toolbar" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.selectAllFrames) },
        type: "button",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.clearSelectedFrames) },
        type: "button",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
        ...{ class: "language-label" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.select, __VLS_intrinsicElements.select)({
        value: (__VLS_ctx.visionTargetLanguage),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        value: "auto",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        value: "zh",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.option, __VLS_intrinsicElements.option)({
        value: "en",
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (__VLS_ctx.selectedFrames.length);
    (__VLS_ctx.frames.length);
}
if (__VLS_ctx.frames.length > 0) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "frame-grid" },
    });
    for (const [frame] of __VLS_getVForSourceType((__VLS_ctx.frames))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
            key: (frame.fileName),
            ...{ class: "frame-item" },
            ...{ class: ({ selected: __VLS_ctx.selectedFrameNames.has(frame.fileName) }) },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
            ...{ onChange: (...[$event]) => {
                    if (!(__VLS_ctx.frames.length > 0))
                        return;
                    __VLS_ctx.toggleFrame(frame.fileName);
                } },
            type: "checkbox",
            checked: (__VLS_ctx.selectedFrameNames.has(frame.fileName)),
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.img)({
            src: (__VLS_ctx.resolveVideoAssetUrl(frame.imageUrl)),
            alt: (frame.fileName),
            loading: "lazy",
        });
    }
}
if (__VLS_ctx.frames.length > 0) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "insert-actions" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.onInsertMarkdown) },
        type: "button",
        ...{ class: "insert-btn" },
        disabled: (__VLS_ctx.selectedFrames.length === 0 || __VLS_ctx.loadingVision),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (__VLS_ctx.onGenerateVisionText) },
        type: "button",
        ...{ class: "insert-btn" },
        disabled: (__VLS_ctx.selectedFrames.length === 0 || __VLS_ctx.loadingVision),
    });
    (__VLS_ctx.loadingVision ? "图生文提取中..." : "图生文提取并注入Text content");
}
if (__VLS_ctx.errorMessage) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "error" },
    });
    (__VLS_ctx.errorMessage);
}
/** @type {__VLS_StyleScopedClasses['video-card']} */ ;
/** @type {__VLS_StyleScopedClasses['field-label']} */ ;
/** @type {__VLS_StyleScopedClasses['actions']} */ ;
/** @type {__VLS_StyleScopedClasses['meta']} */ ;
/** @type {__VLS_StyleScopedClasses['grid-toolbar']} */ ;
/** @type {__VLS_StyleScopedClasses['language-label']} */ ;
/** @type {__VLS_StyleScopedClasses['frame-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['frame-item']} */ ;
/** @type {__VLS_StyleScopedClasses['insert-actions']} */ ;
/** @type {__VLS_StyleScopedClasses['insert-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['insert-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['error']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            resolveVideoAssetUrl: resolveVideoAssetUrl,
            videoUrl: videoUrl,
            loadingMeta: loadingMeta,
            loadingFrames: loadingFrames,
            loadingVision: loadingVision,
            errorMessage: errorMessage,
            videoMeta: videoMeta,
            frames: frames,
            selectedFrameNames: selectedFrameNames,
            visionTargetLanguage: visionTargetLanguage,
            selectedFrames: selectedFrames,
            onParseMeta: onParseMeta,
            onGenerateFrames: onGenerateFrames,
            onGenerateVisionText: onGenerateVisionText,
            toggleFrame: toggleFrame,
            selectAllFrames: selectAllFrames,
            clearSelectedFrames: clearSelectedFrames,
            onInsertMarkdown: onInsertMarkdown,
        };
    },
    __typeEmits: {},
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
    __typeEmits: {},
});
; /* PartiallyEnd: #4569/main.vue */
