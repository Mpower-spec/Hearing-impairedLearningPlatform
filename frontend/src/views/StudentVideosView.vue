<template>
  <div>
    <AppHeader>
      <el-button @click="router.push('/student/join')">加入课堂</el-button>
      <el-button @click="router.push('/student/videos')">视频学习</el-button>
    </AppHeader>

    <main class="page-shell">
      <div class="page-title-row">
        <div>
          <h1 class="page-title">视频学习</h1>
          <p class="page-subtitle">上传 MP4/WebM，生成带估算时间轴的字幕和结构化学习笔记。</p>
        </div>
      </div>

      <section class="panel">
        <div class="panel-heading">
          <div>
            <h2>上传学习视频</h2>
            <p class="muted">最大 100MB，浏览器需直接支持该视频编码；不做转码。</p>
          </div>
        </div>
        <div class="actions-row">
          <el-input v-model="uploadTitle" placeholder="视频标题" maxlength="200" style="max-width: 320px" />
          <el-upload ref="uploadRef" :auto-upload="false" :limit="1" accept=".mp4,.webm" :on-change="onFileChange"
                     :on-remove="() => (uploadFile = null)">
            <el-button>选择视频</el-button>
          </el-upload>
          <el-button type="primary" :loading="uploading" :disabled="!uploadTitle.trim() || !uploadFile"
                     @click="submitUpload">
            上传视频
          </el-button>
        </div>
      </section>

      <div class="video-layout" style="margin-top: 20px">
        <section class="panel">
          <div class="panel-heading">
            <div>
              <h2>{{ selected?.title || '视频播放器' }}</h2>
              <p class="muted" v-if="selected">状态：{{ statusLabel(selected.status) }}</p>
            </div>
            <el-tag v-if="selected?.transcriptSource" :type="selected.transcriptSource === 'MOCK' ? 'warning' : 'success'">
              {{ selected.transcriptSource === 'MOCK' ? '模拟字幕' : 'AI 字幕' }}
            </el-tag>
          </div>
          <video v-if="selected" :key="`${selected.id}-${selected.vttUrl || 'no-vtt'}`"
                 class="video-player" :src="selected.videoUrl" controls preload="metadata">
            <track v-if="selected.vttUrl" :src="selected.vttUrl" kind="captions" srclang="zh" label="中文字幕" default />
          </video>
          <el-empty v-else description="上传或选择一个视频" />
          <p v-if="selected?.timingMode === 'ESTIMATED'" class="warning-text">
            当前字幕时间轴根据文本长度和视频时长估算，并非逐词精确时间戳。
          </p>
          <div class="actions-row" style="margin-top: 16px">
            <el-button type="primary" :loading="transcribing" :disabled="!selected" @click="transcribe">
              {{ selected?.transcript ? '重新生成字幕' : '生成字幕' }}
            </el-button>
            <el-button type="warning" :loading="noting" :disabled="!selected?.transcript" @click="generateNote">
              {{ selected?.note ? '重新生成笔记' : '生成学习笔记' }}
            </el-button>
          </div>
        </section>

        <section class="panel">
          <div class="panel-heading">
            <div>
              <h2>我的视频</h2>
              <p class="muted">共 {{ videos.length }} 个视频</p>
            </div>
            <el-button :loading="loading" @click="load">刷新</el-button>
          </div>
          <div class="video-list">
            <button v-for="video in videos" :key="video.id" class="video-list-item"
                    :class="{ active: selected?.id === video.id }" @click="selected = video">
              <strong>{{ video.title }}</strong>
              <small>{{ statusLabel(video.status) }} · {{ formatTime(video.createdAt) }}</small>
            </button>
            <div v-if="!videos.length" class="empty-note">还没有上传视频</div>
          </div>
        </section>
      </div>

      <section class="panel" v-if="selected">
        <div class="panel-heading">
          <div>
            <h2>语音转写</h2>
            <p class="muted">SiliconFlow SenseVoice 返回整段文本，系统按句长估算时间轴。</p>
          </div>
        </div>
        <div v-if="selected.transcript" class="transcript-box">{{ selected.transcript }}</div>
        <el-empty v-else description="尚未生成字幕" />
      </section>

      <section class="panel" v-if="selected">
        <MarkdownPanel title="AI 学习笔记" :content="selected.note" :source="selected.noteSource"
                       empty-text="生成字幕后，可继续生成结构化学习笔记。" />
      </section>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppHeader from '../components/AppHeader.vue'
import MarkdownPanel from '../components/MarkdownPanel.vue'
import http from '../api/http.js'

const router = useRouter()
const videos = ref([])
const selected = ref(null)
const loading = ref(false)
const uploading = ref(false)
const transcribing = ref(false)
const noting = ref(false)
const uploadTitle = ref('')
const uploadFile = ref(null)
const uploadRef = ref(null)

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

function statusLabel(status) {
  return ({ UPLOADED: '待转写', TRANSCRIBING: '转写中', TRANSCRIBED: '已有字幕', NOTE_READY: '笔记已生成', FAILED: '处理失败' })[status] || status
}

async function load() {
  loading.value = true
  try {
    videos.value = await http.get('/videos')
    if (!selected.value && videos.value.length) {
      selected.value = videos.value[0]
    } else if (selected.value) {
      selected.value = videos.value.find((item) => item.id === selected.value.id) || videos.value[0] || null
    }
  } finally {
    loading.value = false
  }
}

function onFileChange(file) {
  const name = (file.name || '').toLowerCase()
  if (!name.endsWith('.mp4') && !name.endsWith('.webm')) {
    ElMessage.error('请选择 MP4 或 WebM 视频')
    uploadFile.value = null
    return
  }
  uploadFile.value = file.raw
}

async function submitUpload() {
  if (!uploadTitle.value.trim() || !uploadFile.value) return
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('title', uploadTitle.value.trim())
    formData.append('file', uploadFile.value)
    const uploaded = await http.post('/videos', formData)
    uploadTitle.value = ''
    uploadFile.value = null
    uploadRef.value?.clearFiles()
    ElMessage.success('视频已上传')
    await load()
    selected.value = videos.value.find((item) => item.id === uploaded.id) || uploaded
  } finally {
    uploading.value = false
  }
}

function replaceVideo(updated) {
  selected.value = updated
  const index = videos.value.findIndex((item) => item.id === updated.id)
  if (index >= 0) videos.value[index] = updated
}

async function transcribe() {
  if (!selected.value) return
  transcribing.value = true
  try {
    const updated = await http.post(`/ai/transcribe/${selected.value.id}`)
    replaceVideo(updated)
    ElMessage.success(updated.transcriptSource === 'MOCK' ? '已生成演示兜底字幕' : '字幕已生成')
  } finally {
    transcribing.value = false
  }
}

async function generateNote() {
  if (!selected.value) return
  noting.value = true
  try {
    const updated = await http.post(`/ai/note/${selected.value.id}`)
    replaceVideo(updated)
    ElMessage.success(updated.noteSource === 'MOCK' ? '已生成演示兜底笔记' : '学习笔记已生成')
  } finally {
    noting.value = false
  }
}

onMounted(load)
</script>