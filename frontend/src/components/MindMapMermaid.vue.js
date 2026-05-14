import mermaid from "mermaid";
import { onMounted, ref, watch } from "vue";
const props = defineProps();
const container = ref(null);
const renderErr = ref("");
let renderNonce = 0;
function escapeMindmapTopic(raw) {
    return raw.replace(/["#'()]/g, " ").trim() || "topic";
}
function buildMindmapSource(root) {
    const rootLabel = escapeMindmapTopic(root.topic);
    const lines = ["mindmap", `  root((${rootLabel}))`];
    function walk(node, depth) {
        const indent = "  ".repeat(depth);
        const lbl = escapeMindmapTopic(node.topic);
        lines.push(`${indent}${lbl}`);
        for (const c of node.children ?? []) {
            walk(c, depth + 1);
        }
    }
    for (const child of root.children ?? []) {
        walk(child, 2);
    }
    return lines.join("\n");
}
async function draw() {
    renderErr.value = "";
    const raw = props.mindMapJson?.trim() ?? "";
    if (!container.value || !raw) {
        if (container.value) {
            container.value.innerHTML = "";
        }
        return;
    }
    let root;
    try {
        root = JSON.parse(raw);
    }
    catch {
        renderErr.value = "Invalid mind map JSON";
        container.value.innerHTML = "";
        return;
    }
    const nonce = ++renderNonce;
    try {
        mermaid.initialize({ startOnLoad: false, theme: "dark", securityLevel: "strict" });
        const diagram = buildMindmapSource(root);
        const svg = await mermaid.render(`mindmap-svg-${nonce}`, diagram);
        if (nonce === renderNonce && container.value) {
            container.value.innerHTML = svg.svg;
        }
    }
    catch (e) {
        if (nonce === renderNonce) {
            renderErr.value = e instanceof Error ? e.message : "Mind map rendering failed.";
            container.value.innerHTML = "";
        }
    }
}
watch(() => props.mindMapJson, () => {
    void draw();
});
onMounted(() => {
    void draw();
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "mindmap-root" },
});
if (__VLS_ctx.renderErr) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "cyber-error" },
    });
    (__VLS_ctx.renderErr);
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div)({
    ref: "container",
    ...{ class: "mindmap-svg-host" },
});
/** @type {typeof __VLS_ctx.container} */ ;
/** @type {__VLS_StyleScopedClasses['mindmap-root']} */ ;
/** @type {__VLS_StyleScopedClasses['cyber-error']} */ ;
/** @type {__VLS_StyleScopedClasses['mindmap-svg-host']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            container: container,
            renderErr: renderErr,
        };
    },
    __typeProps: {},
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
    __typeProps: {},
});
; /* PartiallyEnd: #4569/main.vue */
