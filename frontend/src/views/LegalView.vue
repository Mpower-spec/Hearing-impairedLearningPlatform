<template>
  <main class="legal-page">
    <div class="legal-shell">
      <RouterLink class="legal-back" to="/">返回首页</RouterLink>
      <p class="legal-kicker">声学无碍学习平台</p>
      <h1>{{ current.title }}</h1>
      <p class="legal-updated">平台说明 · 更新于 2026 年 10 月 29 日</p>
      <section v-for="section in current.sections" :key="section.title" class="legal-section">
        <h2>{{ section.title }}</h2>
        <p>{{ section.body }}</p>
      </section>
      <RouterLink class="legal-action" to="/login">登录进入课堂</RouterLink>
    </div>
  </main>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const documents = {
  privacy: {
    title: '隐私说明',
    sections: [
      { title: '数据使用', body: '当前外部首页展示产品功能。预置账号、课堂内容和学习资料仅用于功能验证，不应录入真实个人敏感信息。' },
      { title: '本地存储', body: '登录状态保存在浏览器 localStorage 中，用于识别当前账号。退出登录后会清除该浏览器的登录状态。' },
      { title: '课堂内容', body: '在当前环境中，上传的课件、字幕和视频由本地服务处理。正式上线前需要补充数据保留周期、删除机制和访问权限说明。' },
      { title: '联系与更新', body: '本说明会随产品能力变化更新。正式发布前应由项目负责人完成合规审核。' }
    ]
  },
  terms: {
    title: '使用条款',
    sections: [
      { title: '使用范围', body: '本平台用于无障碍课堂流程体验，不承诺生产级可用性、数据备份或第三方服务连续性。' },
      { title: '账号使用', body: '请使用项目提供的预置账号访问教师端和学生端。不要尝试提交真实身份信息或未经授权的课程资料。' },
      { title: '内容责任', body: '课堂字幕和 AI 生成资料可能存在识别或理解误差，重要内容应由教师复核。手语数字人仅提供辅助表达，不替代专业翻译。' },
      { title: '可用性', body: '语音识别、视频转写和 AI 服务可能因浏览器、网络或上游服务状态而不可用，平台会尽量显示明确的降级状态。' }
    ]
  }
}
const fallback = documents.privacy
const current = computed(() => documents[route.params.doc] || fallback)
</script>

<style scoped>
.legal-page { min-height: 100vh; padding: 64px 24px; background: #f4f1e8; color: #102a2b; font-family: "Noto Sans SC", "Microsoft YaHei", "PingFang SC", system-ui, sans-serif; }
.legal-shell { width: min(760px, 100%); margin: 0 auto; }
.legal-back { display: inline-flex; padding: 8px 12px; border: 1px solid #d3dbd4; border-radius: 12px; color: #566865; background: #fbfaf5; font-size: 14px; text-decoration: none; }
.legal-kicker { margin: 48px 0 0; color: #159ca6; font-size: 14px; font-weight: 700; letter-spacing: .04em; }
h1 { margin: 16px 0 0; font-size: 48px; line-height: 1; letter-spacing: -.045em; }
.legal-updated { margin: 16px 0 0; color: #566865; font-size: 14px; }
.legal-section { margin-top: 40px; padding-top: 24px; border-top: 1px solid #d3dbd4; }
.legal-section h2 { margin: 0; font-size: 24px; line-height: 32px; }
.legal-section p { margin: 12px 0 0; color: #566865; font-size: 16px; line-height: 24px; }
.legal-action { display: inline-flex; margin-top: 48px; padding: 12px 16px; border-radius: 12px; color: #fff; background: #0a2c31; font-size: 16px; font-weight: 700; text-decoration: none; }
@media (max-width: 767px) { .legal-page { padding: 32px 16px; } h1 { font-size: 36px; } }
</style>
