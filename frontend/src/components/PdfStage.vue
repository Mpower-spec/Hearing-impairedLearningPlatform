<template>
  <div ref="stageEl" class="pdf-stage">
    <VuePdfEmbed
      v-if="source"
      :source="source"
      :page="page"
      :width="renderWidth"
      @loaded="onLoaded"
    />
    <div v-else class="pdf-empty">{{ emptyText }}</div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import VuePdfEmbed from 'vue-pdf-embed'

const props = defineProps({
  source: { type: String, default: '' },
  page: { type: Number, default: 1 },
  emptyText: { type: String, default: 'PDF 加载中' }
})

const stageEl = ref(null)
const renderWidth = ref(960)
const availableWidth = ref(960)
const availableHeight = ref(540)
const pdfAspect = ref(16 / 9)
let loadedDocument = null
let resizeObserver = null

function measure() {
  if (!stageEl.value) return
  const style = getComputedStyle(stageEl.value)
  const horizontalPadding = (parseFloat(style.paddingLeft) || 0) + (parseFloat(style.paddingRight) || 0)
  const verticalPadding = (parseFloat(style.paddingTop) || 0) + (parseFloat(style.paddingBottom) || 0)
  availableWidth.value = Math.max(280, stageEl.value.clientWidth - horizontalPadding)
  availableHeight.value = Math.max(158, stageEl.value.clientHeight - verticalPadding)
  updateRenderWidth()
}

function updateRenderWidth() {
  const fittedWidth = Math.min(availableWidth.value, availableHeight.value * pdfAspect.value)
  renderWidth.value = Math.max(280, Math.floor(fittedWidth))
}

async function updateAspect(document) {
  if (!document) return
  try {
    const pdfPage = await document.getPage(props.page)
    const viewport = pdfPage.getViewport({ scale: 1 })
    pdfAspect.value = viewport.width / viewport.height
    updateRenderWidth()
  } catch {
    // Keep the 16:9 default if page metadata cannot be read.
  }
}

async function onLoaded(document) {
  loadedDocument = document
  await updateAspect(document)
  await nextTick()
  measure()
}

watch(
  () => props.page,
  async () => {
    await updateAspect(loadedDocument)
  }
)

watch(
  () => props.source,
  async () => {
    loadedDocument = null
    pdfAspect.value = 16 / 9
    await nextTick()
    measure()
  }
)

onMounted(async () => {
  await nextTick()
  measure()
  resizeObserver = new ResizeObserver(measure)
  if (stageEl.value) resizeObserver.observe(stageEl.value)
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
})
</script>