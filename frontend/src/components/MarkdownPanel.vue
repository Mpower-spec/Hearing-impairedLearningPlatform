<template>
  <section class="markdown-panel" :aria-label="title">
    <div class="panel-heading">
      <div>
        <h3>{{ title }}</h3>
        <p v-if="source === 'MOCK'" class="warning-text">当前内容为演示兜底结果，并非真实 AI 输出。</p>
      </div>
      <el-tag v-if="source" :type="source === 'MOCK' ? 'warning' : 'success'" effect="plain">
        {{ source === 'MOCK' ? '模拟结果' : 'AI 生成' }}
      </el-tag>
    </div>
    <div v-if="content" class="markdown-body" v-html="html"></div>
    <el-empty v-else :description="emptyText" />
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { renderMarkdown } from '../utils/markdown.js'

const props = defineProps({
  title: { type: String, default: '内容' },
  content: { type: String, default: '' },
  source: { type: String, default: '' },
  emptyText: { type: String, default: '暂无内容' }
})

const html = computed(() => renderMarkdown(props.content))
</script>