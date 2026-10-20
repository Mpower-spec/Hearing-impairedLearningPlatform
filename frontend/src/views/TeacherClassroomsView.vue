<template>
  <div>
    <AppHeader>
      <el-button @click="router.push('/teacher/lessons')">课程与课件</el-button>
    </AppHeader>

    <main class="page-shell classroom-list-page">
      <div class="page-title-row">
        <div>
          <span class="page-kicker">LIVE CLASSROOM</span>
          <h1 class="page-title">实时课堂</h1>
          <p class="page-subtitle">查看进行中和历史课堂，进入课堂继续同步 PPT、字幕与弹幕。</p>
        </div>
        <div class="actions-row">
          <el-button :loading="loading" @click="load">刷新</el-button>
          <el-button type="primary" @click="router.push('/teacher/lessons')">
            去课程列表开课
          </el-button>
        </div>
      </div>

      <section class="classroom-overview">
        <div class="overview-card overview-card--live">
          <span class="overview-label">进行中</span>
          <strong>{{ liveRooms.length }}</strong>
          <small>正在同步的课堂</small>
        </div>
        <div class="overview-card overview-card--ended">
          <span class="overview-label">已结束</span>
          <strong>{{ endedRooms.length }}</strong>
          <small>可回看的课堂记录</small>
        </div>
        <div class="overview-note">
          <el-icon><VideoCamera /></el-icon>
          <div>
            <strong>每次开课都会生成 6 位房间码</strong>
            <span>学生输入房间码加入，课堂结束后仍会保留记录。</span>
          </div>
        </div>
      </section>

      <section class="panel room-list-panel">
        <div class="list-toolbar">
          <div class="filter-tabs" role="tablist" aria-label="课堂筛选">
            <button v-for="tab in filters" :key="tab.value" :class="{ active: filter === tab.value }" @click="filter = tab.value">
              {{ tab.label }}
              <span>{{ countFor(tab.value) }}</span>
            </button>
          </div>
          <span class="muted">共 {{ filteredRooms.length }} 个课堂</span>
        </div>

        <div v-if="filteredRooms.length" class="room-card-grid">
          <article v-for="room in filteredRooms" :key="room.id" class="room-card" :class="`room-card--${room.status.toLowerCase()}`">
            <div class="room-card-top">
              <el-tag :type="room.status === 'LIVE' ? 'success' : 'info'" effect="dark">
                {{ room.status === 'LIVE' ? '进行中' : '已结束' }}
              </el-tag>
              <span class="room-card-time">{{ formatTime(room.createdAt) }}</span>
            </div>
            <div class="room-code-block">
              <span>房间码</span>
              <strong>{{ room.roomCode }}</strong>
            </div>
            <h2>{{ room.lessonTitle }}</h2>
            <div class="room-meta">
              <span>第 {{ room.currentPage }} / {{ room.pageCount }} 页</span>
              <span v-if="room.status === 'ENDED'">结束于 {{ formatTime(room.endedAt) }}</span>
              <span v-else>课堂保持同步</span>
            </div>
            <el-button :type="room.status === 'LIVE' ? 'primary' : 'default'" size="large" class="room-action" @click="router.push(`/teacher/classroom/${room.id}`)">
              {{ room.status === 'LIVE' ? '进入课堂' : '查看课堂记录' }}
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </article>
        </div>
        <el-empty v-else :description="filter === 'ALL' ? '还没有课堂记录，先去课程列表开课吧' : '当前筛选下没有课堂'" />
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, VideoCamera } from '@element-plus/icons-vue'
import AppHeader from '../components/AppHeader.vue'
import http from '../api/http.js'

const router = useRouter()
const loading = ref(false)
const rooms = ref([])
const filter = ref('ALL')
const filters = [
  { value: 'ALL', label: '全部' },
  { value: 'LIVE', label: '进行中' },
  { value: 'ENDED', label: '已结束' }
]

const liveRooms = computed(() => rooms.value.filter((room) => room.status === 'LIVE'))
const endedRooms = computed(() => rooms.value.filter((room) => room.status === 'ENDED'))
const filteredRooms = computed(() => filter.value === 'ALL' ? rooms.value : rooms.value.filter((room) => room.status === filter.value))

function countFor(value) {
  if (value === 'LIVE') return liveRooms.value.length
  if (value === 'ENDED') return endedRooms.value.length
  return rooms.value.length
}

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

async function load() {
  loading.value = true
  try {
    rooms.value = await http.get('/rooms/mine')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.classroom-list-page { padding-bottom: 34px; }
.page-kicker { color: var(--primary); font-size: 12px; font-weight: 900; letter-spacing: 0.16em; }
.classroom-overview { display: grid; grid-template-columns: 180px 180px minmax(0, 1fr); gap: 14px; margin-bottom: 18px; }
.overview-card { padding: 16px 18px; border-radius: 18px; border: 1px solid var(--line); background: #fff; box-shadow: 0 10px 25px rgba(16,42,67,.06); }
.overview-card strong { display: block; margin: 5px 0 2px; font-size: 34px; line-height: 1; }
.overview-card small { color: var(--ink-soft); }
.overview-card--live { border-color: #b9e3d7; background: linear-gradient(135deg, #f4fbf8, #e5f6f0); }
.overview-card--live strong { color: #1f8a70; }
.overview-card--ended { border-color: #d8e5ec; background: linear-gradient(135deg, #f8fbfc, #edf4f7); }
.overview-note { display: flex; align-items: center; gap: 16px; padding: 16px 20px; border-radius: 18px; color: #d7edf5; background: linear-gradient(135deg, #102a43, #0b6e99); box-shadow: 0 12px 30px rgba(11,110,153,.18); }
.overview-note .el-icon { font-size: 34px; color: #8de1d1; }
.overview-note strong, .overview-note span { display: block; }
.overview-note span { margin-top: 5px; font-size: 13px; opacity: .78; }
.room-list-panel { padding: 20px; }
.list-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 18px; }
.filter-tabs { display: flex; gap: 7px; padding: 4px; border-radius: 12px; background: #f1f5f8; }
.filter-tabs button { border: 0; padding: 8px 13px; border-radius: 9px; background: transparent; color: var(--ink-soft); cursor: pointer; font-weight: 700; }
.filter-tabs button span { margin-left: 4px; font-size: 11px; opacity: .7; }
.filter-tabs button.active { background: #fff; color: var(--primary-strong); box-shadow: 0 3px 12px rgba(16,42,67,.1); }
.room-card-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 14px; }
.room-card { display: flex; flex-direction: column; min-height: 280px; padding: 18px; border: 1px solid var(--line); border-radius: 18px; background: #fff; transition: transform .18s ease, box-shadow .18s ease; }
.room-card:hover { transform: translateY(-3px); box-shadow: 0 16px 32px rgba(16,42,67,.12); }
.room-card--live { border-color: #a9dcd0; }
.room-card-top { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.room-card-time { color: var(--ink-soft); font-size: 12px; }
.room-code-block { margin: 20px 0 8px; padding: 12px 15px; border-radius: 12px; background: #f3f8fa; }
.room-code-block span { display: block; color: var(--ink-soft); font-size: 11px; }
.room-code-block strong { display: block; margin-top: 2px; font-size: 30px; letter-spacing: .15em; color: var(--primary-strong); font-variant-numeric: tabular-nums; }
.room-card h2 { margin: 8px 0 0; font-size: 20px; }
.room-meta { display: flex; justify-content: space-between; gap: 10px; margin-top: 12px; color: var(--ink-soft); font-size: 13px; }
.room-action { margin-top: auto; width: 100%; }
@media (max-width: 900px) { .classroom-overview { grid-template-columns: 1fr 1fr; } .overview-note { grid-column: 1 / -1; } }
@media (max-width: 560px) { .classroom-overview { grid-template-columns: 1fr; } .list-toolbar { align-items: flex-start; flex-direction: column; } .room-card-grid { grid-template-columns: 1fr; } }
</style>