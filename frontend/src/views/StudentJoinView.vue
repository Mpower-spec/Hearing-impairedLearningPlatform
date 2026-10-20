<template>
  <div>
    <AppHeader>
      <el-button @click="router.push('/student/join')">加入课堂</el-button>
      <el-button @click="router.push('/student/videos')">视频学习</el-button>
    </AppHeader>

    <main class="page-shell">
      <div class="join-card panel">
        <div class="panel-heading">
          <div>
            <h1 class="page-title" style="font-size: 30px">加入课堂</h1>
            <p class="page-subtitle">输入教师提供的 6 位房间码，即可同步查看幻灯片和实时字幕。</p>
          </div>
        </div>

        <el-input
          v-model="code"
          class="join-code-input"
          size="large"
          maxlength="6"
          inputmode="numeric"
          placeholder="000000"
          aria-label="六位房间码"
          @input="normalizeCode"
          @keyup.enter="search"
        />
        <div class="actions-row" style="margin-top: 16px">
          <el-button type="primary" size="large" :loading="searching" :disabled="code.length !== 6" @click="search">
            查找课堂
          </el-button>
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
          <p v-if="room.status === 'LIVE'" class="muted">
            加入后可查看同步幻灯片、大字号字幕，并发送弹幕。
          </p>
          <p v-else class="warning-text">该课堂已经结束。如已加入过，可以在原课堂页面查看学习资料。</p>
          <el-button v-if="room.status === 'LIVE'" type="primary" size="large" @click="join">
            进入课堂
          </el-button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '../components/AppHeader.vue'
import http from '../api/http.js'

const router = useRouter()
const code = ref('')
const room = ref(null)
const searching = ref(false)

function normalizeCode(value) {
  code.value = String(value || '').replace(/\D/g, '').slice(0, 6)
  room.value = null
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

function join() {
  sessionStorage.setItem('hp_room_code', code.value)
  router.push({ path: `/student/classroom/${room.value.id}`, query: { code: code.value } })
}
</script>