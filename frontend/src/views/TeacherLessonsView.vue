<template>
  <div>
    <AppHeader />

    <main class="page-shell teacher-library-page">
      <section class="library-hero">
        <div class="library-hero-copy">
          <span class="library-kicker">COURSE LIBRARY · TEACHING ASSETS</span>
          <h1 class="page-title">课程资料库</h1>
          <p class="page-subtitle">把每一份课件整理成可预览、可备课、可开课的教学资产。</p>
          <div class="library-hero-actions">
            <el-button type="primary" size="large" @click="uploadVisible = true">上传新课程</el-button>
            <el-button size="large" :loading="loading" @click="load">刷新资料库</el-button>
          </div>
        </div>
        <div class="library-metrics" aria-label="课程数据概览">
          <div class="library-metric library-metric--courses">
            <span>COURSES</span><strong>{{ lessons.length }}</strong><small>门课程</small>
          </div>
          <div class="library-metric library-metric--pages">
            <span>SLIDES</span><strong>{{ totalPages }}</strong><small>页课件</small>
          </div>
          <div class="library-metric library-metric--ready">
            <span>READY</span><strong>{{ readyCount }}</strong><small>份可开课</small>
          </div>
        </div>
      </section>

      <section class="library-shelf">
        <div class="library-shelf-heading">
          <div>
            <span class="section-kicker">资料架</span>
            <h2>全部课程</h2>
            <p>选择一份课件开始备课，或直接创建实时课堂。</p>
          </div>
          <span class="library-shelf-count">{{ lessons.length }} 份资料</span>
        </div>

        <div v-if="lessons.length" class="library-grid">
          <article v-for="(lesson, index) in lessons" :key="lesson.id" class="library-card">
            <div class="library-cover" :class="`library-cover--${index % 4}`">
              <span>COURSE {{ String(index + 1).padStart(2, '0') }}</span>
              <strong>{{ lesson.pageCount }}</strong>
              <small>PAGES</small>
              <div class="library-cover-line"></div>
            </div>
            <div class="library-card-body">
              <div class="library-card-topline">
                <el-tag :type="lesson.status === 'READY' ? 'success' : 'info'" effect="plain">
                  {{ lesson.status === 'READY' ? 'PDF 已就绪' : lesson.status }}
                </el-tag>
                <time>{{ formatTime(lesson.createdAt) }}</time>
              </div>
              <h2>{{ lesson.title }}</h2>
              <div class="library-card-meta">
                <span>{{ lesson.pageCount }} 页</span>
                <span>可预览 PDF</span>
              </div>
              <div class="library-card-actions">
                <el-button type="primary" :loading="creatingId === lesson.id" @click="openRoom(lesson)">开课</el-button>
                <el-button @click="router.push(`/teacher/prepare/${lesson.id}`)">备课</el-button>
                <el-button text type="danger" :loading="deletingId === lesson.id" @click="deleteLesson(lesson)">删除</el-button>
              </div>
            </div>
          </article>
        </div>
        <div v-else class="library-empty">
          <div class="library-empty-mark">＋</div>
          <h2>资料架还是空的</h2>
          <p>上传第一份 PPTX，系统会自动转换为 PDF 并提取课程文字。</p>
          <el-button type="primary" size="large" @click="uploadVisible = true">上传第一份课程</el-button>
        </div>
      </section>
    </main>

    <el-dialog v-model="uploadVisible" title="上传 PPTX" width="520px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="课程名称">
          <el-input v-model="uploadTitle" maxlength="200" show-word-limit placeholder="例如：光合作用" />
        </el-form-item>
        <el-form-item label="PPTX 文件">
          <el-upload
            ref="uploadRef"
            drag
            :auto-upload="false"
            :limit="1"
            accept=".pptx"
            :on-change="onFileChange"
            :on-remove="() => (uploadFile = null)"
            style="width: 100%"
          >
            <div style="padding: 22px; text-align: center">
              <div style="font-size: 38px; margin-bottom: 8px">＋</div>
              <div>拖入 .pptx，或点击选择文件</div>
              <small class="muted">最大 50MB，需安装 LibreOffice 才能转换</small>
            </div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" :disabled="!uploadTitle.trim() || !uploadFile" @click="submitUpload">
          上传并转换
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppHeader from '../components/AppHeader.vue'
import http from '../api/http.js'

const router = useRouter()
const lessons = ref([])
const totalPages = computed(() => lessons.value.reduce((sum, lesson) => sum + (lesson.pageCount || 0), 0))
const readyCount = computed(() => lessons.value.filter((lesson) => lesson.status === 'READY').length)
const loading = ref(false)
const uploading = ref(false)
const creatingId = ref(null)
const deletingId = ref(null)
const uploadVisible = ref(false)
const uploadTitle = ref('')
const uploadFile = ref(null)
const uploadRef = ref(null)

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

async function load() {
  loading.value = true
  try {
    lessons.value = await http.get('/lessons')
  } finally {
    loading.value = false
  }
}

function onFileChange(file) {
  const name = file.name || ''
  if (!name.toLowerCase().endsWith('.pptx')) {
    ElMessage.error('请选择 .pptx 文件')
    uploadRef.value?.clearFiles()
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
    await http.post('/lessons', formData)
    ElMessage.success('课程已创建，PPT 转换完成')
    uploadVisible.value = false
    uploadTitle.value = ''
    uploadFile.value = null
    uploadRef.value?.clearFiles()
    await load()
  } finally {
    uploading.value = false
  }
}

async function openRoom(lesson) {
  creatingId.value = lesson.id
  try {
    const room = await http.post('/rooms', { lessonId: lesson.id })
    router.push(`/teacher/classroom/${room.id}`)
  } finally {
    creatingId.value = null
  }
}

async function deleteLesson(lesson) {
  try {
    await ElMessageBox.confirm(
      `确定删除课程“${lesson.title}”吗？课程文件、所有课堂记录、进行中的课堂、字幕、弹幕和 AI 资料都会一并删除，且无法恢复。`,
      '删除课程',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger'
      }
    )
  } catch {
    return
  }
  deletingId.value = lesson.id
  try {
    await http.delete(`/lessons/${lesson.id}`)
    ElMessage.success('课程已删除')
    await load()
  } finally {
    deletingId.value = null
  }
}

onMounted(load)
</script>