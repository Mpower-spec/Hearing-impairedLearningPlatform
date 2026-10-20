<template>
  <div>
    <AppHeader>
      <el-button @click="router.push('/student/join')">加入课堂</el-button>
      <el-button @click="router.push('/student/videos')">视频学习</el-button>
    </AppHeader>

    <main class="page-shell classroom-page classroom-focus-page" v-loading="loading">
      <div class="focus-topline">
        <div class="focus-title">
          <span class="focus-kicker">LIVE CLASSROOM</span>
          <div class="focus-title-row">
            <h1>{{ lessonTitle || '学生课堂' }}</h1>
            <span class="focus-room-code">房间码 <strong>{{ roomCode || '------' }}</strong></span>
          </div>
        </div>
        <div class="focus-top-actions">
          <template v-if="status === 'ENDED' && lessonId">
            <el-button v-if="material" type="warning" @click="materialVisible = true">查看课堂资料</el-button>
            <el-button type="primary" :loading="generatingMaterial" @click="generateMaterial">
              {{ material ? '重新生成资料' : '生成本节课资料' }}
            </el-button>
          </template>
          <span class="connection-pill" :class="connectionState">
            <span class="connection-dot"></span>{{ connectionText }}
          </span>
          <el-tag :type="status === 'LIVE' ? 'success' : 'info'" size="large">
            {{ status === 'LIVE' ? '课堂进行中' : (status === 'ENDED' ? '课堂已结束' : '等待加入') }}
          </el-tag>
        </div>
      </div>

      <section class="caption-focus" aria-label="实时字幕">
        <div class="caption-toolbar">
          <span class="caption-label">实时字幕</span>
          <el-tag :type="status === 'LIVE' ? 'success' : 'info'" size="small">
            {{ status === 'LIVE' ? '正在接收' : (status === 'ENDED' ? '课堂已结束' : '等待加入') }}
          </el-tag>
        </div>
        <div class="caption-big" aria-live="polite">
          {{ currentCaption || '教师开始语音字幕后，内容会显示在这里。' }}
        </div>
      </section>

      <section class="ppt-focus">
        <PdfStage class="ppt-stage-focus" :source="pdfUrl" :page="page" empty-text="等待教师开始课堂" />
        <div class="ppt-controlbar">
          <span class="page-indicator">第 {{ page }} / {{ pageCount || 0 }} 页 · 教师控制</span>
          <el-switch v-model="showDanmaku" active-text="弹幕显示" inactive-text="弹幕隐藏" />
          <el-button @click="recordsVisible = true">
            课堂记录
            <span class="record-count">{{ captions.length + danmaku.length }}</span>
          </el-button>
        </div>
      </section>
    </main>

    <el-drawer v-model="recordsVisible" title="课堂记录" direction="btt" size="48%">
      <div class="records-grid">
        <section class="record-panel">
          <div class="record-heading"><h3>字幕记录</h3><span>{{ captions.length }} 条</span></div>
          <div class="caption-history" aria-label="字幕记录">
            <div v-for="item in captions" :key="item.id" class="caption-line">
              <time>{{ formatTime(item.createdAt) }}</time>
              {{ item.text || item.content }}
            </div>
            <div v-if="!captions.length" class="empty-note">暂无字幕记录</div>
          </div>
        </section>
        <section v-if="showDanmaku" class="record-panel">
          <div class="record-heading"><h3>课堂弹幕</h3><span>{{ danmaku.length }} 条</span></div>
          <div class="danmaku-list" aria-live="polite">
            <div v-for="item in danmaku" :key="item.id" class="danmaku-item">
              <strong>{{ item.userName }}</strong>
              <p>{{ item.content }}</p>
            </div>
            <div v-if="!danmaku.length" class="empty-note">还没有人发送弹幕</div>
          </div>
          <div class="actions-row danmaku-input-row">
            <el-input
              v-model="danmakuText"
              maxlength="500"
              placeholder="输入弹幕内容"
              :disabled="status !== 'LIVE'"
              @keyup.enter="sendDanmaku"
            />
            <el-button type="primary" :disabled="status !== 'LIVE' || !danmakuText.trim()" @click="sendDanmaku">
              发送
            </el-button>
          </div>
        </section>
      </div>
    </el-drawer>

    <el-dialog v-model="materialVisible" title="课堂学习资料" width="min(860px, 92vw)">
      <MarkdownPanel title="课后资料" :content="material?.content" :source="material?.source" />
    </el-dialog>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppHeader from '../components/AppHeader.vue'
import PdfStage from '../components/PdfStage.vue'
import MarkdownPanel from '../components/MarkdownPanel.vue'
import http from '../api/http.js'
import { session } from '../auth/session.js'

const route = useRoute()
const router = useRouter()
const roomId = route.params.id
const roomCode = ref(route.query.code || sessionStorage.getItem('hp_room_code') || '')
const loading = ref(true)
const lessonTitle = ref('')
const lessonId = ref(null)
const pdfUrl = ref('')
const page = ref(1)
const pageCount = ref(0)
const status = ref('WAITING')
const connectionState = ref('connecting')
const connectionText = ref('连接中…')
const captions = ref([])
const currentCaption = ref('')
const danmaku = ref([])
const danmakuText = ref('')
const showDanmaku = ref(true)
const recordsVisible = ref(false)
const materialVisible = ref(false)
const generatingMaterial = ref(false)
const material = ref(null)

let socket = null
let reconnectTimer = null
let manuallyClosed = false
let historyLoaded = false

function formatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(11, 19)
}

function wsUrl() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws/classroom?token=${encodeURIComponent(session.token)}`
}

function applyRoom(room) {
  lessonTitle.value = room.lessonTitle
  lessonId.value = room.lessonId
  roomCode.value = room.roomCode
  pdfUrl.value = room.pdfUrl
  page.value = room.currentPage ?? room.page ?? 1
  pageCount.value = room.pageCount
  status.value = room.status
}

function connect() {
  if (status.value === 'ENDED') return
  if (!roomCode.value) {
    connectionState.value = 'error'
    connectionText.value = '缺少房间码'
    ElMessage.error('缺少房间码，请重新从加入课堂页面进入')
    return
  }
  clearTimeout(reconnectTimer)
  connectionState.value = 'connecting'
  connectionText.value = '连接中…'
  socket = new WebSocket(wsUrl())
  socket.onopen = () => {
    connectionState.value = 'live'
    connectionText.value = '已连接'
    send({ type: 'join', roomCode: roomCode.value })
  }
  socket.onmessage = (event) => {
    try {
      handleMessage(JSON.parse(event.data))
    } catch {
      ElMessage.error('收到无法解析的课堂消息')
    }
  }
  socket.onerror = () => {
    connectionState.value = 'error'
    connectionText.value = '连接异常'
  }
  socket.onclose = () => {
    if (status.value !== 'ENDED' && !manuallyClosed) {
      connectionState.value = 'connecting'
      connectionText.value = '连接断开，准备重连…'
      reconnectTimer = setTimeout(connect, 2500)
    } else {
      connectionState.value = status.value === 'ENDED' ? 'live' : 'error'
      connectionText.value = status.value === 'ENDED' ? '课堂已结束' : '连接已关闭'
    }
  }
}

async function handleMessage(message) {
  switch (message.type) {
    case 'state':
      applyRoom(message)
      connectionState.value = 'live'
      connectionText.value = '已连接'
      if (!historyLoaded) {
        historyLoaded = true
        await loadHistory()
      }
      break
    case 'slide':
      page.value = message.page
      break
    case 'caption':
      currentCaption.value = message.text
      addCaption({ id: message.id, text: message.text, createdAt: message.createdAt })
      break
    case 'danmaku':
      addDanmaku(message)
      break
    case 'end':
      status.value = 'ENDED'
      connectionState.value = 'live'
      connectionText.value = '课堂已结束'
      ElMessage.info('课堂已结束，可以生成学习资料')
      await loadExistingMaterial()
      break
    case 'error':
      ElMessage.error(message.message || '课堂消息处理失败')
      break
  }
}

function send(payload) {
  if (socket?.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify(payload))
  }
}

function addCaption(item) {
  if (!item.id || captions.value.some((entry) => entry.id === item.id)) return
  captions.value.push(item)
}

function addDanmaku(item) {
  if (!item.id || danmaku.value.some((entry) => entry.id === item.id)) return
  danmaku.value.push(item)
}

function sendDanmaku() {
  const text = danmakuText.value.trim()
  if (!text || status.value !== 'LIVE') return
  send({ type: 'danmaku', content: text })
  danmakuText.value = ''
}

async function loadHistory() {
  const [captionHistory, danmakuHistory] = await Promise.all([
    http.get(`/rooms/${roomId}/captions`, { silent: true }).catch(() => []),
    http.get(`/rooms/${roomId}/danmaku`, { silent: true }).catch(() => [])
  ])
  captions.value = captionHistory || []
  danmaku.value = danmakuHistory || []
  if (captions.value.length) currentCaption.value = captions.value.at(-1).content
}

async function loadExistingMaterial() {
  if (!lessonId.value) return
  try {
    material.value = await http.get(`/materials/class/${lessonId.value}`, { silent: true })
  } catch {
    material.value = null
  }
}

async function generateMaterial() {
  if (!lessonId.value) return
  generatingMaterial.value = true
  try {
    material.value = await http.post(`/ai/material/${lessonId.value}`)
    materialVisible.value = true
    ElMessage.success(material.value.source === 'MOCK'
      ? '已生成演示兜底学习资料'
      : '本节课学习资料已生成')
  } finally {
    generatingMaterial.value = false
  }
}

async function initialize() {
  loading.value = true
  try {
    try {
      const existing = await http.get(`/rooms/${roomId}`, { silent: true })
      applyRoom(existing)
      await loadHistory()
      historyLoaded = true
      if (existing.status === 'ENDED') {
        connectionState.value = 'live'
        connectionText.value = '课堂已结束'
        await loadExistingMaterial()
        return
      }
    } catch {
      // First-time join: membership is created by the WebSocket join message.
    }
    connect()
  } finally {
    loading.value = false
  }
}

onMounted(initialize)
onBeforeUnmount(() => {
  manuallyClosed = true
  clearTimeout(reconnectTimer)
  try { socket?.close() } catch { /* ignored */ }
})
</script>