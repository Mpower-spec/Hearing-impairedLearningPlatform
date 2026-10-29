<template>
  <div ref="hostRef" class="sign-avatar-runtime" :class="{ 'sign-avatar-runtime--embedded': embedded }" aria-hidden="true"></div>
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
const WELCOME_FALLBACK_MS = 6000
const CAPTION_FALLBACK_MS = 10000
const SDK_READY_TIMEOUT_MS = 45000
const runtime = (window.__hpSignAvatarRuntime ||= {
  sdkPromise: null,
  app: null,
  ready: false,
  element: null
})
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
const hostRef = ref(null)
const status = ref('loading')
const errorMessage = ref('')
const hidden = ref(false)
const htmlReady = ref(false)
const playing = ref(false)
const welcomeText = computed(() => props.signText || props.initialText || '欢迎使用，祝你学习顺利')

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

function getAvatarElement() {
  if (runtime.element) return runtime.element
  runtime.element = document.getElementById('yiyuAppElement')
  return runtime.element
}

function supportsWebGl() {
  try {
    const canvas = document.createElement('canvas')
    return Boolean(canvas.getContext('webgl2') || canvas.getContext('webgl'))
  } catch {
    return false
  }
}

function positionEmbeddedAvatar() {
  const host = hostRef.value
  const element = getAvatarElement()
  if (!host || !element) return
  try {
    if (element.parentElement !== host) host.appendChild(element)
    runtime.app?.setSize('100%', '100%')
    runtime.app?.setPosition('0px', '0px')
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
      pointerEvents: 'none'
    })
    initialPositionApplied = true
  } catch (error) {
    console.warn('[SignAvatar] 数字人嵌入定位失败', error)
  }
}

function positionAvatarInitially() {
  if (initialPositionApplied) return
  const element = getAvatarElement()
  if (!element) return
  if (props.embedded) {
    positionEmbeddedAvatar()
    return
  }
  if (element.parentElement !== document.body) document.body.appendChild(element)
  const width = Math.min(340, Math.max(260, window.innerWidth - 24))
  const height = Math.min(400, Math.max(300, window.innerHeight - 24))
  const top = Math.max(84, Math.min(112, window.innerHeight - height - 8))
  const left = Math.max(12, window.innerWidth - width - 24)
  try {
    runtime.app?.setSize(`${width}px`, `${height}px`)
    runtime.app?.setPosition(`${top}px`, `${left}px`)
    Object.assign(element.style, {
      display: 'block',
      position: 'fixed',
      top: `${top}px`,
      left: `${left}px`,
      right: 'auto',
      bottom: 'auto',
      width: `${width}px`,
      height: `${height}px`,
      zIndex: '900',
      cursor: 'move',
      userSelect: 'none',
      opacity: hidden.value ? '0' : '1',
      pointerEvents: hidden.value ? 'none' : 'auto'
    })
    initialPositionApplied = true
  } catch (error) {
    console.warn('[SignAvatar] 数字人初始定位失败', error)
  }
}

function onHtmlReady() {
  if (disposed) return
  htmlReady.value = true
  positionAvatarInitially()
  console.info('[SignAvatar] 数字人界面资源已加载，等待动作服务就绪')
}

function showAvatar() {
  hidden.value = false
  if (runtime.app && runtime.ready) {
    try { runtime.app.enableYiyuApp() } catch { /* SDK may not be ready yet */ }
  }
  const element = getAvatarElement()
  if (element) {
    element.style.display = 'block'
    element.style.opacity = '1'
    element.style.pointerEvents = props.embedded ? 'none' : 'auto'
  }
  emit('visibility-change', true)
}

function hideAvatar() {
  hidden.value = true
  stopWelcome()
  stopCaption(true)
  if (runtime.app) {
    try { runtime.app.disableYiyuApp() } catch { /* ignored */ }
  }
  const element = getAvatarElement()
  if (element) element.style.pointerEvents = 'none'
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

function configureReadyApp() {
  status.value = 'ready'
  runtime.ready = true
  errorMessage.value = ''
  try { runtime.app.setIsShowTextarea(false) } catch { /* optional SDK UI */ }
  try { runtime.app.onSentenceFinished(handleSentenceFinished) } catch { /* optional SDK callback */ }
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

function onAppReady() {
  if (disposed) return
  clearTimeout(readyTimer)
  htmlReady.value = true
  console.info('[SignAvatar] 数字人动作服务已就绪')
  configureReadyApp()
}

async function initializeAvatar() {
  disposed = false
  welcomeStarted = false
  clearTimeout(readyTimer)
  status.value = 'loading'
  errorMessage.value = ''
  htmlReady.value = false
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
  try {
    console.info('[SignAvatar] SDK 脚本已加载，开始初始化数字人')
    runtime.app = await loadSdk()
    try { runtime.app.onHtmlReady(onHtmlReady) } catch { /* optional SDK callback */ }
    runtime.app.onAppReady(onAppReady)
    runtime.app.init({
      name: account,
      showToast: false,
      draggable: true,
      remotePath: true,
      readLocalResource: false
    })
    readyTimer = window.setTimeout(() => {
      if (status.value === 'loading' && !runtime.ready) {
        status.value = 'error'
        errorMessage.value = htmlReady.value
          ? '数字人资源已加载，但动作服务未在 45 秒内就绪'
          : '数字人资源加载超时'
        console.warn('[SignAvatar] 初始化超时', { htmlReady: htmlReady.value })
        emit('error', new Error(errorMessage.value))
      }
    }, SDK_READY_TIMEOUT_MS)
  } catch (error) {
    status.value = 'error'
    errorMessage.value = error?.message || '数字人加载失败'
    console.error('[SignAvatar] 初始化失败', error)
    emit('error', error)
  }
}

function retry() {
  if (status.value === 'error') initializeAvatar()
}

defineExpose({ replay, pushCaption, clearCaptions, showAvatar, hideAvatar, retry })

onMounted(() => {
  if (props.embedded && hostRef.value && typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => positionEmbeddedAvatar())
    resizeObserver.observe(hostRef.value)
  }
  initializeAvatar()
})
onBeforeUnmount(() => {
  disposed = true
  stopWelcome()
  stopCaption(false)
  clearTimeout(readyTimer)
  resizeObserver?.disconnect()
  resizeObserver = null
  const element = getAvatarElement()
  if (element) {
    try { runtime.app?.disableYiyuApp() } catch { /* ignored */ }
    element.style.display = 'none'
  }
})
</script>

<style scoped>
.sign-avatar-runtime { display: none; }
.sign-avatar-runtime--embedded {
  position: absolute;
  inset: 0;
  z-index: 1;
  display: block;
  width: 100%;
  height: 100%;
  overflow: hidden;
  pointer-events: none;
}
</style>
