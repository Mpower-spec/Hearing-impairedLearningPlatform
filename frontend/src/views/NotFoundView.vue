<template>
  <main class="not-found-page">
    <div class="not-found-card">
      <span class="not-found-code">404</span>
      <h1>这一页还没有被看见。</h1>
      <p>链接可能已经移动，或者地址输入有误。回到首页继续了解声学无碍学习平台。</p>
      <RouterLink to="/">返回外部首页</RouterLink>
    </div>
  </main>
</template>

<style scoped>
.not-found-page { min-height: 100vh; display: grid; place-items: center; padding: 24px; background: #f4f1e8; color: #102a2b; font-family: "Noto Sans SC", "Microsoft YaHei", "PingFang SC", system-ui, sans-serif; }
.not-found-card { width: min(680px, 100%); padding: 48px; border: 1px solid #d3dbd4; border-radius: 24px; background: #fbfaf5; }
.not-found-code { color: #159ca6; font-family: "Cascadia Mono", monospace; font-size: 14px; font-weight: 700; letter-spacing: .08em; }
h1 { margin: 16px 0 0; font-size: 48px; line-height: 1; letter-spacing: -.045em; }
p { margin: 24px 0 0; color: #566865; font-size: 16px; line-height: 24px; }
a { display: inline-flex; margin-top: 32px; padding: 12px 16px; border-radius: 12px; color: #fff; background: #0a2c31; font-size: 16px; font-weight: 700; text-decoration: none; }
@media (max-width: 767px) { .not-found-card { padding: 32px 24px; } h1 { font-size: 36px; } }
</style>
