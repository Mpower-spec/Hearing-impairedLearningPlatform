<template>
  <div>
    <AppHeader>
      <el-button @click="router.push('/teacher/lessons')">我的课程</el-button>
    </AppHeader>

    <main class="page-shell">
      <div class="page-title-row">
        <div>
          <h1 class="page-title">教师课程</h1>
          <p class="page-subtitle">上传 PPTX 后自动转换为 PDF，开课即可生成学生可加入的房间码。</p>
        </div>
        <el-button type="primary" size="large" @click="uploadVisible = true">上传新课程</el-button>
      </div>

      <section class="panel">
        <div class="panel-heading">
          <div>
            <h2>课程列表</h2>
            <p class="muted">共 {{ lessons.length }} 门课程</p>
          </div>
          <el-button :loading="loading" @click="load">刷新</el-button>
        </div>

        <el-table v-if="lessons.length" :data="lessons" stripe>
          <el-table-column prop="title" label="课程名称" min-width="220" />
          <el-table-column prop="pageCount" label="页数" width="90" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.status === 'READY' ? 'success' : 'info'">
                {{ row.status === 'READY' ? '可开课' : row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" min-width="170">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="270" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="router.push(`/teacher/prepare/${row.id}`)">备课</el-button>
              <el-button type="primary" plain :loading="creatingId === row.id" @click="openRoom(row)">
                开课
              </el-button>
              <el-button text type="danger" :loading="deletingId === row.id" @click="deleteLesson(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="还没有课程，先上传一份 PPTX 吧" />
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
        <el-button type="primary" :loading="uploading" :disabled="!uploadTitle.trim() || !uploadFile"
                   @click="submitUpload">
          上传并转换
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppHeader from '../components/AppHeader.vue'
import http from '../api/http.js'

const router = useRouter()
const lessons = ref([])
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