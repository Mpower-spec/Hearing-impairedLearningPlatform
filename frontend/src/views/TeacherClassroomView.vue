<template>
  <div>
    <AppHeader>
      <el-button @click="router.push('/teacher/lessons')">返回课程</el-button>
    </AppHeader>

    <main class="page-shell classroom-page" v-loading="loading">
      <div class="page-title-row">
        <div>
          <h1 class="page-title">{{ lessonTitle || '教师课堂' }}</h1>
          <p class="page-subtitle">同步幻灯片、广播字幕并查看学生弹幕。</p>
        </div>

      </div>

      <div class="room-banner" v-if="room">
        <div>
          <div class="muted room-code-label">房间码</div>
          <div class="room-code" aria-live="polite">{{ roomCode }}</div>
        </div>
        <el-button @click="copyCode">复制房间码</el-button>
        <div class="spacer"></div>
        <span class="connection-pill" :class="connectionState">
          <span class="connection-dot"></span>{{ connectionText }}
        </span>
        <el-tag :type="status === 'LIVE' ? 'success' : 'info'" size="large">
          {{ status === 'LIVE' ? '课堂进行中' : '课堂已结束' }}
        </el-tag>
      </div>

      <section class="panel caption-hero" aria-label="实时字幕">
        <div class="panel-heading">
          <div>
            <h2>实时字幕</h2>
          </div>
          <div class="caption-hero-actions">
            <el-tag :type="listening ? 'success' : 'info'" size="large">
              {{ listening ? '识别中' : '未开始' }}
            </el-tag>
            <el-button type="primary" :disabled="status !== 'LIVE' || !speechSupported" @click="toggleSpeech">
              {{ listening ? '停止语音字幕' : '开始语音字幕' }}
            </el-button>
            <el-button :disabled="status !== 'LIVE'" @click="sendDemoCaption">
              发送下一句演示字幕
            </el-button>
          </div>
        </div>
        <div class="caption-big" aria-live="polite">
          {{ currentCaption || '点击“开始语音字幕”后，教师发言会显示在这里。' }}
        </div>
        <div v-if="interimCaption" class="interim-caption" aria-live="polite">{{ interimCaption }}</div>
        <p v-if="!speechSupported" class="warning-text">
          {{ speechUnavailableReason || '当前浏览器不支持 Web Speech API，请使用 Chrome/Edge，或使用演示字幕按钮。' }}
        </p>
      </section>

      <div class="classroom-grid">
        <section class="panel classroom-main-panel">
          <div class="panel-heading">
            <div>
              <h2>幻灯片</h2>
              <p class="muted">第 {{ page }} / {{ pageCount || 0 }} 页</p>
            </div>
            <span class="muted">学生端自动跟随页码</span>
          </div>
          <PdfStage :source="pdfUrl" :page="page" empty-text="PDF 加载中" />
          <div class="actions-row classroom-controls">
            <el-button size="large" :disabled="status !== 'LIVE' || page <= 1" @click="go(-1)">上一页</el-button>
            <el-button size="large" type="primary" :disabled="status !== 'LIVE' || page >= pageCount"
                       @click="go(1)">下一页</el-button>
            <span class="spacer"></span>
            <el-button type="danger" plain :disabled="status !== 'LIVE'" @click="endClass">结束课堂</el-button>
          </div>
        </section>

        <section class="panel classroom-side-panel">
          <div class="side-block classroom-history-block">
            <div class="side-heading">
              <h3>字幕记录</h3>
              <span class="muted">共 {{ captions.length }} 条</span>
            </div>
            <div class="caption-history" aria-label="字幕记录">
              <div v-for="item in captions" :key="item.id" class="caption-line">
                <time>{{ formatTime(item.createdAt) }}</time>
                {{ item.text || item.content }}
              </div>
              <div v-if="!captions.length" class="empty-note">暂无字幕记录</div>
            </div>
          </div>

          <div class="side-block classroom-danmaku-block">
            <div class="side-heading">
              <h3>学生弹幕</h3>
              <el-switch v-model="showDanmaku" active-text="显示" inactive-text="隐藏" />
            </div>
            <div v-if="showDanmaku" class="danmaku-list" aria-live="polite">
              <div v-for="item in danmaku" :key="item.id" class="danmaku-item">
                <strong>{{ item.userName }}</strong>
                <p>{{ item.content }}</p>
              </div>
              <div v-if="!danmaku.length" class="empty-note">还没有学生发送弹幕</div>
            </div>
          </div>
        </section>
      </div>
    </main>


  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppHeader from '../components/AppHeader.vue'
import PdfStage from '../components/PdfStage.vue'
import http from '../api/http.js'
import { session } from '../auth/session.js'

const route = useRoute()
const router = useRouter()
const roomId = route.params.id
const loading = ref(true)
const room = ref(null)
const lessonTitle = ref('')
const lessonId = ref(null)
const roomCode = ref('')
const pdfUrl = ref('')
const page = ref(1)
const pageCount = ref(0)
const status = ref('LIVE')
const connectionState = ref('connecting')
const connectionText = ref('连接中…')
const captions = ref([])
const currentCaption = ref('')
const danmaku = ref([])
const showDanmaku = ref(true)
const speechSupported = ref(false)
const speechUnavailableReason = ref('')
const listening = ref(false)
const interimCaption = ref('')


const demoCaptions = [
  '同学们好，今天我们来学习本节课的核心内容。',
  '首先看第一部分，理解概念的基本含义。',
  '接下来通过一个例子，观察条件和结论之间的关系。',
  '请注意这一处容易混淆，我们可以用两个关键词来区分。',
  '最后回顾今天的重点，并完成一道复习题。'
]

let socket = null
let reconnectTimer = null
let recognition = null
let manuallyClosed = false
let demoIndex = 0

function formatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(11, 19)
}

function wsUrl() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}/ws/classroom?token=${encodeURIComponent(session.token)}`
}

function connect() {
  if (status.value === 'ENDED') return
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
    if (status.value === 'LIVE' && !manuallyClosed) {
      connectionState.value = 'connecting'
      connectionText.value = '连接断开，准备重连…'
      reconnectTimer = setTimeout(connect, 2500)
    } else {
      connectionState.value = status.value === 'ENDED' ? 'live' : 'error'
      connectionText.value = status.value === 'ENDED' ? '课堂已结束' : '连接已关闭'
    }
  }
}

function handleMessage(message) {
  switch (message.type) {
    case 'state':
      lessonTitle.value = message.lessonTitle
      lessonId.value = message.lessonId
      roomCode.value = message.roomCode
      page.value = message.page
      pageCount.value = message.pageCount
      pdfUrl.value = message.pdfUrl
      status.value = message.status
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
      stopSpeech()
      ElMessage.info('课堂已结束')
      break
    case 'error':
      ElMessage.error(message.message || '课堂消息处理失败')
      break
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

function send(payload) {
  if (socket?.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify(payload))
  }
}

function go(delta) {
  const next = page.value + delta
  if (next < 1 || next > pageCount.value) return
  page.value = next
  send({ type: 'slide', page: next })
}

async function copyCode() {
  try {
    await navigator.clipboard.writeText(roomCode.value)
    ElMessage.success('房间码已复制')
  } catch {
    ElMessage.info(`房间码：${roomCode.value}`)
  }
}

function speechErrorMessage(code) {
  const labels = {
    network: '语音识别服务连接失败，请检查网络或改用演示字幕。',
    'not-allowed': '麦克风权限被拒绝，请在浏览器地址栏允许麦克风。',
    'service-not-allowed': '浏览器禁止使用语音识别服务，请检查站点权限或改用演示字幕。',
    'audio-capture': '没有找到可用麦克风，请检查设备连接。',
    'language-not-supported': '当前浏览器不支持中文语音识别。',
    'no-speech': '暂时没有检测到语音，请靠近麦克风后重试。',
    aborted: '语音识别已停止。'
  }
  return labels[code] || `语音识别异常：${code}`
}

function setupSpeech() {
  if (!window.isSecureContext) {
    speechSupported.value = false
    speechUnavailableReason.value = '当前通过局域网 HTTP 访问，浏览器不允许授权麦克风。教师请在本机打开 http://localhost:5173，或使用“发送下一句演示字幕”。'
    return
  }
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  speechSupported.value = Boolean(SpeechRecognition)
  if (!SpeechRecognition) {
    speechUnavailableReason.value = '当前浏览器不支持 Web Speech API，请使用 Chrome/Edge，或使用演示字幕按钮。'
    return
  }
  recognition = new SpeechRecognition()
  recognition.lang = 'zh-CN'
  recognition.continuous = true
  recognition.interimResults = true
  recognition.onresult = (event) => {
    let interim = ''
    for (let i = event.resultIndex; i < event.results.length; i++) {
      const text = event.results[i][0].transcript.trim()
      if (event.results[i].isFinal) {
        if (text) {
          currentCaption.value = text
          send({ type: 'caption', text })
        }
      } else {
        interim += text
      }
    }
    interimCaption.value = interim
  }
  recognition.onend = () => {
    interimCaption.value = ''
    if (listening.value) {
      setTimeout(() => {
        if (!listening.value) return
        try { recognition.start() } catch { stopSpeech() }
      }, 300)
    }
  }
  recognition.onerror = (event) => {
    const fatalErrors = ['network', 'not-allowed', 'service-not-allowed', 'audio-capture', 'language-not-supported']
    if (fatalErrors.includes(event.error)) {
      listening.value = false
      interimCaption.value = ''
      speechUnavailableReason.value = speechErrorMessage(event.error)
    }
    if (!['no-speech', 'aborted'].includes(event.error)) {
      ElMessage.warning(speechErrorMessage(event.error))
    }
  }
}

function toggleSpeech() {
  if (listening.value) {
    stopSpeech()
    return
  }
  if (!speechSupported.value || !recognition) {
    ElMessage.warning(speechUnavailableReason.value || '当前环境无法使用语音识别，请改用演示字幕。')
    return
  }
  listening.value = true
  try {
    recognition.start()
  } catch (error) {
    listening.value = false
    speechUnavailableReason.value = '无法启动语音识别，请检查麦克风权限或改用演示字幕。'
    ElMessage.error(speechUnavailableReason.value)
  }
}

function stopSpeech() {
  listening.value = false
  interimCaption.value = ''
  try { recognition?.stop() } catch { /* ignored */ }
}

function sendDemoCaption() {
  const text = demoCaptions[demoIndex % demoCaptions.length]
  demoIndex += 1
  currentCaption.value = text
  send({ type: 'caption', text })
}

async function endClass() {
  try {
    await ElMessageBox.confirm('结束后学生将不能继续发送弹幕，但仍可查看已生成的资料。', '结束课堂', {
      confirmButtonText: '确认结束',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  if (socket?.readyState === WebSocket.OPEN) {
    send({ type: 'end' })
  } else {
    await http.post(`/rooms/${roomId}/end`)
    status.value = 'ENDED'
    connectionText.value = '课堂已结束'
  }
}


async function load() {
  loading.value = true
  try {
    room.value = await http.get(`/rooms/${roomId}`)
    lessonTitle.value = room.value.lessonTitle
    lessonId.value = room.value.lessonId
    roomCode.value = room.value.roomCode
    pdfUrl.value = room.value.pdfUrl
    page.value = room.value.currentPage
    pageCount.value = room.value.pageCount
    status.value = room.value.status
    const [captionHistory, danmakuHistory] = await Promise.all([
      http.get(`/rooms/${roomId}/captions`, { silent: true }).catch(() => []),
      http.get(`/rooms/${roomId}/danmaku`, { silent: true }).catch(() => [])
    ])
    captions.value = captionHistory || []
    danmaku.value = danmakuHistory || []
    if (captions.value.length) currentCaption.value = captions.value.at(-1).content
    setupSpeech()
    connect()
  } finally {
    loading.value = false
  }
}

onMounted(load)
onBeforeUnmount(() => {
  manuallyClosed = true
  clearTimeout(reconnectTimer)
  stopSpeech()
  try { socket?.close() } catch { /* ignored */ }
})
</script>