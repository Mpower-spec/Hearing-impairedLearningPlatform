<template>
  <div>
    <AppHeader />

    <main class="page-shell teacher-studio-page">
      <section class="studio-hero">
        <div class="studio-hero-copy">
          <span class="studio-kicker">AI LESSON STUDIO</span>
          <h1 class="page-title">备课工作室</h1>
          <p class="page-subtitle">从课件中选择一门课程，让 AI 帮你整理教学目标、重点、术语和练习。</p>
          <div class="studio-hero-actions">
            <el-button size="large" :loading="loading" @click="load">刷新课程</el-button>
            <el-button type="primary" size="large" @click="router.push('/teacher/lessons')">上传新课程</el-button>
          </div>
        </div>
        <div class="studio-orb" aria-hidden="true">
          <div class="studio-orb-ring"></div>
          <el-icon><MagicStick /></el-icon>
          <span>AI PREP</span>
          <div class="studio-orb-metrics">
            <strong>{{ lessons.length }}</strong><small>门课程</small>
            <strong>{{ totalPages }}</strong><small>页资料</small>
          </div>
        </div>
      </section>

      <section class="studio-workbench">
        <aside class="studio-rail">
          <span class="studio-rail-label">WORKFLOW</span>
          <div class="studio-step">
            <span>01</span><div><strong>选择课程</strong><small>从已上传课件中选择</small></div>
          </div>
          <div class="studio-step">
            <span>02</span><div><strong>预览内容</strong><small>检查 PDF 转换结果</small></div>
          </div>
          <div class="studio-step">
            <span>03</span><div><strong>生成资料</strong><small>输出目标、重点、术语与练习</small></div>
          </div>
          <div class="studio-rail-note">
            <el-icon><MagicStick /></el-icon>
            <p>备课资料依据 PPT 提取文字生成，可随时重新生成。</p>
          </div>
        </aside>

        <section class="studio-course-stage">
          <div class="studio-stage-heading">
            <div><span class="section-kicker">课程素材</span><h2>选择课程开始备课</h2></div>
            <span>{{ lessons.length }} 份可用课件</span>
          </div>

          <div v-if="lessons.length" class="studio-course-list">
            <article v-for="(lesson, index) in lessons" :key="lesson.id" class="studio-course-row">
              <div class="studio-course-index">{{ String(index + 1).padStart(2, '0') }}</div>
              <div class="studio-course-main">
                <span>COURSE {{ String(index + 1).padStart(2, '0') }}</span>
                <h2>{{ lesson.title }}</h2>
                <p>创建于 {{ formatTime(lesson.createdAt) }}</p>
              </div>
              <div class="studio-course-meta">
                <strong>{{ lesson.pageCount }}</strong><small>页课件</small>
                <el-tag :type="lesson.status === 'READY' ? 'success' : 'info'" effect="plain">
                  {{ lesson.status === 'READY' ? '可备课' : lesson.status }}
                </el-tag>
              </div>
              <el-button type="primary" class="studio-course-action" @click="router.push(`/teacher/prepare/${lesson.id}`)">
                进入备课 <el-icon><ArrowRight /></el-icon>
              </el-button>
            </article>
          </div>
          <el-empty v-else description="还没有课程，先上传一份 PPTX 吧" />
        </section>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, MagicStick } from '@element-plus/icons-vue'
import AppHeader from '../components/AppHeader.vue'
import http from '../api/http.js'

const router = useRouter()
const loading = ref(false)
const lessons = ref([])
const totalPages = computed(() => lessons.value.reduce((sum, lesson) => sum + (lesson.pageCount || 0), 0))

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

async function load() {
  loading.value = true
  try {
    lessons.value = await http.get('/lessons')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.prepare-center-page { padding-bottom: 34px; }
.page-kicker { color: var(--primary); font-size: 12px; font-weight: 900; letter-spacing: 0.16em; }
.prepare-process { display: flex; align-items: stretch; gap: 10px; margin-bottom: 18px; padding: 16px 18px; border: 1px solid var(--line-strong); border-radius: 20px; color: var(--ink); background: radial-gradient(circle at 0% 0%, rgba(37,161,142,.1), transparent 22rem), linear-gradient(135deg, rgba(250,254,253,.97), rgba(232,246,245,.95)); box-shadow: var(--shadow-soft); }
.process-step { flex: 1; min-width: 0; padding: 12px 14px; border: 1px solid rgba(159,199,207,.5); border-radius: 14px; background: rgba(255,255,255,.54); }
.process-step span { display: inline-grid; place-items: center; width: 30px; height: 30px; margin-bottom: 9px; border-radius: 50%; color: #ffffff; background: linear-gradient(135deg, #0b6e99, #25a18e); box-shadow: 0 5px 12px rgba(11,110,153,.2); font-size: 11px; font-weight: 900; }
.process-step strong, .process-step small { display: block; }
.process-step strong { color: var(--ink); font-size: 15px; }
.process-step small { margin-top: 4px; color: var(--ink-soft); font-size: 12px; line-height: 1.5; }
.process-line { flex: 0 0 32px; align-self: center; width: 32px; height: 2px; border-radius: 99px; background: linear-gradient(90deg, #74bcbf, #f0b429); }
.prepare-summary { display: grid; grid-template-columns: 150px 150px minmax(0, 1fr); gap: 14px; margin-bottom: 18px; }
.prepare-summary > div { padding: 14px 18px; border: 1px solid var(--line-strong); border-radius: 16px; background: linear-gradient(145deg, rgba(251,254,253,.98), rgba(235,247,245,.95)); box-shadow: var(--shadow-soft); }
.prepare-summary span { display: block; color: var(--ink-soft); font-size: 12px; }
.prepare-summary strong { display: block; margin-top: 4px; font-size: 28px; }
.prepare-summary-note { display: flex; align-items: center; gap: 12px; color: var(--primary-strong); background: #eaf4f9 !important; border-color: #c6e2ee !important; }
.prepare-summary-note .el-icon { font-size: 26px; }
.prepare-summary-note span { color: var(--primary-strong); line-height: 1.55; }
.prepare-list-panel { padding: 20px; }
.prepare-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(330px, 1fr)); gap: 16px; }
.prepare-card { display: grid; grid-template-columns: 120px minmax(0, 1fr); min-height: 230px; overflow: hidden; border: 1px solid var(--line-strong); border-radius: 18px; background: linear-gradient(145deg, rgba(251,254,253,.98), rgba(235,247,245,.95)); transition: transform .18s ease, box-shadow .18s ease, border-color .18s ease; }
.prepare-card:hover { transform: translateY(-3px); border-color: #8ebfc9; box-shadow: var(--shadow-raised); }
.prepare-cover { display: flex; flex-direction: column; justify-content: center; padding: 18px; color: #fff; background: linear-gradient(150deg, #0b6e99, #25a18e); }
.prepare-cover--1 { background: linear-gradient(150deg, #264653, #2a9d8f); }
.prepare-cover--2 { background: linear-gradient(150deg, #7a5200, #d9941a); }
.cover-label { font-size: 9px; font-weight: 900; letter-spacing: .1em; opacity: .7; }
.prepare-cover strong { margin-top: 16px; font-size: 40px; line-height: 1; }
.prepare-cover small { margin-top: 4px; font-size: 12px; opacity: .75; }
.cover-line { width: 28px; height: 3px; margin-top: 18px; border-radius: 99px; background: rgba(255,255,255,.6); }
.prepare-card-body { display: flex; flex-direction: column; padding: 18px; min-width: 0; }
.prepare-card-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 10px; }
.prepare-card-heading h3 { margin: 0; font-size: 20px; line-height: 1.4; word-break: break-word; }
.prepare-card-body p { margin: 8px 0 0; color: var(--ink-soft); font-size: 12px; }
.prepare-card-meta { display: flex; gap: 12px; margin-top: 16px; color: var(--ink-soft); font-size: 12px; }
.prepare-action { margin-top: auto; width: 100%; }
@media (max-width: 850px) {
  .prepare-process { align-items: stretch; flex-direction: column; }
  .process-line { flex: none; align-self: flex-start; width: 2px; height: 18px; margin-left: 28px; background: linear-gradient(#74bcbf, #f0b429); }
  .prepare-summary { grid-template-columns: 1fr 1fr; }
  .prepare-summary-note { grid-column: 1 / -1; }
}
@media (max-width: 560px) { .prepare-summary { grid-template-columns: 1fr; } .prepare-summary-note { grid-column: auto; } .prepare-grid { grid-template-columns: 1fr; } .prepare-card { grid-template-columns: 95px minmax(0, 1fr); } }
</style>
