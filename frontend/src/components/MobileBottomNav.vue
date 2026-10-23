<template>
  <nav class="mobile-bottom-nav" aria-label="学生端主导航">
    <router-link
      v-for="item in items"
      :key="item.path"
      :to="item.path"
      class="mobile-nav-item"
      :class="{ active: isActive(item) }"
      :aria-current="isActive(item) ? 'page' : undefined"
    >
      <el-icon><component :is="item.icon" /></el-icon>
      <span>{{ item.label }}</span>
    </router-link>
  </nav>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { ChatDotRound, HomeFilled, Monitor, VideoCamera } from '@element-plus/icons-vue'

const route = useRoute()
const items = [
  { label: '首页', path: '/home', icon: HomeFilled },
  { label: '课堂', path: '/student/join', icon: Monitor, match: ['/student/join', '/student/classroom'] },
  { label: '视频', path: '/student/videos', icon: VideoCamera },
  { label: '社区', path: '/community', icon: ChatDotRound }
]

function isActive(item) {
  return (item.match || [item.path]).some((path) => route.path === path || route.path.startsWith(`${path}/`))
}
</script>

<style scoped>
.mobile-bottom-nav { display: none; }
@media (max-width: 767px) {
  .mobile-bottom-nav {
    position: fixed;
    left: 50%;
    bottom: calc(8px + env(safe-area-inset-bottom));
    z-index: 1200;
    width: min(520px, calc(100% - 18px));
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 4px;
    padding: 7px;
    transform: translateX(-50%);
    border: 1px solid rgba(122, 170, 181, 0.35);
    border-radius: 18px;
    background: linear-gradient(145deg, rgba(250, 254, 253, 0.97), rgba(225, 243, 242, 0.96));
    box-shadow: 0 12px 32px rgba(16, 42, 67, 0.18);
    backdrop-filter: blur(16px) saturate(125%);
  }
  .mobile-nav-item {
    min-height: 48px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-direction: column;
    gap: 3px;
    border-radius: 13px;
    color: var(--ink-soft);
    font-size: 11px;
    font-weight: 700;
    text-decoration: none;
  }
  .mobile-nav-item .el-icon { font-size: 21px; }
  .mobile-nav-item.active {
    color: var(--primary-strong);
    background: linear-gradient(135deg, rgba(11, 110, 153, 0.12), rgba(37, 161, 142, 0.14));
  }
  .mobile-nav-item:active { transform: scale(0.97); }
}
</style>
