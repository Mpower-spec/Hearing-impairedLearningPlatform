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
      <el-button v-if="route.path !== '/home'" class="header-home-button" @click="router.push('/home')">返回首页</el-button>
      <slot />
      <span class="user-chip">
        {{ session.user?.name || session.user?.username }}
        <small>{{ roleLabel }}</small>
      </span>
      <el-button class="header-logout-button" @click="logout">退出登录</el-button>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { session, clearSession, homeForRole } from '../auth/session.js'

const route = useRoute()
const router = useRouter()
const home = computed(() => homeForRole())
const roleLabel = computed(() => session.user?.role === 'TEACHER' ? '教师' : '学生')

function logout() {
  clearSession()
  router.push('/login')
}
</script>
<style scoped>
.header-home-button {
  --el-button-bg-color: #e7f4fa;
  --el-button-border-color: #c5e2ee;
  --el-button-text-color: #0b5f7d;
  --el-button-hover-bg-color: #d8eef7;
  --el-button-hover-border-color: #9fcfe2;
  --el-button-hover-text-color: #084f70;
  --el-button-active-bg-color: #c7e5f0;
  --el-button-active-border-color: #8ac3d9;
}
.header-logout-button {
  --el-button-bg-color: #fff0ed;
  --el-button-border-color: #f3cbc5;
  --el-button-text-color: #b42318;
  --el-button-hover-bg-color: #ffe2de;
  --el-button-hover-border-color: #eeaea5;
  --el-button-hover-text-color: #912018;
  --el-button-active-bg-color: #ffd4cd;
  --el-button-active-border-color: #e99b90;
}
</style>