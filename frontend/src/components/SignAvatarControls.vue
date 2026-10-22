<template>
  <div class="sign-avatar-controls" aria-live="polite">
    <div class="sign-avatar-status">
      <span class="sign-avatar-status-dot" :class="statusClass"></span>
      <div>
        <strong>{{ statusText }}</strong>
        <small v-if="hasError">手语教学暂不可用，请稍后重试</small>
        <small v-else>数字人可拖动调整位置，并根据当前页面字幕提供手语辅助</small>
      </div>
    </div>
    <div class="sign-avatar-actions">
      <el-button size="small" :disabled="!ready || hidden" @click="$emit('replay')">重播</el-button>
      <el-button v-if="hasError" size="small" @click="$emit('retry')">重试</el-button>
      <el-button size="small" type="primary" :disabled="!ready" @click="$emit('toggle')">
        {{ hidden ? '显示数字人' : '隐藏数字人' }}
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  ready: { type: Boolean, default: false },
  hidden: { type: Boolean, default: false },
  playing: { type: Boolean, default: false },
  hasError: { type: Boolean, default: false }
})
defineEmits(['replay', 'toggle', 'retry'])

const statusClass = computed(() => props.hasError ? 'error' : (props.ready ? 'ready' : 'loading'))
const statusText = computed(() => {
  if (props.hasError) return '手语教学加载失败'
  if (!props.ready) return '正在为你生成手语教学'
  if (props.hidden) return '数字人已隐藏'
  if (props.playing) return '正在用手语表达内容'
  return '手语教学已就绪'
})
</script>

<style scoped>
.sign-avatar-controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 11px 13px;
  border: 1px solid #c8dfe4;
  border-radius: 13px;
  background: linear-gradient(145deg, rgba(249, 254, 253, 0.97), rgba(232, 245, 244, 0.96));
}
.sign-avatar-status { display: flex; align-items: center; gap: 9px; min-width: 0; }
.sign-avatar-status strong, .sign-avatar-status small { display: block; }
.sign-avatar-status strong { color: var(--ink); font-size: 13px; }
.sign-avatar-status small { margin-top: 3px; color: var(--ink-soft); font-size: 11px; line-height: 1.45; }
.sign-avatar-status-dot { flex: 0 0 auto; width: 9px; height: 9px; border-radius: 50%; background: #f0b429; box-shadow: 0 0 0 4px rgba(240,180,41,.15); }
.sign-avatar-status-dot.ready { background: #22c55e; box-shadow: 0 0 0 4px rgba(34,197,94,.14); }
.sign-avatar-status-dot.error { background: #ef4444; box-shadow: 0 0 0 4px rgba(239,68,68,.14); }
.sign-avatar-actions { display: flex; align-items: center; gap: 5px; flex-wrap: wrap; justify-content: flex-end; }
.sign-avatar-actions .el-button + .el-button { margin-left: 0; }
@media (max-width: 640px) {
  .sign-avatar-controls { align-items: flex-start; flex-direction: column; }
  .sign-avatar-actions { justify-content: flex-start; }
}
</style>
