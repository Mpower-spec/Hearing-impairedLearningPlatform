<template>
  <div>
    <AppHeader>
      <el-button @click="router.push('/teacher/lessons')">返回课程</el-button>
    </AppHeader>

    <main class="page-shell" v-loading="loading">
      <div class="page-title-row">
        <div>
          <h1 class="page-title">{{ lesson?.title || '课程备课' }}</h1>
          <p class="page-subtitle">预览转换后的 PDF，并根据 PPT 文字生成备课资料。</p>
        </div>
        <div class="actions-row">
          <el-button type="primary" :loading="generating" :disabled="!lesson" @click="generate">
            {{ material ? '重新生成备课资料' : '生成备课资料' }}
          </el-button>
        </div>
      </div>

      <div class="classroom-grid">
        <section class="panel">
          <div class="panel-heading">
            <div>
              <h2>幻灯片预览</h2>
              <p class="muted">第 {{ page }} / {{ lesson?.pageCount || 0 }} 页</p>
            </div>
          </div>
          <PdfStage :source="lesson?.pdfUrl || ''" :page="page" empty-text="PDF 尚未就绪" />
          <div class="actions-row" style="margin-top: 16px">
            <el-button :disabled="page <= 1" @click="page--">上一页</el-button>
            <el-button :disabled="page >= (lesson?.pageCount || 1)" @click="page++">下一页</el-button>
            <span class="spacer"></span>
            <span class="muted">页码仅用于备课预览，不影响课堂同步</span>
          </div>
        </section>

        <section class="panel">
          <MarkdownPanel
            title="AI 备课资料"
            :content="material?.content"
            :source="material?.source"
            empty-text="点击“生成备课资料”，系统会提取 PPT 文字并调用大模型。"
          />
        </section>
      </div>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppHeader from '../components/AppHeader.vue'
import PdfStage from '../components/PdfStage.vue'
import MarkdownPanel from '../components/MarkdownPanel.vue'
import http from '../api/http.js'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const generating = ref(false)
const lesson = ref(null)
const material = ref(null)
const page = ref(1)

async function load() {
  loading.value = true
  try {
    lesson.value = await http.get(`/lessons/${route.params.id}`)
    try {
      material.value = await http.get(`/materials/prepare/${route.params.id}`, { silent: true })
    } catch {
      material.value = null
    }
  } finally {
    loading.value = false
  }
}

async function generate() {
  generating.value = true
  try {
    material.value = await http.post(`/ai/prepare/${route.params.id}`)
    ElMessage.success(material.value.source === 'MOCK'
      ? '已生成演示兜底资料'
      : '备课资料已生成')
  } finally {
    generating.value = false
  }
}

onMounted(load)
</script>