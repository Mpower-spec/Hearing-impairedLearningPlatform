<template>
  <div>
    <AppHeader />

    <main class="page-shell teacher-command-page">
      <section class="command-hero">
        <div class="command-hero-copy">
          <span class="command-kicker"><span class="signal-dot"></span> LIVE CLASSROOM CONTROL</span>
          <h1 class="page-title">课堂指挥台</h1>
          <p class="page-subtitle">查看正在同步的课堂、房间码与历史记录，快速进入教学现场。</p>
          <div class="command-hero-actions">
            <el-button type="primary" size="large" @click="router.push('/teacher/lessons')">创建新课堂</el-button>
            <el-button size="large" :loading="loading" @click="load">刷新状态</el-button>
          </div>
        </div>
        <div class="command-metrics">
          <div class="command-metric command-metric--live">
            <span>LIVE NOW</span><strong>{{ liveRooms.length }}</strong><small>进行中</small>
            <div class="command-signal"><i v-for="bar in 9" :key="bar" :style="{ '--bar': bar }"></i></div>
          </div>
          <div class="command-metric command-metric--ended">
            <span>ARCHIVE</span><strong>{{ endedRooms.length }}</strong><small>已结束</small>
          </div>
        </div>
      </section>

      <section class="command-console">
        <aside class="command-sidebar">
          <span class="command-sidebar-label">SESSIONS</span>
          <button v-for="tab in filters" :key="tab.value" class="command-filter" :class="{ active: filter === tab.value }" @click="filter = tab.value">
            <span>{{ tab.label }}</span><strong>{{ countFor(tab.value) }}</strong>
          </button>
          <div class="command-tip">
            <el-icon><VideoCamera /></el-icon>
            <p>每次开课都会生成新的 6 位房间码，结束后仍保留课堂记录。</p>
          </div>
        </aside>

        <div class="command-stage">
          <div class="command-stage-heading">
            <div><span class="section-kicker">课堂列表</span><h2>{{ filters.find((tab) => tab.value === filter)?.label }}课堂</h2></div>
            <span>共 {{ filteredRooms.length }} 个课堂</span>
          </div>

          <div v-if="filteredRooms.length" class="command-session-list">
            <article v-for="room in filteredRooms" :key="room.id" class="command-session" :class="`command-session--${room.status.toLowerCase()}`">
              <div class="command-session-status">
                <span class="command-session-dot"></span>
                <span>{{ room.status === 'LIVE' ? '正在同步' : '已归档' }}</span>
              </div>
              <div class="command-session-title">
                <h2>{{ room.lessonTitle }}</h2>
                <p>创建于 {{ formatTime(room.createdAt) }}<span v-if="room.status === 'ENDED'"> · 结束于 {{ formatTime(room.endedAt) }}</span></p>
              </div>
              <div class="command-session-code">
                <span>ROOM</span><strong>{{ room.roomCode }}</strong>
              </div>
              <div class="command-session-meta">
                <span>第 {{ room.currentPage }} / {{ room.pageCount }} 页</span>
                <span>{{ room.status === 'LIVE' ? '字幕同步中' : '记录可回看' }}</span>
              </div>
              <el-button :type="room.status === 'LIVE' ? 'primary' : 'default'" class="command-session-action" @click="router.push(`/teacher/classroom/${room.id}`)">
                {{ room.status === 'LIVE' ? '进入课堂' : '查看记录' }}
                <el-icon><ArrowRight /></el-icon>
              </el-button>
            </article>
          </div>
          <el-empty v-else :description="filter === 'ALL' ? '还没有课堂记录，先去课程库创建课堂吧' : '当前筛选下没有课堂'" />
        </div>
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
