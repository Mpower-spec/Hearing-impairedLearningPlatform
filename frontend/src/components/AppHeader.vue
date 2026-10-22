<template>
  <header class="app-header" :class="roleClass">
    <div class="brand">
      <div class="brand-mark" aria-hidden="true">声</div>
      <div>
        <router-link :to="home" class="brand-title">声学无碍学习平台</router-link>
        <p class="brand-subtitle">字幕优先 · 课堂同步 · 课后复习</p>
      </div>
    </div>
    <div class="header-actions">
      <SignAvatarControls
        v-if="session.user?.role === 'STUDENT'"
        :ready="avatarState.ready"
        :hidden="avatarState.hidden"
        :playing="avatarState.playing"
        :has-error="avatarState.hasError"
        @replay="replayAvatar"
        @toggle="toggleAvatar"
        @retry="retryAvatar"
      />
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
import SignAvatarControls from './SignAvatarControls.vue'
import { avatarState, replayAvatar, retryAvatar, toggleAvatar } from '../avatar/avatarStore.js'

const route = useRoute()
const router = useRouter()
const home = computed(() => homeForRole())
const roleLabel = computed(() => session.user?.role === 'TEACHER' ? '教师' : '学生')
const roleClass = computed(() => session.user?.role === 'TEACHER' ? 'app-header--teacher' : 'app-header--student')

function logout() {
  clearSession()
  router.push('/login')
}
</script>

<style scoped>
.app-header--teacher {
  --header-glow-left: rgba(37, 161, 142, 0.18);
  --header-glow-right: rgba(240, 180, 41, 0.12);
  --header-line-start: #084f70;
  --header-line-middle: #25a18e;
  --header-line-end: #f0b429;
}
.app-header--student {
  --header-glow-left: rgba(11, 110, 153, 0.15);
  --header-glow-right: rgba(37, 161, 142, 0.17);
  --header-line-start: #0b6e99;
  --header-line-middle: #35b5a5;
  --header-line-end: #f6c453;
}
.app-header--teacher .user-chip,
.app-header--student .user-chip {
  border: 1px solid rgba(11, 110, 153, 0.12);
  box-shadow: 0 4px 12px rgba(16, 42, 67, 0.06);
}
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
.header-actions :deep(.sign-avatar-controls) {
  padding: 7px 9px;
  border-radius: 11px;
}
.header-actions :deep(.sign-avatar-status small) { display: none; }
.header-actions :deep(.sign-avatar-status strong) { white-space: nowrap; }
@media (max-width: 900px) {
  .header-actions :deep(.sign-avatar-controls) { width: 100%; }
}
</style>
