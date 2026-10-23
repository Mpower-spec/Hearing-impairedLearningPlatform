<template>
  <div>
    <AppHeader />

    <main class="page-shell student-join-page">
      <div class="join-page-content">
        <section class="join-card panel">
          <div class="panel-heading">
            <div>
              <h1 class="page-title">加入课堂</h1>
              <p class="page-subtitle">输入教师提供的 6 位房间码；已经加入过的课堂会保留在下方记录中。</p>
            </div>
          </div>

          <el-input v-model="code" class="join-code-input" size="large" maxlength="6" inputmode="numeric" placeholder="000000" aria-label="六位房间码" @input="normalizeCode" @keyup.enter="search" />
          <div class="actions-row join-search-actions">
            <el-button type="primary" size="large" :loading="searching" :disabled="code.length !== 6" @click="search">查找课堂</el-button>
            <span class="muted">请向任课教师获取房间码</span>
          </div>

          <div v-if="room" class="join-preview">
            <div class="panel-heading">
              <div>
                <h3>{{ room.lessonTitle }}</h3>
                <p class="muted">{{ room.pageCount }} 页 · 当前第 {{ room.currentPage }} 页</p>
              </div>
              <el-tag :type="room.status === 'LIVE' ? 'success' : 'info'" size="large">
                {{ room.status === 'LIVE' ? '进行中' : '已结束' }}
              </el-tag>
            </div>
            <p v-if="room.status === 'LIVE'" class="muted">加入后可查看同步幻灯片、大字号字幕，并发送弹幕。</p>
            <p v-else class="warning-text">该课堂已经结束。已加入的学生可以直接查看课堂记录和课后资料。</p>
            <el-button v-if="room.status === 'LIVE'" type="primary" size="large" :loading="enteringId === room.id" @click="enterRoom(room, room.roomCode)">
              进入课堂
            </el-button>
            <el-button v-else-if="joinedIds.has(room.id)" size="large" :loading="enteringId === room.id" @click="enterRoom(room, room.roomCode)">
              查看课堂记录
            </el-button>
          </div>
        </section>

        <section class="joined-panel panel" aria-labelledby="joined-title">
          <div class="panel-heading">
            <div>
              <span class="section-kicker">学习轨迹</span>
              <h2 id="joined-title">我加入的课堂</h2>
              <p class="muted">进行中的课堂可继续进入，已结束的课堂可回看字幕和学习资料。</p>
            </div>
            <el-button :loading="joinedLoading" @click="loadJoined">刷新记录</el-button>
          </div>

          <div v-if="joinedLoading && !joinedRooms.length" class="joined-loading">
            <el-icon class="is-loading"><Loading /></el-icon>
            正在读取课堂记录…
          </div>
          <div v-else-if="joinedRooms.length" class="joined-grid">
            <article v-for="item in joinedRooms" :key="item.id" class="joined-card" :class="`joined-card--${item.status.toLowerCase()}`">
              <div class="joined-card__top">
                <el-tag :type="item.status === 'LIVE' ? 'success' : 'info'" effect="plain">
                  {{ item.status === 'LIVE' ? '正在上课' : '已结束' }}
                </el-tag>
                <time>{{ formatDateTime(item.endedAt || item.createdAt) }}</time>
              </div>
              <h3>{{ item.lessonTitle }}</h3>
              <p class="joined-meta">房间码 {{ item.roomCode }} · {{ item.pageCount }} 页 · 第 {{ item.currentPage }} 页</p>
              <div class="joined-card__footer">
                <span>{{ item.status === 'LIVE' ? '无需重新输入邀请码' : '字幕和资料仍可查看' }}</span>
                <el-button :type="item.status === 'LIVE' ? 'primary' : 'default'" :loading="enteringId === item.id" @click="enterRoom(item, item.roomCode)">
                  {{ item.status === 'LIVE' ? '继续课堂' : '查看记录' }}
                </el-button>
              </div>
            </article>
          </div>
          <div v-else class="empty-note">还没有加入过课堂。成功进入一次课堂后，记录会显示在这里。</div>
        </section>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import AppHeader from '../components/AppHeader.vue'
import http from '../api/http.js'

const router = useRouter()
const code = ref('')
const room = ref(null)
const searching = ref(false)
const joinedRooms = ref([])
const joinedLoading = ref(false)
const enteringId = ref(null)
const joinedIds = computed(() => new Set(joinedRooms.value.map((item) => item.id)))

function normalizeCode(value) {
  code.value = String(value || '').replace(/\D/g, '').slice(0, 6)
  room.value = null
}

function formatDateTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ').slice(5, 16)
  return date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

async function loadJoined() {
  joinedLoading.value = true
  try {
    joinedRooms.value = await http.get('/rooms/joined', { silent: true })
  } catch {
    ElMessage.error('课堂记录加载失败，请稍后重试')
  } finally {
    joinedLoading.value = false
  }
}

async function search() {
  if (code.value.length !== 6) return
  searching.value = true
  try {
    room.value = await http.get(`/rooms/by-code/${code.value}`)
  } finally {
    searching.value = false
  }
}

async function enterRoom(target, roomCode) {
  if (!target || enteringId.value) return
  const targetCode = String(roomCode || code.value || '').trim()
  if (!targetCode) {
    ElMessage.error('缺少课堂邀请码，请重新输入')
    return
  }
  enteringId.value = target.id
  try {
    let destination = target
    if (target.status === 'LIVE') {
      destination = await http.post('/rooms/join', { roomCode: targetCode })
    }
    sessionStorage.setItem('hp_room_code', targetCode)
    await router.push({ path: `/student/classroom/${destination.id}`, query: { code: targetCode } })
  } finally {
    enteringId.value = null
  }
}

onMounted(loadJoined)
</script>
<style scoped>
.join-page-content { width: min(1040px, 100%); margin: 0 auto; display: flex; flex-direction: column; gap: 18px; }
.student-join-page .join-card { width: 100%; margin: 0; }
.join-search-actions { align-items: center; }
.section-kicker { display: inline-block; margin-bottom: 4px; color: var(--primary); font-size: 10px; font-weight: 900; letter-spacing: .14em; }
.joined-panel { overflow: hidden; }
.joined-panel .panel-heading h2 { margin: 0; font-size: clamp(20px, 2vw, 26px); }
.joined-loading { min-height: 140px; display: flex; align-items: center; justify-content: center; gap: 8px; color: var(--ink-soft); }
.joined-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.joined-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--reading-card-border);
  border-left: 5px solid var(--line-strong);
  border-radius: 16px;
  color: var(--reading-card-text);
  background: var(--reading-card-bg);
  box-shadow: var(--reading-card-shadow);
}
.joined-card--live { border-left-color: #169b78; }
.joined-card--ended { border-left-color: #7892a5; }
.joined-card__top { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.joined-card__top time { color: var(--ink-soft); font-size: 11px; }
.joined-card h3 { margin: 13px 0 0; color: var(--reading-card-text); font-size: 20px; line-height: 1.4; overflow-wrap: anywhere; }
.joined-meta { margin: 7px 0 0; color: var(--ink-soft); font-size: 12px; line-height: 1.55; }
.joined-card__footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: auto; padding-top: 18px; }
.joined-card__footer span { color: var(--ink-soft); font-size: 11px; }
.joined-card__footer .el-button { flex: 0 0 auto; }
@media (max-width: 767px) {
  .join-page-content { gap: 14px; }
  .join-search-actions { align-items: stretch; flex-direction: column; }
  .join-search-actions .el-button { width: 100%; }
  .joined-grid { grid-template-columns: 1fr; }
  .joined-panel .panel-heading { align-items: stretch; flex-direction: column; }
  .joined-panel .panel-heading > .el-button { width: 100%; }
  .joined-card__footer { align-items: stretch; flex-direction: column; }
  .joined-card__footer .el-button { width: 100%; }
}
</style>