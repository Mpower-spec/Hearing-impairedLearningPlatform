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
          <el-button type="primary" plain :disabled="!lessonId" @click="qaVisible = true">课程问答</el-button>
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
          <div v-if="status === 'ENDED'" class="page-nav" role="group" aria-label="PPT 页码">
            <el-button :disabled="page <= 1" aria-label="查看上一页 PPT" @click="go(-1)">上一页</el-button>
            <span class="page-indicator" aria-live="polite">
              第 {{ page }} / {{ pageCount || 0 }} 页
              <small>课后可自由翻阅</small>
            </span>
            <el-button type="primary" :disabled="page >= pageCount" aria-label="查看下一页 PPT" @click="go(1)">下一页</el-button>
          </div>
          <span v-else class="page-indicator">第 {{ page }} / {{ pageCount || 0 }} 页 · 教师控制</span>
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


    <el-drawer
      v-model="qaVisible"
      class="course-qa-drawer"
      direction="rtl"
      size="min(420px, 100%)"
      :with-header="true"
    >
      <template #header>
        <div class="qa-drawer-title">
          <strong>课程 AI 问答</strong>
          <span>{{ lessonTitle || '当前课堂' }}</span>
        </div>
      </template>

      <div class="qa-drawer-content">
        <div class="qa-context">
          <el-icon><ChatDotRound /></el-icon>
          <span>回答依据：PPT 内容、当前课堂字幕和课堂学习资料</span>
        </div>

        <div class="qa-message-list" aria-live="polite">
          <div v-if="!qaMessages.length" class="qa-empty">
            <el-icon><ChatDotRound /></el-icon>
            <strong>有问题，直接问</strong>
            <p>例如：“这节课的重点是什么？”或“请解释刚才提到的概念。”</p>
          </div>

          <article
            v-for="(message, index) in qaMessages"
            :key="`${message.role}-${index}`"
            class="qa-message"
            :class="`qa-message--${message.role}`"
          >
            <div class="qa-bubble">{{ message.content }}</div>
            <div v-if="message.role === 'assistant' && message.source" class="qa-answer-meta">
              <el-tag size="small" :type="message.source === 'MOCK' ? 'warning' : (message.source === 'LOCAL' ? 'info' : 'success')">
                {{ qaSourceLabel(message.source) }}
              </el-tag>
            </div>
            <el-collapse v-if="message.sources?.length" class="qa-sources">
              <el-collapse-item title="查看引用来源">
                <div v-for="(source, sourceIndex) in message.sources" :key="sourceIndex" class="qa-source-item">
                  <el-tag size="small" effect="plain">{{ source.type }}</el-tag>
                  <strong>{{ source.label }}</strong>
                  <p>{{ source.excerpt }}</p>
                </div>
              </el-collapse-item>
            </el-collapse>
          </article>

          <div v-if="qaLoading" class="qa-loading">
            <el-icon class="is-loading"><Loading /></el-icon>
            正在检索课程资料并生成回答…
          </div>
        </div>

        <div class="qa-composer">
          <el-alert v-if="qaError" :title="qaError" type="error" :closable="false" show-icon />
          <el-button v-if="qaRetryQuestion" text type="primary" @click="askQuestion(true)">重试上一个问题</el-button>
          <el-input
            v-model="qaInput"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            resize="none"
            placeholder="输入课程问题，Enter 发送，Shift+Enter 换行"
            @keydown.enter.exact.prevent="askQuestion()"
          />
          <div class="qa-composer-footer">
            <span>回答仅基于当前课程资料</span>
            <el-button type="primary" :loading="qaLoading" :disabled="!qaInput.trim() || !lessonId" @click="askQuestion()">
              发送问题
            </el-button>
          </div>
        </div>
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
import { ChatDotRound, Loading } from '@element-plus/icons-vue'
import AppHeader from '../components/AppHeader.vue'
import PdfStage from '../components/PdfStage.vue'
import MarkdownPanel from '../components/MarkdownPanel.vue'
import { toPlainText } from '../utils/plainText.js'

import http from '../api/http.js'
import { clearSignCaptions, pushSignCaption } from '../avatar/avatarStore.js'
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
const qaVisible = ref(false)
const qaInput = ref('')
const qaMessages = ref([])
const qaLoading = ref(false)
const qaError = ref('')
const qaRetryQuestion = ref('')

let socket = null
let reconnectTimer = null
let manuallyClosed = false
let historyLoaded = false


async function askQuestion(retry = false) {
  const question = retry ? qaRetryQuestion.value : qaInput.value.trim()
  if (!question || !lessonId.value || qaLoading.value) return
  if (!retry) {
    qaMessages.value.push({ role: 'user', content: toPlainText(question) })
    qaInput.value = ''
  }
  qaLoading.value = true
  qaError.value = ''
  try {
    const result = await http.post('/course-qa/ask', {
      roomId: Number(roomId),
      question
    })
    qaMessages.value.push({
      role: 'assistant',
      content: toPlainText(result.answer),
      source: result.source,
      sources: (result.sources || []).map((source) => ({
        ...source,
        excerpt: toPlainText(source.excerpt)
      }))
    })
    qaRetryQuestion.value = ''
  } catch {
    qaError.value = '课程问答暂时失败，请重试。'
    qaRetryQuestion.value = question
  } finally {
    qaLoading.value = false
  }
}

function qaSourceLabel(source) {
  if (source === 'MOCK') return '模拟回答'
  if (source === 'LOCAL') return '本地检索'
  return 'AI 回答'
}

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
      pushSignCaption(message.text, message.id)
      break
    case 'danmaku':
      addDanmaku(message)
      break
    case 'end':
      status.value = 'ENDED'
      connectionState.value = 'live'
      connectionText.value = '课堂已结束'
      clearSignCaptions()
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

function go(delta) {
  const next = page.value + delta
  if (next < 1 || next > pageCount.value) return
  page.value = next
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
  clearSignCaptions()
  try { socket?.close() } catch { /* ignored */ }
})
</script>
<style scoped>
.qa-drawer-title strong, .qa-drawer-title span { display: block; }
.qa-drawer-title strong { color: var(--ink); font-size: 17px; }
.qa-drawer-title span { margin-top: 3px; color: var(--ink-soft); font-size: 12px; }
.qa-drawer-content { height: 100%; min-height: 0; display: flex; flex-direction: column; }
.qa-context { display: flex; align-items: flex-start; gap: 8px; margin: 12px 16px 0; padding: 10px 12px; border: 1px solid #cde3e7; border-radius: 12px; background: linear-gradient(135deg, #eef8f7, #f7fbf4); color: var(--ink-soft); font-size: 11px; line-height: 1.5; }
.qa-context .el-icon { flex: 0 0 auto; margin-top: 2px; color: var(--primary); }
.qa-message-list { flex: 1; min-height: 0; overflow-y: auto; padding: 14px 16px; }
.qa-empty { min-height: 180px; display: flex; align-items: center; justify-content: center; flex-direction: column; text-align: center; color: var(--ink-soft); }
.qa-empty .el-icon { margin-bottom: 10px; color: var(--primary); font-size: 34px; }
.qa-empty p { max-width: 280px; margin: 7px 0 0; line-height: 1.6; }
.qa-message { margin-bottom: 14px; }
.qa-message--user { display: flex; justify-content: flex-end; }
.qa-message--assistant { display: flex; align-items: flex-start; flex-direction: column; }
.qa-bubble { max-width: 92%; padding: 10px 12px; border-radius: 14px; line-height: 1.7; white-space: pre-wrap; overflow-wrap: anywhere; }
.qa-message--user .qa-bubble { color: #fff; background: linear-gradient(135deg, #0b6e99, #198c86); border-bottom-right-radius: 4px; }
.qa-message--assistant .qa-bubble { color: var(--ink); background: linear-gradient(145deg, #f5fafb, #e8f4f2); border: 1px solid #d2e5e9; border-bottom-left-radius: 4px; }
.qa-answer-meta { margin-top: 5px; }
.qa-sources { width: 100%; margin-top: 5px; }
.qa-source-item { padding: 8px 0; border-bottom: 1px solid var(--line); }
.qa-source-item:last-child { border-bottom: 0; }
.qa-source-item strong { margin-left: 6px; font-size: 12px; }
.qa-source-item p { margin: 5px 0 0; color: var(--ink-soft); font-size: 12px; line-height: 1.6; }
.qa-loading { display: flex; align-items: center; gap: 7px; color: var(--primary-strong); font-size: 12px; }
.qa-composer { flex-shrink: 0; padding: 12px 16px calc(12px + env(safe-area-inset-bottom)); border-top: 1px solid var(--line); background: rgba(244, 251, 250, 0.96); }
.qa-composer .el-alert { margin-bottom: 8px; }
.qa-composer-footer { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-top: 8px; }
.qa-composer-footer span { color: var(--ink-soft); font-size: 10px; }
@media (max-width: 767px) {
  .qa-drawer-content { height: calc(100vh - 58px); }
  .qa-message-list { padding: 12px; }
  .qa-context { margin: 8px 12px 0; }
  .qa-composer { padding: 10px 12px calc(10px + env(safe-area-inset-bottom)); }
  .qa-composer-footer { align-items: stretch; flex-direction: column; }
  .qa-composer-footer .el-button { width: 100%; }
}
</style>
