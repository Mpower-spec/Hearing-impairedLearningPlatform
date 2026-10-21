<template>
  <div class="sign-avatar-widget" aria-live="polite">
    <div v-if="hidden" class="avatar-restore-card">
      <span class="avatar-status-dot ready"></span>
      <span>手语数字人已隐藏</span>
      <el-button size="small" type="primary" @click="showAvatar">显示</el-button>
    </div>
    <div v-else class="avatar-control-card">
      <div class="avatar-control-heading">
        <span class="avatar-status-dot" :class="status"></span>
        <div>
          <strong>手语数字人</strong>
          <small>{{ statusText }}</small>
        </div>
      </div>
      <p v-if="status === 'error'" class="avatar-error">{{ errorMessage }}</p>
      <div class="avatar-control-actions">
        <el-button size="small" type="primary" :disabled="status !== 'ready'" @click="playHello">重播“你好”</el-button>
        <el-button v-if="status === 'error'" size="small" @click="retry">重试</el-button>
        <el-button v-else size="small" text :disabled="status !== 'ready'" @click="hideAvatar">隐藏</el-button>
      </div>
      <small v-if="status === 'ready'" class="avatar-drag-tip">可拖动数字人调整位置</small>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps({
  initialText: { type: String, default: '你好' },
  autoPlay: { type: Boolean, default: true }
})
const emit = defineEmits(['ready', 'error', 'visibility-change'])

const SDK_URL = 'https://avatar.gbqr.net/yiyu.js'
let sdkPromise = null
let sharedApp = null
let sharedReady = false
let sharedAutoPlayed = false
let disposed = false
let readyTimer = null

const status = ref('loading')
const errorMessage = ref('')
const hidden = ref(false)

const statusText = computed(() => {
  if (status.value === 'loading') return '正在加载 SDK…'
  if (status.value === 'ready') return '已就绪，可拖动浮层'
  if (status.value === 'error') return '加载失败'
  return '等待初始化'
})

async function loadSdk() {
  if (window.yiyu?.app) return window.yiyu.app
  if (!sdkPromise) {
    sdkPromise = import(/* @vite-ignore */ SDK_URL)
      .then((module) => {
        const app = module?.yiyu || window.yiyu?.app
        if (!app) throw new Error('SDK 未暴露 yiyu.app')
        return app
      })
      .catch((error) => {
        sdkPromise = null
        throw error
      })
  }
  return sdkPromise
}

function getAvatarElement() {
  return document.getElementById('yiyuAppElement')
}

function restorePointerEvents() {
  const element = getAvatarElement()
  if (element) element.style.pointerEvents = 'auto'
}

function disablePointerEvents() {
  const element = getAvatarElement()
  if (element) element.style.pointerEvents = 'none'
}

function showAvatar() {
  hidden.value = false
  if (sharedApp && sharedReady) {
    try { sharedApp.enableYiyuApp() } catch { /* SDK may not be ready yet */ }
  }
  restorePointerEvents()
  emit('visibility-change', true)
}

function hideAvatar() {
  hidden.value = true
  if (sharedApp) {
    try { sharedApp.cancelTranslation() } catch { /* ignored */ }
    try { sharedApp.disableYiyuApp() } catch { /* ignored */ }
  }
  disablePointerEvents()
  emit('visibility-change', false)
}

function playHello() {
  if (!sharedApp || status.value !== 'ready') return
  try {
    sharedApp.startTranslate(props.initialText)
  } catch (error) {
    errorMessage.value = `播放失败：${error.message}`
    status.value = 'error'
    emit('error', error)
  }
}

function onAppReady() {
  if (disposed) return
  clearTimeout(readyTimer)
  status.value = 'ready'
  sharedReady = true
  errorMessage.value = ''
  try { sharedApp.setIsShowTextarea(false) } catch { /* optional SDK UI */ }
  try { sharedApp.setAvatarSize(4) } catch { /* optional SDK sizing */ }
  restorePointerEvents()
  emit('ready')
  if (props.autoPlay && !sharedAutoPlayed) {
    sharedAutoPlayed = true
    window.setTimeout(() => playHello(), 400)
  }
}

async function initializeAvatar() {
  clearTimeout(readyTimer)
  status.value = 'loading'
  errorMessage.value = ''
  if (sharedReady && sharedApp) {
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
    sharedApp = await loadSdk()
    sharedApp.onAppReady(onAppReady)
    sharedApp.init({
      name: account,
      showToast: false,
      draggable: true,
      remotePath: true,
      readLocalResource: false
    })
    readyTimer = window.setTimeout(() => {
      if (status.value === 'loading' && !sharedReady) {
        status.value = 'error'
        errorMessage.value = '数字人初始化超时，请检查网络或 SDK 账号'
        emit('error', new Error(errorMessage.value))
      }
    }, 20000)
  } catch (error) {
    status.value = 'error'
    errorMessage.value = error?.message || '数字人 SDK 加载失败'
    emit('error', error)
  }
}

function retry() {
  if (status.value === 'error') initializeAvatar()
}

onMounted(initializeAvatar)
onBeforeUnmount(() => {
  disposed = true
  clearTimeout(readyTimer)
  if (sharedApp) {
    try { sharedApp.cancelTranslation() } catch { /* ignored */ }
    try { sharedApp.disableYiyuApp() } catch { /* ignored */ }
  }
  disablePointerEvents()
})
</script>

<style scoped>
.sign-avatar-widget {
  position: fixed;
  right: 18px;
  bottom: 18px;
  z-index: 1000;
  pointer-events: none;
}
.avatar-control-card, .avatar-restore-card {
  pointer-events: auto;
  border: 1px solid #c5e2ee;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12px 30px rgba(16, 42, 67, 0.16);
  backdrop-filter: blur(10px);
}
.avatar-control-card { width: 228px; padding: 12px 14px; }
.avatar-restore-card { display: flex; align-items: center; gap: 8px; padding: 9px 11px; color: var(--ink-soft); font-size: 13px; }
.avatar-control-heading { display: flex; align-items: center; gap: 9px; }
.avatar-control-heading strong, .avatar-control-heading small { display: block; }
.avatar-control-heading strong { font-size: 14px; color: var(--ink); }
.avatar-control-heading small { margin-top: 3px; color: var(--ink-soft); font-size: 11px; }
.avatar-status-dot { flex: 0 0 auto; width: 8px; height: 8px; border-radius: 50%; background: #98a2b3; }
.avatar-status-dot.loading { background: #f0b429; box-shadow: 0 0 0 4px rgba(240,180,41,.16); }
.avatar-status-dot.ready { background: #22c55e; box-shadow: 0 0 0 4px rgba(34,197,94,.14); }
.avatar-status-dot.error { background: #ef4444; box-shadow: 0 0 0 4px rgba(239,68,68,.14); }
.avatar-control-actions { display: flex; align-items: center; gap: 5px; margin-top: 10px; flex-wrap: wrap; }
.avatar-control-actions .el-button + .el-button { margin-left: 0; }
.avatar-error { margin: 8px 0 0; color: #b42318; font-size: 11px; line-height: 1.5; }
.avatar-drag-tip { display: block; margin-top: 7px; color: #8aa2b2; font-size: 10px; }
</style>