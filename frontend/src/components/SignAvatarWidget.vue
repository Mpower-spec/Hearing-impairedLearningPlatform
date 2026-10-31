<template>
  <div
    ref="hostRef"
    class="sign-avatar-runtime"
    :class="{
      'sign-avatar-runtime--embedded': embedded,
      'sign-avatar-runtime--floating': !embedded,
      'sign-avatar-runtime--visible': avatarVisible
    }"
    :aria-hidden="embedded ? 'true' : undefined"
  >
    <button
      v-if="!embedded"
      ref="dragHandleRef"
      class="sign-avatar-drag-handle"
      :class="{ 'is-dragging': dragging }"
      type="button"
      aria-label="拖动数字人位置"
      title="拖动数字人，方向键也可移动"
      @pointerdown="startFloatingDrag"
      @pointermove="moveFloatingDrag"
      @pointerup="endFloatingDrag"
      @pointercancel="endFloatingDrag"
      @lostpointercapture="endFloatingDrag"
      @keydown="moveFloatingAvatarWithKeyboard"
    >
      <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
        <circle cx="7" cy="5" r="1.2" fill="currentColor" />
        <circle cx="13" cy="5" r="1.2" fill="currentColor" />
        <circle cx="7" cy="10" r="1.2" fill="currentColor" />
        <circle cx="13" cy="10" r="1.2" fill="currentColor" />
        <circle cx="7" cy="15" r="1.2" fill="currentColor" />
        <circle cx="13" cy="15" r="1.2" fill="currentColor" />
      </svg>
    </button>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  initialText: { type: String, default: '欢迎使用，祝你学习顺利' },
  signText: { type: String, default: '' },
  autoPlay: { type: Boolean, default: true },
  embedded: { type: Boolean, default: false },
  avatarSize: { type: Number, default: 4 }
})
const emit = defineEmits(['ready', 'error', 'visibility-change', 'state-change'])

const SDK_URL = 'https://avatar.gbqr.net/yiyu.js'
const AVATAR_ELEMENT_ID = 'yiyuAppElement'
const WELCOME_FALLBACK_MS = 6000
const CAPTION_FALLBACK_MS = 10000
const SDK_READY_TIMEOUT_MS = 45000
const runtime = (window.__hpSignAvatarRuntime ||= {})
const runtimeDefaults = {
  sdkPromise: null,
  initPromise: null,
  app: null,
  ready: false,
  htmlReady: false,
  element: null,
  owner: null,
  callbacksBound: false,
  resolveInit: null,
  onSentenceFinished: null
}
for (const [key, value] of Object.entries(runtimeDefaults)) {
  if (runtime[key] === undefined) runtime[key] = value
}
const widgetToken = Symbol('sign-avatar-widget')
let disposed = false
let readyTimer = null
let welcomeFallbackTimer = null
let welcomeSegments = []
let welcomeIndex = 0
let welcomeStarted = false
let lastAdvanceAt = 0
let initialPositionApplied = false
let captionFallbackTimer = null
let activePlaybackType = null
let pendingCaptionText = ''
let captionSessionStarted = false
let lastCaptionId = null
let lastCaptionText = ''
let captionPendingAfterReady = ''
let resizeObserver = null
let dragState = null
let floatingPosition = null
const hostRef = ref(null)
const dragHandleRef = ref(null)
const status = ref('loading')
const errorMessage = ref('')
const hidden = ref(false)
const playing = ref(false)
const dragging = ref(false)
const welcomeText = computed(() => props.signText || props.initialText || '欢迎使用，祝你学习顺利')
const avatarVisible = computed(() => status.value === 'ready' && !hidden.value)

watch([status, hidden, playing, errorMessage], () => {
  emit('state-change', {
    ready: status.value === 'ready',
    hidden: hidden.value,
    playing: playing.value,
    hasError: status.value === 'error',
    error: errorMessage.value
  })
}, { immediate: true })

async function loadSdk() {
  if (window.yiyu?.app) return window.yiyu.app
  if (!runtime.sdkPromise) {
    runtime.sdkPromise = import(/* @vite-ignore */ SDK_URL)
      .then((module) => {
        const app = module?.yiyu || window.yiyu?.app
        if (!app) throw new Error('SDK 未暴露 yiyu.app')
        return app
      })
      .catch((error) => {
        runtime.sdkPromise = null
        throw error
      })
  }
  return runtime.sdkPromise
}

function getAvatarElements() {
  return Array.from(document.querySelectorAll(`#${AVATAR_ELEMENT_ID}`))
}

function removeDuplicateAvatarElements(keep = null) {
  const elements = getAvatarElements()
  // The SDK's internal pointer follows the newest container. Keep that node
  // and remove older nodes left behind by an interrupted initialization.
  const active = keep && elements.includes(keep) ? keep : elements.at(-1) || null
  for (const element of elements) {
    if (element === active) continue
    element.style.display = 'none'
    element.style.pointerEvents = 'none'
    element.remove()
  }
  if (active) {
    runtime.element = active
  } else if (runtime.element && !runtime.element.isConnected) {
    runtime.element = null
  }
  return active
}

function removeAllAvatarElements() {
  for (const element of getAvatarElements()) {
    element.style.display = 'none'
    element.style.pointerEvents = 'none'
    element.remove()
  }
  runtime.element = null
}

function getAvatarElement() {
  return removeDuplicateAvatarElements()
}

function supportsWebGl() {
  try {
    const canvas = document.createElement('canvas')
    return Boolean(canvas.getContext('webgl2') || canvas.getContext('webgl'))
  } catch {
    return false
  }
}

function layoutAvatarElement(element) {
  const host = hostRef.value
  if (!host || !element) return false
  try {
    if (element.parentElement !== host) host.appendChild(element)
    Object.assign(element.style, {
      display: 'block',
      position: 'absolute',
      top: '0',
      left: '0',
      right: 'auto',
      bottom: 'auto',
      width: '100%',
      height: '100%',
      zIndex: '1',
      cursor: 'default',
      userSelect: 'none',
      opacity: hidden.value ? '0' : '1',
      pointerEvents: props.embedded || hidden.value ? 'none' : 'auto'
    })
    return true
  } catch (error) {
    console.warn('[SignAvatar] 数字人容器布局失败', error)
    return false
  }
}

function positionEmbeddedAvatar() {
  const host = hostRef.value
  const element = getAvatarElement()
  if (!host || !element) return
  try {
    layoutAvatarElement(element)
    runtime.app?.setSize('100%', '100%')
    runtime.app?.setPosition('0px', '0px')
    initialPositionApplied = true
  } catch (error) {
    console.warn('[SignAvatar] 数字人嵌入定位失败', error)
  }
}

function getFloatingSize() {
  return {
    width: Math.max(1, Math.min(340, window.innerWidth - 24)),
    height: Math.max(1, Math.min(400, window.innerHeight - 24))
  }
}

function clampFloatingPosition(left, top, width, height) {
  const margin = 12
  const maxLeft = Math.max(margin, window.innerWidth - width - margin)
  const maxTop = Math.max(margin, window.innerHeight - height - margin)
  return {
    left: Math.min(Math.max(margin, left), maxLeft),
    top: Math.min(Math.max(margin, top), maxTop)
  }
}

function applyFloatingPosition(resetPosition = false) {
  const host = hostRef.value
  if (!host) return
  const { width, height } = getFloatingSize()
  if (resetPosition || !floatingPosition) {
    floatingPosition = {
      left: window.innerWidth - width - 24,
      top: Math.max(84, Math.min(112, window.innerHeight - height - 8))
    }
  }
  floatingPosition = clampFloatingPosition(floatingPosition.left, floatingPosition.top, width, height)
  Object.assign(host.style, {
    width: `${width}px`,
    height: `${height}px`,
    top: `${floatingPosition.top}px`,
    left: `${floatingPosition.left}px`,
    right: 'auto',
    bottom: 'auto'
  })
}

function positionFloatingAvatar(resetPosition = false) {
  const host = hostRef.value
  const element = getAvatarElement()
  if (!host) return
  applyFloatingPosition(resetPosition)
  if (!element) return
  try {
    layoutAvatarElement(element)
    runtime.app?.setSize('100%', '100%')
    runtime.app?.setPosition('0px', '0px')
    initialPositionApplied = true
  } catch (error) {
    console.warn('[SignAvatar] 数字人浮动定位失败', error)
  }
}

function syncCurrentAvatarLayout(resetPosition = false) {
  if (props.embedded) positionEmbeddedAvatar()
  else positionFloatingAvatar(resetPosition)
}

function positionAvatarInitially() {
  if (initialPositionApplied) return
  syncCurrentAvatarLayout(true)
}

function startFloatingDrag(event) {
  if (props.embedded || (event.pointerType === 'mouse' && event.button !== 0)) return
  const host = hostRef.value
  if (!host) return
  event.preventDefault()
  const rect = host.getBoundingClientRect()
  dragState = {
    pointerId: event.pointerId,
    startX: event.clientX,
    startY: event.clientY,
    startLeft: rect.left,
    startTop: rect.top
  }
  dragging.value = true
  try { dragHandleRef.value?.setPointerCapture(event.pointerId) } catch { /* optional */ }
}

function moveFloatingDrag(event) {
  if (!dragState || event.pointerId !== dragState.pointerId) return
  event.preventDefault()
  const { width, height } = getFloatingSize()
  floatingPosition = clampFloatingPosition(
    dragState.startLeft + event.clientX - dragState.startX,
    dragState.startTop + event.clientY - dragState.startY,
    width,
    height
  )
  applyFloatingPosition(false)
}

function endFloatingDrag(event) {
  if (!dragState) return
  if (event?.pointerId !== undefined && event.pointerId !== dragState.pointerId) return
  try {
    if (dragHandleRef.value?.hasPointerCapture(dragState.pointerId)) {
      dragHandleRef.value.releasePointerCapture(dragState.pointerId)
    }
  } catch { /* optional */ }
  dragState = null
  dragging.value = false
}

function moveFloatingAvatarWithKeyboard(event) {
  const directions = {
    ArrowUp: [0, -1],
    ArrowDown: [0, 1],
    ArrowLeft: [-1, 0],
    ArrowRight: [1, 0]
  }
  const direction = directions[event.key]
  if (!direction || props.embedded) return
  event.preventDefault()
  const { width, height } = getFloatingSize()
  const host = hostRef.value
  const rect = host?.getBoundingClientRect()
  const step = event.shiftKey ? 24 : 8
  const current = floatingPosition || { left: rect?.left || 0, top: rect?.top || 0 }
  floatingPosition = clampFloatingPosition(
    current.left + direction[0] * step,
    current.top + direction[1] * step,
    width,
    height
  )
  applyFloatingPosition(false)
}

function handleViewportResize() {
  if (!initialPositionApplied) return
  syncCurrentAvatarLayout(false)
}

function showAvatar() {
  hidden.value = false
  if (runtime.app && runtime.ready) {
    try { runtime.app.enableYiyuApp() } catch { /* SDK may not be ready yet */ }
  }
  const host = hostRef.value
  if (host) host.style.display = 'block'
  const element = getAvatarElement()
  if (element) layoutAvatarElement(element)
  emit('visibility-change', true)
}

function hideAvatar() {
  hidden.value = true
  stopWelcome()
  stopCaption(true)
  if (runtime.app) {
    try { runtime.app.disableYiyuApp() } catch { /* ignored */ }
  }
  const host = hostRef.value
  if (host) host.style.display = 'none'
  const element = getAvatarElement()
  if (element) {
    element.style.opacity = '0'
    element.style.pointerEvents = 'none'
    element.style.display = 'none'
  }
  emit('visibility-change', false)
}

function splitSignSegments(text) {
  return String(text || '')
    .split(/[，,。.!?！？；;]/)
    .map((part) => part.trim())
    .filter(Boolean)
}

function clearWelcomeFallback() {
  clearTimeout(welcomeFallbackTimer)
  welcomeFallbackTimer = null
}

function scheduleWelcomeFallback() {
  clearWelcomeFallback()
  welcomeFallbackTimer = window.setTimeout(() => {
    if (playing.value && activePlaybackType === 'welcome') advanceWelcome()
  }, WELCOME_FALLBACK_MS)
}

function advanceWelcome() {
  if (!playing.value || !runtime.app || activePlaybackType !== 'welcome') return
  clearWelcomeFallback()
  if (welcomeIndex >= welcomeSegments.length) {
    playing.value = false
    activePlaybackType = null
    return
  }
  const next = welcomeSegments[welcomeIndex]
  welcomeIndex += 1
  try {
    runtime.app.appendTranslate(next)
    lastAdvanceAt = Date.now()
    scheduleWelcomeFallback()
  } catch (error) {
    playing.value = false
    activePlaybackType = null
    clearWelcomeFallback()
    errorMessage.value = `欢迎手语播放失败：${error.message}`
    status.value = 'error'
    emit('error', error)
  }
}

function handleSentenceFinished() {
  const now = Date.now()
  if (now - lastAdvanceAt < 800) return
  lastAdvanceAt = now
  if (activePlaybackType === 'welcome') advanceWelcome()
  else if (activePlaybackType === 'caption') advanceCaption()
}

function stopWelcome() {
  if (activePlaybackType === 'welcome') {
    playing.value = false
    activePlaybackType = null
  }
  welcomeSegments = []
  welcomeIndex = 0
  lastAdvanceAt = 0
  clearWelcomeFallback()
  if (runtime.app) {
    try { runtime.app.cancelTranslation() } catch { /* ignored */ }
  }
}

function clearCaptionFallback() {
  clearTimeout(captionFallbackTimer)
  captionFallbackTimer = null
}

function scheduleCaptionFallback() {
  clearCaptionFallback()
  captionFallbackTimer = window.setTimeout(() => {
    if (playing.value && activePlaybackType === 'caption') advanceCaption()
  }, CAPTION_FALLBACK_MS)
}

function playCaptionSegment(text) {
  if (!text || hidden.value) return
  if (!runtime.app || status.value !== 'ready') return
  try {
    if (!captionSessionStarted) {
      runtime.app.cancelTranslation()
      runtime.app.startTranslate(text)
      captionSessionStarted = true
    } else {
      runtime.app.appendTranslate(text)
    }
    activePlaybackType = 'caption'
    playing.value = true
    lastAdvanceAt = Date.now()
    scheduleCaptionFallback()
  } catch (error) {
    playing.value = false
    activePlaybackType = null
    captionSessionStarted = false
    clearCaptionFallback()
    errorMessage.value = `课堂手语播放失败：${error.message}`
    status.value = 'error'
    emit('error', error)
  }
}

function advanceCaption() {
  if (!playing.value || activePlaybackType !== 'caption') return
  clearCaptionFallback()
  if (pendingCaptionText) {
    const next = pendingCaptionText
    pendingCaptionText = ''
    playCaptionSegment(next)
    return
  }
  playing.value = false
  activePlaybackType = null
}

function stopCaption(preserveReplay = true) {
  clearCaptionFallback()
  if (activePlaybackType === 'caption') {
    playing.value = false
    activePlaybackType = null
    captionSessionStarted = false
    if (runtime.app) {
      try { runtime.app.cancelTranslation() } catch { /* ignored */ }
    }
  }
  pendingCaptionText = ''
  captionPendingAfterReady = ''
  if (!preserveReplay) {
    lastCaptionId = null
    lastCaptionText = ''
  }
}

function playCaptionText(text) {
  const segments = splitSignSegments(text)
  if (!segments.length) return
  stopWelcome()
  stopCaption(true)
  lastCaptionText = text
  pendingCaptionText = segments.length > 1 ? segments.at(-1) : ''
  playCaptionSegment(segments[0])
}

function pushCaption(text, captionId) {
  if (hidden.value) return
  const normalized = String(text || '').trim()
  if (!normalized) return
  const id = captionId == null ? normalized : String(captionId)
  if (lastCaptionId === id) return
  lastCaptionId = id
  lastCaptionText = normalized
  if (!runtime.app || status.value !== 'ready') {
    captionPendingAfterReady = normalized
    return
  }
  const segments = splitSignSegments(normalized)
  if (!segments.length) return
  if (playing.value && activePlaybackType === 'welcome') stopWelcome()
  if (playing.value && activePlaybackType === 'caption') {
    pendingCaptionText = segments.at(-1)
    return
  }
  pendingCaptionText = segments.length > 1 ? segments.at(-1) : ''
  playCaptionSegment(segments[0])
}

function clearCaptions() {
  stopCaption(false)
  lastCaptionId = null
  lastCaptionText = ''
}

function replay() {
  if (status.value !== 'ready' || hidden.value) return
  if (lastCaptionText) playCaptionText(lastCaptionText)
  else playWelcome()
}

function playWelcome() {
  if (!runtime.app || status.value !== 'ready' || hidden.value) return
  const segments = splitSignSegments(welcomeText.value)
  if (!segments.length) return
  stopCaption(true)
  stopWelcome()
  captionSessionStarted = false
  activePlaybackType = 'welcome'
  welcomeSegments = segments
  welcomeIndex = 1
  playing.value = true
  lastAdvanceAt = Date.now()
  try {
    runtime.app.startTranslate(segments[0])
    scheduleWelcomeFallback()
  } catch (error) {
    playing.value = false
    clearWelcomeFallback()
    errorMessage.value = `欢迎手语播放失败：${error.message}`
    status.value = 'error'
    emit('error', error)
  }
}

function resolveSdkInitialization() {
  const resolve = runtime.resolveInit
  runtime.resolveInit = null
  resolve?.()
}

function bindSdkCallbacks(app) {
  if (runtime.callbacksBound) return
  runtime.callbacksBound = true
  app.onHtmlReady(() => {
    runtime.htmlReady = true
    const element = getAvatarElement()
    if (element && !runtime.owner) {
      element.style.display = 'none'
      element.style.pointerEvents = 'none'
    }
    console.info('[SignAvatar] 数字人界面资源已加载，等待动作服务就绪')
  })
  app.onAppReady(() => {
    runtime.ready = true
    if (!runtime.owner) {
      const element = getAvatarElement()
      if (element) {
        element.style.display = 'none'
        element.style.pointerEvents = 'none'
      }
    }
    console.info('[SignAvatar] 数字人动作服务已就绪')
    resolveSdkInitialization()
  })
  app.onSentenceFinished(() => runtime.onSentenceFinished?.())
}

function ensureSdkInitialized() {
  if (runtime.ready && runtime.app) return Promise.resolve(runtime.app)
  if (runtime.initPromise) return runtime.initPromise
  runtime.initPromise = (async () => {
    const app = runtime.app || await loadSdk()
    runtime.app = app
    bindSdkCallbacks(app)
    removeAllAvatarElements()
    await new Promise((resolve, reject) => {
      runtime.resolveInit = resolve
      try {
        app.init({
          name: import.meta.env.VITE_SIGN_AVATAR_ACCOUNT,
          showToast: false,
          // The SDK drag handler covers its whole transparent container.
          // Use our 44px handle instead so normal page clicks stay available.
          draggable: false,
          remotePath: true,
          readLocalResource: false
        })
      } catch (error) {
        reject(error)
      }
    })
    runtime.ready = true
    return app
  })().catch((error) => {
    runtime.initPromise = null
    runtime.ready = false
    runtime.resolveInit = null
    removeAllAvatarElements()
    throw error
  })
  return runtime.initPromise
}

function configureReadyApp() {
  runtime.owner = widgetToken
  runtime.onSentenceFinished = handleSentenceFinished
  status.value = 'ready'
  runtime.ready = true
  errorMessage.value = ''
  try { runtime.app.setIsShowTextarea(false) } catch { /* optional SDK UI */ }
  try { runtime.app.setAvatarSize(props.avatarSize) } catch { /* optional SDK sizing */ }
  positionAvatarInitially()
  emit('ready')
  if (captionPendingAfterReady) {
    const pending = captionPendingAfterReady
    captionPendingAfterReady = ''
    window.setTimeout(() => playCaptionText(pending), 300)
  } else if (props.autoPlay && !welcomeStarted) {
    welcomeStarted = true
    window.setTimeout(() => playWelcome(), 300)
  }
}

async function initializeAvatar() {
  disposed = false
  welcomeStarted = false
  clearTimeout(readyTimer)
  status.value = 'loading'
  errorMessage.value = ''
  initialPositionApplied = false
  if (!supportsWebGl()) {
    status.value = 'error'
    errorMessage.value = '当前浏览器未启用 WebGL，请开启硬件加速或更换 Chrome/Edge'
    emit('error', new Error(errorMessage.value))
    return
  }
  if (runtime.ready && runtime.app) {
    configureReadyApp()
    showAvatar()
    return
  }
  const account = import.meta.env.VITE_SIGN_AVATAR_ACCOUNT
  if (!account) {
    status.value = 'error'
    errorMessage.value = '未配置 VITE_SIGN_AVATAR_ACCOUNT'
    emit('error', new Error(errorMessage.value))
    return
  }
  readyTimer = window.setTimeout(() => {
    readyTimer = null
    if (disposed || runtime.ready) return
    status.value = 'error'
    errorMessage.value = runtime.htmlReady
      ? '数字人资源已加载，但动作服务未在 45 秒内就绪'
      : '数字人资源加载超时'
    console.warn('[SignAvatar] 初始化超时', { htmlReady: runtime.htmlReady })
    emit('error', new Error(errorMessage.value))
  }, SDK_READY_TIMEOUT_MS)
  try {
    console.info('[SignAvatar] SDK 脚本已加载，开始初始化数字人')
    await ensureSdkInitialized()
    if (disposed) return
    configureReadyApp()
    showAvatar()
  } catch (error) {
    if (disposed) return
    status.value = 'error'
    errorMessage.value = error?.message || '数字人加载失败'
    console.error('[SignAvatar] 初始化失败', error)
    emit('error', error)
  } finally {
    clearTimeout(readyTimer)
    readyTimer = null
  }
}

function retry() {
  if (status.value === 'error') initializeAvatar()
}

defineExpose({ replay, pushCaption, clearCaptions, showAvatar, hideAvatar, retry })

onMounted(() => {
  if (hostRef.value && typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => syncCurrentAvatarLayout(false))
    resizeObserver.observe(hostRef.value)
  }
  window.addEventListener('resize', handleViewportResize)
  initializeAvatar()
})
onBeforeUnmount(() => {
  disposed = true
  stopWelcome()
  stopCaption(false)
  clearTimeout(readyTimer)
  readyTimer = null
  resizeObserver?.disconnect()
  resizeObserver = null
  window.removeEventListener('resize', handleViewportResize)
  endFloatingDrag()
  if (runtime.owner !== widgetToken) return
  runtime.owner = null
  if (runtime.onSentenceFinished === handleSentenceFinished) runtime.onSentenceFinished = null
  try { runtime.app?.disableYiyuApp() } catch { /* ignored */ }
  const element = runtime.element?.isConnected ? runtime.element : null
  if (element) {
    element.style.opacity = '0'
    element.style.pointerEvents = 'none'
    element.style.display = 'none'
    // Keep the SDK singleton alive when this Vue host is removed. The next
    // route adopts it back into its own host before showing it again.
    document.body.appendChild(element)
  }
  if (hostRef.value) hostRef.value.style.display = 'none'
})
</script>

<style scoped>
.sign-avatar-runtime { display: none; }
.sign-avatar-runtime--embedded {
  position: absolute;
  inset: 0;
  z-index: 1;
  width: 100%;
  height: 100%;
  overflow: hidden;
  pointer-events: none;
}
.sign-avatar-runtime--floating {
  position: fixed !important;
  z-index: 900 !important;
  max-width: calc(100vw - 24px);
  max-height: calc(100vh - 24px);
  overflow: hidden;
  pointer-events: none;
}
.sign-avatar-runtime--floating.sign-avatar-runtime--visible { display: block !important; }
.sign-avatar-runtime :deep(#yiyuAppElement) {
  position: absolute !important;
  top: 0 !important;
  left: 0 !important;
  right: auto !important;
  bottom: auto !important;
  width: 100% !important;
  height: 100% !important;
  max-width: 100% !important;
  max-height: 100% !important;
  cursor: default !important;
}
.sign-avatar-drag-handle {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 3;
  width: 44px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 1px solid rgba(255, 255, 255, 0.26);
  border-radius: 12px;
  color: #e8fbf5;
  background: rgba(9, 40, 43, 0.78);
  box-shadow: 0 6px 18px rgba(4, 24, 27, 0.28);
  backdrop-filter: blur(8px);
  cursor: grab;
  pointer-events: auto;
  touch-action: none;
}
.sign-avatar-drag-handle:hover { background: rgba(18, 69, 74, 0.9); }
.sign-avatar-drag-handle:focus-visible { outline: 3px solid #8ed8c4; outline-offset: 2px; }
.sign-avatar-drag-handle.is-dragging { cursor: grabbing; }
.sign-avatar-drag-handle svg { width: 20px; height: 20px; }
</style>
