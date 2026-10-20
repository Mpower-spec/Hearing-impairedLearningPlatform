<template>
  <header class="app-header">
    <div class="brand">
      <div class="brand-mark" aria-hidden="true">声</div>
      <div>
        <router-link :to="home" class="brand-title">声学无碍学习平台</router-link>
        <p class="brand-subtitle">字幕优先 · 课堂同步 · 课后复习</p>
      </div>
    </div>
    <div class="header-actions">
      <slot />
      <span class="user-chip">
        {{ session.user?.name || session.user?.username }}
        <small>{{ roleLabel }}</small>
      </span>
      <el-button text @click="logout">退出登录</el-button>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { session, clearSession, homeForRole } from '../auth/session.js'

const router = useRouter()
const home = computed(() => homeForRole())
const roleLabel = computed(() => session.user?.role === 'TEACHER' ? '教师' : '学生')

function logout() {
  clearSession()
  router.push('/login')
}
</script>