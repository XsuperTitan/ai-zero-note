<script setup lang="ts">
import mermaid from "mermaid";
import { onMounted, ref, watch } from "vue";

/** Matches backend SummaryService mindMap JSON shape */
export interface MindMapNode {
  topic: string;
  keywords?: string[];
  children?: MindMapNode[];
}

const props = defineProps<{
  mindMapJson: string | null | undefined;
}>();

const container = ref<HTMLDivElement | null>(null);
const renderErr = ref("");
let renderNonce = 0;

function escapeMindmapTopic(raw: string): string {
  return raw.replace(/["#'()]/g, " ").trim() || "topic";
}

function buildMindmapSource(root: MindMapNode): string {
  const rootLabel = escapeMindmapTopic(root.topic);
  const lines: string[] = ["mindmap", `  root((${rootLabel}))`];

  function walk(node: MindMapNode, depth: number) {
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

  let root: MindMapNode;
  try {
    root = JSON.parse(raw) as MindMapNode;
  } catch {
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
  } catch (e) {
    if (nonce === renderNonce) {
      renderErr.value = e instanceof Error ? e.message : "Mind map rendering failed.";
      container.value!.innerHTML = "";
    }
  }
}

watch(
  () => props.mindMapJson,
  () => {
    void draw();
  }
);

onMounted(() => {
  void draw();
});
</script>

<template>
  <div class="mindmap-root">
    <p v-if="renderErr" class="cyber-error">{{ renderErr }}</p>
    <div ref="container" class="mindmap-svg-host" />
  </div>
</template>

<style scoped>
.mindmap-root {
  margin-top: 0.75rem;
}
.mindmap-svg-host :deep(svg) {
  max-width: 100%;
  height: auto;
}
</style>
