<template>
  <div class="lp-home">
    <a class="lp-skip-link" href="#lp-main">跳到主要内容</a>

    <header class="lp-nav-shell">
      <nav class="lp-nav" aria-label="外部首页主导航">
        <RouterLink class="lp-brand" to="/" aria-label="声学无碍学习平台首页">
          <span class="lp-brand-mark" aria-hidden="true">
            <svg viewBox="0 0 32 32" fill="none"><path d="M8 18.5c2.6 0 2.6-5 5.2-5s2.6 7 5.2 7 2.6-4 5.6-4" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" /></svg>
          </span>
          <span class="lp-brand-copy"><strong>声学无碍</strong><small>学习平台</small></span>
        </RouterLink>

        <div class="lp-nav-links">
          <a href="#capabilities">产品能力</a>
          <a href="#flow">学习流程</a>
          <a href="#scenario">课堂场景</a>
          <a href="#faq">常见问题</a>
        </div>

        <RouterLink class="lp-login-button" :to="session.token ? '/home' : '/login'">
          {{ session.token ? '进入平台' : '登录' }}
          <svg viewBox="0 0 20 20" fill="none" aria-hidden="true"><path d="M4 10h11M11 6l4 4-4 4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" /></svg>
        </RouterLink>
      </nav>
    </header>

    <main id="lp-main">
      <section class="lp-hero" :class="{ 'is-ready': heroReady }" aria-labelledby="lp-hero-title">
        <div class="lp-container lp-hero-grid">
          <div class="lp-hero-copy">
            <p class="lp-eyebrow lp-enter" style="--enter-delay: 80ms"><span class="lp-signal-dot" aria-hidden="true"></span>无障碍学习 · 字幕优先</p>
            <h1 id="lp-hero-title" class="lp-enter" style="--enter-delay: 140ms">让课堂里的每一句话，<span>都成为看得见的知识。</span></h1>
            <p class="lp-hero-lede lp-enter" style="--enter-delay: 210ms">为听障学生设计的同步学习空间。教师讲解实时转成字幕，课件、手语辅助和课后资料留在同一个学习流程里。</p>
            <div class="lp-hero-actions lp-enter" style="--enter-delay: 280ms">
              <RouterLink class="lp-primary-button" :to="session.token ? '/home' : '/login'">{{ session.token ? '进入学习平台' : '登录进入课堂' }}<svg viewBox="0 0 20 20" fill="none" aria-hidden="true"><path d="M4 10h11M11 6l4 4-4 4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" /></svg></RouterLink>
              <span class="lp-avatar-note">实时手语数字人 · 3D 动作服务</span>
            </div>
            <ul class="lp-hero-points lp-enter" style="--enter-delay: 350ms" aria-label="平台核心能力"><li>实时字幕</li><li>课件同步</li><li>手语辅助</li><li>课后可复习</li></ul>
          </div>

          <div class="lp-hero-visual lp-enter" style="--enter-delay: 420ms">
            <span class="lp-visual-shape lp-visual-shape--mint" aria-hidden="true"></span>
            <span class="lp-visual-shape lp-visual-shape--amber" aria-hidden="true"></span>
            <div class="lp-avatar-frame" :class="{ 'is-ready': avatarState.ready, 'is-error': avatarState.hasError }">
              <div class="lp-avatar-chrome">
                <div class="lp-avatar-title">
                  <span class="lp-avatar-title-dot" aria-hidden="true"></span>
                  <strong>手语数字人</strong>
                  <span>实时表达</span>
                </div>
                <span class="lp-avatar-status-chip">
                  <i aria-hidden="true"></i>
                  {{ avatarState.playing ? 'PLAYING' : (avatarState.ready ? 'READY' : 'CONNECTING') }}
                </span>
                <button type="button" class="lp-avatar-replay" :disabled="!avatarState.ready" @click="replayAvatar">
                  <svg viewBox="0 0 20 20" fill="none" aria-hidden="true"><path d="M15.5 7.2A6 6 0 1 0 16 12" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/><path d="M15.5 3.8v3.7h-3.7" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg>
                  重播手语
                </button>
              </div>
              <div class="lp-avatar-canvas">
                <span class="lp-avatar-orbit lp-avatar-orbit--one" aria-hidden="true"></span>
                <span class="lp-avatar-orbit lp-avatar-orbit--two" aria-hidden="true"></span>
                <span class="lp-avatar-ground" aria-hidden="true"></span>
                <SignAvatarWidget
                  ref="avatarRef"
                  embedded
                  :avatar-size="4"
                  :initial-text="avatarText"
                  :sign-text="avatarText"
                  :auto-play="true"
                  @state-change="handleAvatarState"
                />
                <div v-if="!avatarState.ready" class="lp-avatar-state" :class="{ 'is-error': avatarState.hasError }" aria-live="polite">
                  <span class="lp-avatar-state-icon" aria-hidden="true">
                    <svg viewBox="0 0 32 32" fill="none"><path d="M7 19c3.2 0 3.2-6 6.4-6s3.2 8 6.4 8 3.2-6 6.4-6" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/></svg>
                  </span>
                  <strong>{{ avatarState.hasError ? '手语数字人暂不可用' : '正在连接手语数字人' }}</strong>
                  <p>{{ avatarState.hasError ? avatarState.error : '正在加载动作服务，请稍候' }}</p>
                  <button v-if="avatarState.hasError" type="button" @click="retryAvatar">重新连接</button>
                </div>
                <div class="lp-avatar-side-note" aria-hidden="true"><span>LIVE</span><strong>手语<br />同步</strong></div>
              </div>
              <div class="lp-avatar-caption">
                <span class="lp-avatar-caption-label">正在表达</span>
                <p>{{ avatarText }}</p>
              </div>
            </div>
            <div class="lp-float-note lp-float-note--top" aria-hidden="true"><span>动作状态</span><strong>实时</strong></div>
            <div class="lp-float-note lp-float-note--bottom" aria-hidden="true"><span>表达方式</span><strong>手语</strong></div>
          </div>
        </div>
      </section>

      <section class="lp-proof-strip" aria-label="产品能力摘要"><div class="lp-container lp-proof-grid">
        <article class="lp-proof-item lp-reveal"><span class="lp-proof-icon" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none"><rect x="3" y="5" width="18" height="14" rx="3" stroke="currentColor" stroke-width="1.8"/><path d="M6.5 10h7M6.5 14h11" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg></span><div><strong>字幕跟得上</strong><p>教师语音实时转为大字号字幕</p></div></article>
        <article class="lp-proof-item lp-reveal"><span class="lp-proof-icon" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none"><path d="M4 6.5h7v11H4zM13 6.5h7v11h-7z" stroke="currentColor" stroke-width="1.8"/><path d="M11 12h2" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg></span><div><strong>课件不脱节</strong><p>PPT 页码与学生端保持同步</p></div></article>
        <article class="lp-proof-item lp-reveal"><span class="lp-proof-icon" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none"><path d="M7 4.5h7l3 3v12H7z" stroke="currentColor" stroke-width="1.8"/><path d="M10 11h4M10 15h4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/><path d="m14 4.5 3 3h-3z" stroke="currentColor" stroke-width="1.8"/></svg></span><div><strong>复习有依据</strong><p>字幕、笔记与问答集中保留</p></div></article>
        <article class="lp-proof-item lp-reveal"><span class="lp-proof-icon" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="7" r="3" stroke="currentColor" stroke-width="1.8"/><path d="M6 18c.8-3 3-4.5 6-4.5s5.2 1.5 6 4.5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/><path d="M18.5 4.5c.8.8.8 2.2 0 3M20.5 2.5c1.8 1.8 1.8 4.2 0 6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg></span><div><strong>手语辅助</strong><p>关键字幕驱动数字人表达</p></div></article>
      </div></section>

      <section id="capabilities" class="lp-section lp-capabilities" aria-labelledby="capabilities-title">
        <div class="lp-container">
          <div class="lp-section-heading lp-reveal"><p class="lp-kicker">为真实课堂而设计</p><h2 id="capabilities-title">不是把声音放大，<br />而是让信息更清楚。</h2><p>把听觉信息转换为可阅读、可同步、可回看的课堂内容，让学习不再依赖当下是否听清。</p></div>
          <div class="lp-capability-grid">
            <article class="lp-capability-card lp-reveal">
              <div class="lp-capability-visual lp-caption-tile" aria-hidden="true">
                <div class="lp-tile-top"><span><i></i> 实时字幕</span><strong>LIVE</strong></div>
                <p>植物把光能转化为化学能。</p><span class="lp-caption-line"><i class="lp-anim"></i></span>
                <div class="lp-wave-bars"><i v-for="bar in 12" :key="bar" :style="{ '--bar': bar }"></i></div>
              </div>
              <div class="lp-card-copy"><span class="lp-card-index">01</span><h3>字幕成为课堂主线</h3><p>大字号、高对比度字幕优先呈现，弹幕与课堂状态也以文字表达。</p></div>
            </article>
            <article class="lp-capability-card lp-capability-card--tint lp-reveal">
              <div class="lp-capability-visual lp-sync-tile" aria-hidden="true">
                <div class="lp-slide-card"><span>03 / 12</span><strong>光合作用</strong><i></i><i></i></div>
                <span class="lp-sync-link lp-anim"><i></i></span>
                <div class="lp-follow-card"><span>学生端</span><strong>已同步第 3 页</strong><i></i></div>
              </div>
              <div class="lp-card-copy"><span class="lp-card-index">02</span><h3>课件与讲解保持同频</h3><p>教师切换页面后学生端自动跟随，字幕、课件和课堂进度始终对应。</p></div>
            </article>
            <article class="lp-capability-card lp-reveal">
              <div class="lp-capability-visual lp-review-tile" aria-hidden="true">
                <div class="lp-review-sheet"><span>课堂学习资料</span><strong>本节重点</strong><i></i><i></i><i></i><span class="lp-review-ai lp-anim">AI 已生成</span></div>
                <div class="lp-review-stack"></div>
              </div>
              <div class="lp-card-copy"><span class="lp-card-index">03</span><h3>课后复习有迹可循</h3><p>课堂字幕、学习笔记和问答记录继续保留，复习时不再从零开始。</p></div>
            </article>
          </div>
        </div>
      </section>

      <section class="lp-tagline" aria-label="平台理念"><div class="lp-container">
        <p class="lp-tagline-kicker">ACCESSIBLE LEARNING</p>
        <h2 ref="taglineRef" class="lp-tagline-copy" :class="{ 'is-lit': taglineLit }"><span>看见</span><span>每一句话，</span><span>接住</span><span>每一次理解。</span></h2>
      </div></section>

      <section id="flow" class="lp-section lp-flow" aria-labelledby="flow-title"><div class="lp-container">
        <div class="lp-section-heading lp-section-heading--split lp-reveal">
          <div><p class="lp-kicker">三步完成学习闭环</p><h2 id="flow-title">从进入课堂，<br />到留下可复习的资料。</h2></div>
          <p>教师和学生使用不同任务布局，但共享同一份课堂内容。每个环节都为无障碍阅读服务。</p>
        </div>
        <div class="lp-flow-grid">
          <article class="lp-flow-card lp-reveal"><span class="lp-flow-number">01</span><div class="lp-flow-icon" aria-hidden="true"><svg viewBox="0 0 32 32" fill="none"><rect x="4" y="6" width="24" height="20" rx="4" stroke="currentColor" stroke-width="2"/><path d="M10 12h8M10 17h12M10 22h6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg></div><h3>教师开始课堂</h3><p>上传课件并开启课堂房间，语音转成实时字幕，当前页码同步给学生。</p></article>
          <article class="lp-flow-card lp-flow-card--dark lp-reveal"><span class="lp-flow-number">02</span><div class="lp-flow-icon" aria-hidden="true"><svg viewBox="0 0 32 32" fill="none"><circle cx="16" cy="16" r="12" stroke="currentColor" stroke-width="2"/><path d="M12 16h8M16 12v8" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg></div><h3>学生同步学习</h3><p>输入房间码后跟随课件、字幕与手语辅助，用文字参与课堂交流。</p></article>
          <article class="lp-flow-card lp-reveal"><span class="lp-flow-number">03</span><div class="lp-flow-icon" aria-hidden="true"><svg viewBox="0 0 32 32" fill="none"><path d="M7 6h13l5 5v15H7z" stroke="currentColor" stroke-width="2"/><path d="M11 15h9M11 20h9" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg></div><h3>课后继续复习</h3><p>课堂字幕、AI 笔记和相关问答保留下来，随时回看，按自己的节奏理解。</p></article>
        </div>
      </div></section>

      <section id="scenario" class="lp-scenario" aria-labelledby="scenario-title"><div class="lp-container lp-scenario-grid">
        <div class="lp-scenario-copy lp-reveal"><p class="lp-kicker lp-kicker--light">课堂里的 40 分钟</p><h2 id="scenario-title">一句话出现，<br />理解不再落后半拍。</h2><p>课堂信息按时间顺序沉淀：教师讲到哪、课件翻到哪、关键概念是什么，学生都能在同一块屏幕里确认。</p><RouterLink class="lp-text-link" :to="session.token ? '/home' : '/login'">登录查看完整课堂<svg viewBox="0 0 20 20" fill="none" aria-hidden="true"><path d="M4 10h11M11 6l4 4-4 4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg></RouterLink></div>
        <div class="lp-timeline lp-reveal" aria-label="课堂时间线示例">
          <article class="lp-timeline-item is-current lp-anim"><span class="lp-timeline-time">09:42</span><div><strong>课件翻到第 3 页</strong><p>教师端与学生端同步更新</p></div><span class="lp-timeline-mark" aria-hidden="true"></span></article>
          <article class="lp-timeline-item"><span class="lp-timeline-time">09:43</span><div><strong>实时字幕出现</strong><p>“植物把光能转化为化学能。”</p></div><span class="lp-timeline-mark" aria-hidden="true"></span></article>
          <article class="lp-timeline-item"><span class="lp-timeline-time">09:44</span><div><strong>手语辅助播放</strong><p>关键概念由数字人同步表达</p></div><span class="lp-timeline-mark" aria-hidden="true"></span></article>
          <article class="lp-timeline-item"><span class="lp-timeline-time">09:46</span><div><strong>AI 笔记写入资料</strong><p>重点、术语与练习自动归档</p></div><span class="lp-timeline-mark" aria-hidden="true"></span></article>
        </div>
      </div></section>

      <section id="faq" class="lp-section lp-faq" aria-labelledby="faq-title"><div class="lp-container lp-faq-grid">
        <div class="lp-section-heading lp-reveal"><p class="lp-kicker">常见问题</p><h2 id="faq-title">先把疑问，<br />也说清楚。</h2><p>下面是一些常见问题。后续会根据产品更新继续补充。</p></div>
        <div class="lp-faq-list lp-reveal">
          <details><summary>字幕多久会出现？<span aria-hidden="true">+</span></summary><p>字幕由浏览器语音识别生成，在课堂环境中通常会在教师说完一句话后很快出现。网络质量、麦克风和浏览器版本会影响识别速度。</p></details>
          <details><summary>支持哪些课件格式？<span aria-hidden="true">+</span></summary><p>当前支持上传 PPTX 文件，并在服务端转换为 PDF 供课堂预览。课件文字也会用于备课资料和课堂问答。</p></details>
          <details><summary>听不到声音也能完整参与吗？<span aria-hidden="true">+</span></summary><p>可以。课堂以字幕为信息主线，同时提供文字弹幕、同步课件、手语辅助和课后资料，减少对声音的依赖。</p></details>
          <details><summary>手语数字人会替代人工翻译吗？<span aria-hidden="true">+</span></summary><p>不会。它用于关键内容的辅助表达，不能替代专业手语翻译。复杂语境仍需要人工支持。</p></details>
          <details><summary>课后资料是怎么生成的？<span aria-hidden="true">+</span></summary><p>平台根据课件文本、课堂字幕和学习内容生成结构化资料。AI 不可用时会明确显示降级内容，不把模拟结果伪装成真实回答。</p></details>
          <details><summary>需要安装额外软件吗？<span aria-hidden="true">+</span></summary><p>不需要。平台在浏览器中运行，推荐使用最新版 Chrome 或 Edge，以获得更稳定的语音识别和课堂同步体验。</p></details>
        </div>
      </div></section>

      <section class="lp-final-cta" aria-labelledby="cta-title"><div class="lp-container lp-final-card lp-reveal">
        <div><p class="lp-kicker">从这里开始</p><h2 id="cta-title">先从一堂能看懂的课开始。</h2><p>登录后可体验教师端与学生端的课堂同步、字幕和课后资料流程。</p></div>
        <RouterLink class="lp-primary-button lp-primary-button--light" :to="session.token ? '/home' : '/login'">{{ session.token ? '进入学习平台' : '登录进入课堂' }}<svg viewBox="0 0 20 20" fill="none" aria-hidden="true"><path d="M4 10h11M11 6l4 4-4 4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg></RouterLink>
      </div></section>
    </main>

    <footer class="lp-footer">
      <div class="lp-container lp-footer-grid">
        <div class="lp-footer-brand"><RouterLink class="lp-brand lp-brand--footer" to="/" aria-label="返回首页"><span class="lp-brand-mark" aria-hidden="true"><svg viewBox="0 0 32 32" fill="none"><path d="M8 18.5c2.6 0 2.6-5 5.2-5s2.6 7 5.2 7 2.6-4 5.6-4" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" /></svg></span><span class="lp-brand-copy"><strong>声学无碍</strong><small>学习平台</small></span></RouterLink><p>让每一句话都被看见，让每一次理解都有据可循。</p></div>
        <div class="lp-footer-links">
          <div><strong>产品</strong><a href="#capabilities">产品能力</a><a href="#flow">学习流程</a><a href="#scenario">课堂场景</a></div>
          <div><strong>支持</strong><a href="#faq">常见问题</a><RouterLink to="/login">登录平台</RouterLink></div>
          <div><strong>说明</strong><RouterLink to="/legal/privacy">隐私说明</RouterLink><RouterLink to="/legal/terms">使用条款</RouterLink></div>
        </div>
      </div>
      <div class="lp-container lp-footer-bottom"><span>© 2026 声学无碍学习平台</span><span>字幕优先 · 课堂同步 · 手语辅助</span></div>
    </footer>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import SignAvatarWidget from '../components/SignAvatarWidget.vue'
import { session } from '../auth/session.js'

const heroReady = ref(false)
const avatarRef = ref(null)
const avatarText = '欢迎使用声学无碍平台，皆在解决听障学生在学习中遇到所有问题。'
const avatarState = ref({ ready: false, hidden: false, playing: false, hasError: false, error: '' })
const taglineLit = ref(false)
const taglineRef = ref(null)
let revealObserver = null
let motionObserver = null
let taglineObserver = null

function setLandingClass(active) {
  document.documentElement.classList.toggle('lp-page-active', active)
  document.body.classList.toggle('lp-page-active', active)
}

function handleAvatarState(state) {
  avatarState.value = state
}

function replayAvatar() {
  avatarRef.value?.replay?.()
}

function retryAvatar() {
  avatarRef.value?.retry?.()
}

onMounted(async () => {
  setLandingClass(true)
  await nextTick()
  requestAnimationFrame(() => { heroReady.value = true })

  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    taglineLit.value = true
    document.querySelectorAll('.lp-reveal').forEach((el) => el.classList.add('is-visible'))
    document.querySelectorAll('.lp-capabilities, .lp-scenario').forEach((el) => el.classList.add('is-active'))
    return
  }

  revealObserver = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (!entry.isIntersecting) return
      entry.target.classList.add('is-visible')
      revealObserver.unobserve(entry.target)
    })
  }, { threshold: 0.16, rootMargin: '0px 0px -40px' })
  document.querySelectorAll('.lp-reveal').forEach((el) => revealObserver.observe(el))

  motionObserver = new IntersectionObserver((entries) => {
    entries.forEach((entry) => entry.target.classList.toggle('is-active', entry.isIntersecting))
  }, { threshold: 0.2 })
  document.querySelectorAll('.lp-capabilities, .lp-scenario').forEach((el) => motionObserver.observe(el))

  if (taglineRef.value) {
    taglineObserver = new IntersectionObserver((entries) => {
      if (entries.some((entry) => entry.isIntersecting)) {
        taglineLit.value = true
        taglineObserver?.disconnect()
      }
    }, { threshold: 0.45 })
    taglineObserver.observe(taglineRef.value)
  }
})

onBeforeUnmount(() => {
  setLandingClass(false)
  revealObserver?.disconnect()
  motionObserver?.disconnect()
  taglineObserver?.disconnect()
})
</script>

<style>
html.lp-page-active,
body.lp-page-active { background: #f4f1e8 !important; scroll-behavior: smooth; }
body.lp-page-active { color: #102a2b; }
.lp-home { --lp-bg: #f4f1e8; --lp-surface: #fbfaf5; --lp-surface-muted: #ecefe8; --lp-ink: #102a2b; --lp-muted: #566865; --lp-border: #d3dbd4; --lp-cyan: #159ca6; --lp-mint: #51b58d; --lp-mint-soft: #d9eee4; --lp-amber: #e2a625; --lp-dark: #0a2c31; --lp-ease: cubic-bezier(0.22, 1, 0.36, 1); min-height: 100vh; overflow: clip; background: var(--lp-bg); color: var(--lp-ink); font-family: "Noto Sans SC", "Microsoft YaHei", "PingFang SC", system-ui, sans-serif; -webkit-font-smoothing: antialiased; }
.lp-home *, .lp-home *::before, .lp-home *::after { box-sizing: border-box; }
.lp-home a { color: inherit; text-decoration: none; }
.lp-home a:hover { text-decoration: none; }
.lp-home button, .lp-home a, .lp-home summary { -webkit-tap-highlight-color: transparent; }
.lp-home :focus-visible { outline: 3px solid var(--lp-cyan); outline-offset: 4px; }
.lp-skip-link { position: fixed; top: 12px; left: 12px; z-index: 100; padding: 8px 12px; border-radius: 8px; color: #ffffff !important; background: var(--lp-dark); transform: translateY(-160%); transition: transform 200ms var(--lp-ease); }
.lp-skip-link:focus { transform: translateY(0); }
.lp-container { width: min(1320px, calc(100% - 48px)); margin-inline: auto; }
.lp-nav-shell { position: sticky; top: 16px; z-index: 40; width: min(1320px, calc(100% - 48px)); margin: 16px auto 0; }
.lp-nav { min-height: 72px; display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 32px; padding: 12px 16px 12px 20px; border: 1px solid rgba(16, 42, 43, 0.12); border-radius: 24px; background: rgba(251, 250, 245, 0.94); box-shadow: 0 16px 48px rgba(8, 44, 49, 0.1); backdrop-filter: blur(18px) saturate(130%); }
.lp-brand { display: inline-flex; align-items: center; gap: 12px; min-width: max-content; }
.lp-brand-mark { width: 40px; height: 40px; display: grid; place-items: center; border-radius: 12px; color: var(--lp-dark); background: var(--lp-mint-soft); box-shadow: inset 0 0 0 1px rgba(16, 42, 43, 0.08); }
.lp-brand-mark svg { width: 26px; height: 26px; }
.lp-brand-copy { display: flex; align-items: baseline; gap: 4px; letter-spacing: -0.02em; }
.lp-brand-copy strong { font-size: 18px; font-weight: 700; }
.lp-brand-copy small { color: var(--lp-muted); font-size: 14px; font-weight: 500; }
.lp-nav-links { display: flex; align-items: center; justify-content: center; gap: 32px; }
.lp-nav-links a { position: relative; padding: 8px 0; color: var(--lp-muted); font-size: 14px; font-weight: 600; transition: color 300ms var(--lp-ease); }
.lp-nav-links a::after { content: ""; position: absolute; left: 0; right: 0; bottom: 2px; height: 2px; border-radius: 999px; background: var(--lp-cyan); transform: scaleX(0); transform-origin: left; transition: transform 300ms var(--lp-ease); }
.lp-nav-links a:hover { color: var(--lp-ink); }
.lp-nav-links a:hover::after { transform: scaleX(1); }
.lp-home .lp-login-button, .lp-home .lp-primary-button { display: inline-flex; align-items: center; justify-content: center; gap: 8px; min-height: 48px; padding: 8px 16px; border: 1px solid transparent; border-radius: 12px; font-size: 16px; font-weight: 700; white-space: nowrap; transition: transform 300ms var(--lp-ease), background-color 300ms var(--lp-ease), color 300ms var(--lp-ease), border-color 300ms var(--lp-ease), box-shadow 300ms var(--lp-ease); }
.lp-home .lp-login-button { color: #062a26; background: #9fe2d0; border-color: #6fcbb4; box-shadow: 0 8px 20px rgba(44, 155, 115, 0.16); }
.lp-home .lp-login-button:hover, .lp-home .lp-primary-button:hover { transform: translateY(-2px); }
.lp-home .lp-login-button:hover { color: #041f1c; background: #b8efdf; border-color: #4fbaa0; box-shadow: 0 12px 28px rgba(44, 155, 115, 0.22); }
.lp-login-button svg, .lp-primary-button svg, .lp-text-link svg { width: 20px; height: 20px; flex: 0 0 auto; transition: transform 300ms var(--lp-ease); }
.lp-login-button:hover svg, .lp-primary-button:hover svg, .lp-text-link:hover svg { transform: translateX(4px); }
.lp-hero { position: relative; padding: 80px 0 64px; }
.lp-hero::before { content: ""; position: absolute; z-index: 0; top: 48px; left: max(24px, calc((100vw - 1320px) / 2 - 80px)); width: 160px; height: 160px; border: 1px solid rgba(21, 156, 166, 0.18); border-radius: 50%; pointer-events: none; }
.lp-hero::after { content: ""; position: absolute; z-index: 0; right: max(24px, calc((100vw - 1320px) / 2 - 120px)); bottom: 64px; width: 240px; height: 240px; border: 1px solid rgba(226, 166, 37, 0.24); border-radius: 50%; pointer-events: none; }
.lp-hero-grid { position: relative; z-index: 1; display: grid; grid-template-columns: minmax(0, 1.02fr) minmax(420px, 0.98fr); align-items: center; gap: 64px; }
.lp-hero-copy { max-width: 680px; }
.lp-eyebrow, .lp-kicker { display: inline-flex; align-items: center; gap: 8px; margin: 0; color: var(--lp-cyan); font-size: 14px; font-weight: 700; letter-spacing: 0.04em; }
.lp-signal-dot { position: relative; width: 8px; height: 8px; border-radius: 50%; background: var(--lp-mint); box-shadow: 0 0 0 5px rgba(81, 181, 141, 0.16); }
.lp-signal-dot::after { content: ""; position: absolute; inset: -4px; border: 1px solid rgba(81, 181, 141, 0.6); border-radius: 50%; animation: lp-pulse 2s var(--lp-ease) infinite; }
.lp-hero h1 { max-width: 680px; margin: 24px 0 0; font-size: 60px; line-height: 1; font-weight: 700; letter-spacing: -0.045em; text-wrap: balance; color: #102a2b; }
.lp-hero h1 span { display: block; margin-top: 8px; color: transparent; background: linear-gradient(90deg, #102a2b 0%, #5d716d 100%); -webkit-background-clip: text; background-clip: text; }
.lp-hero-lede { max-width: 680px; margin: 24px 0 0; color: var(--lp-muted); font-size: 18px; line-height: 28px; text-wrap: pretty; }
.lp-hero-actions { display: flex; align-items: center; gap: 16px; margin-top: 32px; flex-wrap: wrap; }
.lp-home .lp-primary-button { min-height: 56px; padding: 8px 24px; color: #ffffff; background: var(--lp-dark); box-shadow: 0 14px 32px rgba(10, 44, 49, 0.16); }
.lp-home .lp-primary-button:hover { color: #ffffff; background: #13454a; box-shadow: 0 18px 38px rgba(10, 44, 49, 0.22); }
.lp-avatar-note { color: var(--lp-muted); font-size: 14px; font-weight: 500; }
.lp-hero-points { display: flex; flex-wrap: wrap; gap: 8px; margin: 24px 0 0; padding: 0; list-style: none; }
.lp-hero-points li { padding: 4px 12px; border: 1px solid rgba(16, 42, 43, 0.12); border-radius: 999px; color: var(--lp-muted); background: var(--lp-surface); font-size: 14px; font-weight: 600; }
.lp-hero-visual { position: relative; min-width: 0; padding: 16px; }
.lp-visual-shape { position: absolute; z-index: -1; display: block; border-radius: 24px; }
.lp-visual-shape--mint { top: 0; right: 32px; width: 56%; height: 58%; background: var(--lp-mint-soft); transform: rotate(4deg); }
.lp-visual-shape--amber { left: 0; bottom: 16px; width: 46%; height: 44%; background: #f0d89d; transform: rotate(-4deg); }
.lp-avatar-frame { position: relative; z-index: 1; overflow: hidden; border: 1px solid rgba(16, 42, 43, 0.18); border-radius: 24px; background: var(--lp-dark); box-shadow: 0 32px 80px rgba(10, 44, 49, 0.24); }
.lp-avatar-chrome { min-height: 52px; display: flex; align-items: center; gap: 12px; padding: 8px 12px 8px 16px; border-bottom: 1px solid rgba(167, 199, 196, 0.12); }
.lp-avatar-title { display: flex; align-items: center; gap: 8px; margin-right: auto; color: #d9efea; }
.lp-avatar-title strong { font-size: 14px; font-weight: 700; }
.lp-avatar-title > span:last-child { color: #91b8b1; font-size: 12px; }
.lp-avatar-title-dot { width: 8px; height: 8px; border-radius: 50%; background: #75ddb1; box-shadow: 0 0 0 5px rgba(117, 221, 177, 0.12); animation: lp-avatar-pulse 2s var(--lp-ease) infinite; }
.lp-avatar-status-chip { display: inline-flex; align-items: center; gap: 6px; padding: 4px 8px; border-radius: 999px; color: #bce9dc; background: rgba(81, 181, 141, 0.13); font-family: "Geist Mono", "Cascadia Mono", monospace; font-size: 12px; font-weight: 700; letter-spacing: .04em; }
.lp-avatar-status-chip i { width: 6px; height: 6px; border-radius: 50%; background: #75ddb1; }
.lp-avatar-replay { display: inline-flex; align-items: center; gap: 4px; min-height: 32px; padding: 4px 8px; border: 1px solid rgba(167, 199, 196, 0.18); border-radius: 8px; color: #d9efea; background: transparent; font-size: 12px; font-weight: 700; cursor: pointer; transition: background-color 300ms var(--lp-ease), border-color 300ms var(--lp-ease), opacity 300ms var(--lp-ease); }
.lp-avatar-replay svg { width: 16px; height: 16px; }
.lp-avatar-replay:hover:not(:disabled) { border-color: rgba(142, 216, 196, 0.5); background: rgba(81, 181, 141, 0.12); }
.lp-avatar-replay:disabled { opacity: .38; cursor: wait; }
.lp-avatar-canvas { position: relative; height: 330px; overflow: hidden; isolation: isolate; background: #0e363b; }
.lp-avatar-canvas::before { content: ""; position: absolute; z-index: 0; left: 50%; top: 54%; width: 300px; height: 300px; border-radius: 50%; background: #14484b; transform: translate(-50%, -50%); }
.lp-avatar-canvas::after { content: ""; position: absolute; z-index: 0; left: 50%; bottom: 20px; width: 250px; height: 42px; border-radius: 50%; background: #08282c; transform: translateX(-50%); filter: blur(10px); }
.lp-avatar-orbit { position: absolute; z-index: 0; left: 50%; top: 52%; border: 1px solid rgba(142, 216, 196, 0.18); border-radius: 50%; transform: translate(-50%, -50%); }
.lp-avatar-orbit--one { width: 250px; height: 250px; animation: lp-avatar-orbit 10s linear infinite; }
.lp-avatar-orbit--two { width: 310px; height: 180px; border-style: dashed; animation: lp-avatar-orbit-reverse 14s linear infinite; }
.lp-avatar-ground { position: absolute; z-index: 0; left: 50%; bottom: 34px; width: 210px; height: 1px; background: rgba(142, 216, 196, 0.24); transform: translateX(-50%); }
.lp-avatar-frame.is-ready .lp-avatar-ground { animation: lp-avatar-scan 3.2s var(--lp-ease) infinite; }
.lp-avatar-side-note { position: absolute; z-index: 2; top: 20px; right: 16px; display: flex; flex-direction: column; align-items: flex-end; gap: 4px; color: #8ed8c4; pointer-events: none; }
.lp-avatar-side-note span { font-family: "Geist Mono", "Cascadia Mono", monospace; font-size: 10px; font-weight: 700; letter-spacing: .08em; }
.lp-avatar-side-note strong { color: rgba(255, 255, 255, 0.72); font-size: 16px; line-height: 18px; font-weight: 700; text-align: right; }
.lp-avatar-state { position: absolute; z-index: 4; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 24px; text-align: center; background: rgba(10, 44, 49, 0.94); }
.lp-avatar-state-icon { width: 64px; height: 64px; display: grid; place-items: center; border-radius: 24px; color: #8ed8c4; background: #174449; }
.lp-avatar-state-icon svg { width: 32px; height: 32px; }
.lp-avatar-state strong { margin-top: 16px; color: #ffffff; font-size: 16px; font-weight: 700; }
.lp-avatar-state p { max-width: 320px; margin: 8px 0 0; color: #9fc8c1; font-size: 14px; line-height: 20px; }
.lp-avatar-state button { margin-top: 16px; padding: 8px 12px; border: 0; border-radius: 8px; color: #0a2c31; background: #8ed8c4; font-size: 14px; font-weight: 700; cursor: pointer; }
.lp-avatar-state.is-error .lp-avatar-state-icon { color: #ffd0c7; background: #5b2f2d; }
.lp-avatar-caption { display: grid; grid-template-columns: auto 1fr; align-items: start; gap: 12px; min-height: 78px; padding: 12px 16px; border-top: 1px solid rgba(167, 199, 196, 0.12); background: #0d3035; }
.lp-avatar-caption-label { margin-top: 3px; padding: 4px 8px; border-radius: 999px; color: #bce9dc; background: rgba(81, 181, 141, 0.14); font-size: 12px; font-weight: 700; white-space: nowrap; }
.lp-avatar-caption p { margin: 0; color: #eaf7f3; font-size: 16px; line-height: 24px; text-wrap: pretty; }
.lp-avatar-frame.is-ready .lp-avatar-caption-label { animation: lp-avatar-badge 2.4s var(--lp-ease) infinite; }
.lp-float-note { position: absolute; z-index: 2; display: flex; flex-direction: column; gap: 4px; padding: 12px 16px; border: 1px solid rgba(16, 42, 43, 0.12); border-radius: 16px; background: var(--lp-surface); box-shadow: 0 16px 40px rgba(10, 44, 49, 0.14); }
.lp-float-note span { color: var(--lp-muted); font-size: 12px; font-weight: 600; }
.lp-float-note strong { color: var(--lp-ink); font-size: 16px; font-weight: 700; }
.lp-float-note--top { top: 0; left: 0; }
.lp-float-note--bottom { right: 0; bottom: 0; }
.lp-enter { opacity: 0; filter: blur(2px); transform: translateY(6px); transition: opacity 600ms var(--lp-ease), transform 800ms var(--lp-ease), filter 600ms var(--lp-ease); transition-delay: var(--enter-delay, 0ms); }
.lp-hero.is-ready .lp-enter { opacity: 1; filter: blur(0); transform: translateY(0); }
.lp-proof-strip { position: relative; z-index: 1; border-block: 1px solid rgba(16, 42, 43, 0.1); background: var(--lp-surface); }
.lp-proof-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 0; }
.lp-proof-item { display: flex; align-items: center; gap: 16px; min-height: 112px; padding: 24px; border-right: 1px solid rgba(16, 42, 43, 0.1); }
.lp-proof-item:last-child { border-right: 0; }
.lp-proof-icon { flex: 0 0 40px; width: 40px; height: 40px; display: grid; place-items: center; border-radius: 12px; color: var(--lp-cyan); background: #e2f1ef; }
.lp-proof-icon svg { width: 24px; height: 24px; }
.lp-proof-item strong { display: block; font-size: 16px; font-weight: 700; }
.lp-proof-item p { margin: 4px 0 0; color: var(--lp-muted); font-size: 14px; line-height: 20px; }
.lp-section { padding: 96px 0; }
.lp-section-heading { max-width: 680px; }
.lp-section-heading h2, .lp-scenario-copy h2, .lp-final-card h2 { margin: 16px 0 0; font-size: 48px; line-height: 1; font-weight: 700; letter-spacing: -0.045em; text-wrap: balance; }
.lp-section-heading > p:last-child, .lp-scenario-copy > p, .lp-final-card p:last-child { margin: 24px 0 0; color: var(--lp-muted); font-size: 18px; line-height: 28px; text-wrap: pretty; }
.lp-capabilities { background: var(--lp-bg); }
.lp-capability-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 24px; margin-top: 48px; }
.lp-capability-card { display: flex; flex-direction: column; overflow: hidden; border: 1px solid rgba(16, 42, 43, 0.12); border-radius: 24px; background: var(--lp-surface); }
.lp-capability-card--tint { background: #eef6f0; }
.lp-capability-visual { position: relative; height: 264px; overflow: hidden; margin: 8px; border-radius: 16px; }
.lp-card-copy { padding: 16px 24px 32px; }
.lp-card-index { display: block; color: var(--lp-cyan); font-family: "Geist Mono", "Cascadia Mono", monospace; font-size: 12px; font-weight: 700; }
.lp-card-copy h3 { margin: 8px 0 0; font-size: 24px; line-height: 32px; font-weight: 700; letter-spacing: -0.03em; }
.lp-card-copy p { margin: 8px 0 0; color: var(--lp-muted); font-size: 16px; line-height: 24px; }
.lp-caption-tile { padding: 24px; color: #ffffff; background: var(--lp-dark); }
.lp-tile-top { display: flex; align-items: center; justify-content: space-between; color: #a9d8ce; font-size: 12px; font-weight: 700; }
.lp-tile-top span { display: inline-flex; align-items: center; gap: 8px; }
.lp-tile-top span i { width: 8px; height: 8px; border-radius: 50%; background: #75ddb1; box-shadow: 0 0 0 5px rgba(117, 221, 177, 0.14); }
.lp-tile-top strong { color: #75ddb1; font-family: "Geist Mono", "Cascadia Mono", monospace; letter-spacing: 0.06em; }
.lp-caption-tile > p { max-width: 320px; margin: 32px 0 0; font-size: 24px; line-height: 32px; font-weight: 700; letter-spacing: -0.02em; }
.lp-caption-line { display: block; height: 8px; overflow: hidden; margin-top: 24px; border-radius: 999px; background: rgba(255, 255, 255, 0.12); }
.lp-caption-line i { display: block; width: 38%; height: 100%; border-radius: 999px; background: var(--lp-mint); }
.lp-wave-bars { position: absolute; left: 24px; right: 24px; bottom: 24px; display: flex; align-items: center; gap: 8px; height: 40px; opacity: 0.52; }
.lp-wave-bars i { flex: 1; height: calc(8px + (var(--bar) % 5) * 6px); border-radius: 999px; background: #8cd6c2; transform-origin: center; }
.lp-sync-tile { display: flex; align-items: center; justify-content: center; gap: 16px; padding: 24px; background: #dff0e8; }
.lp-slide-card, .lp-follow-card { position: relative; z-index: 1; width: 42%; min-height: 176px; padding: 16px; border: 1px solid rgba(16, 42, 43, 0.12); border-radius: 16px; background: var(--lp-surface); box-shadow: 0 12px 28px rgba(10, 44, 49, 0.1); }
.lp-slide-card span, .lp-follow-card span { color: var(--lp-cyan); font-family: "Geist Mono", "Cascadia Mono", monospace; font-size: 12px; font-weight: 700; }
.lp-slide-card strong, .lp-follow-card strong { display: block; margin-top: 16px; font-size: 16px; line-height: 24px; font-weight: 700; }
.lp-slide-card i, .lp-follow-card i { display: block; height: 8px; margin-top: 12px; border-radius: 999px; background: #d8e1da; }
.lp-slide-card i:nth-of-type(2) { width: 62%; }
.lp-follow-card { background: var(--lp-dark); color: #ffffff; }
.lp-follow-card span { color: #91d7c2; }
.lp-follow-card i { background: #315b5c; }
.lp-sync-link { position: absolute; z-index: 0; display: grid; place-items: center; width: 40px; height: 40px; border-radius: 999px; color: var(--lp-dark); background: var(--lp-amber); }
.lp-sync-link i { width: 16px; height: 2px; border-radius: 999px; background: currentColor; }
.lp-sync-link::after { content: ""; position: absolute; right: 10px; width: 6px; height: 6px; border-top: 2px solid currentColor; border-right: 2px solid currentColor; transform: rotate(45deg); }
.lp-review-tile { display: grid; place-items: center; background: #f1e7cb; }
.lp-review-sheet { position: relative; z-index: 1; width: 66%; padding: 24px; border: 1px solid rgba(16, 42, 43, 0.12); border-radius: 16px; background: var(--lp-surface); box-shadow: 0 14px 34px rgba(10, 44, 49, 0.12); }
.lp-review-sheet > span:first-child { color: var(--lp-cyan); font-size: 12px; font-weight: 700; }
.lp-review-sheet strong { display: block; margin-top: 16px; font-size: 20px; line-height: 28px; font-weight: 700; }
.lp-review-sheet i { display: block; width: 100%; height: 8px; margin-top: 12px; border-radius: 999px; background: #dce3dc; }
.lp-review-sheet i:nth-of-type(2) { width: 78%; }
.lp-review-sheet i:nth-of-type(3) { width: 52%; }
.lp-review-ai { display: inline-flex; margin-top: 24px; padding: 4px 8px; border-radius: 999px; color: #7c5b0f; background: #f4dfaa; font-size: 12px; font-weight: 700; }
.lp-review-stack { position: absolute; width: 54%; height: 72%; border: 1px solid rgba(16, 42, 43, 0.1); border-radius: 16px; background: #f8f2e1; transform: rotate(5deg) translate(18px, 8px); }
.lp-tagline { padding: 96px 0; color: #ffffff; background: var(--lp-dark); }
.lp-tagline-kicker { margin: 0 0 32px; color: #82caba; font-family: "Geist Mono", "Cascadia Mono", monospace; font-size: 14px; font-weight: 700; letter-spacing: 0.08em; }
.lp-tagline-copy { display: flex; flex-wrap: wrap; gap: 0 0.28em; max-width: 1080px; margin: 0; font-size: 60px; line-height: 1; font-weight: 700; letter-spacing: -0.045em; text-wrap: balance; }
.lp-tagline-copy span { color: rgba(255, 255, 255, 0.28); transition: color 600ms var(--lp-ease); }
.lp-tagline-copy.is-lit span { color: #ffffff; }
.lp-tagline-copy.is-lit span:nth-child(1) { transition-delay: 80ms; }
.lp-tagline-copy.is-lit span:nth-child(2) { transition-delay: 200ms; }
.lp-tagline-copy.is-lit span:nth-child(3) { transition-delay: 320ms; }
.lp-tagline-copy.is-lit span:nth-child(4) { transition-delay: 440ms; }
.lp-flow { background: var(--lp-surface); }
.lp-section-heading--split { max-width: none; display: grid; grid-template-columns: minmax(0, 1fr) minmax(320px, 460px); align-items: end; gap: 64px; }
.lp-section-heading--split > p { margin: 0; }
.lp-flow-grid { position: relative; display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 24px; margin-top: 48px; }
.lp-flow-grid::before { content: ""; position: absolute; top: 64px; left: 12%; right: 12%; height: 1px; background: #c9d5cc; }
.lp-flow-card { position: relative; z-index: 1; min-height: 320px; padding: 32px; border: 1px solid rgba(16, 42, 43, 0.12); border-radius: 24px; background: var(--lp-surface); }
.lp-flow-card--dark { color: #ffffff; background: var(--lp-dark); border-color: var(--lp-dark); }
.lp-flow-number { color: var(--lp-cyan); font-family: "Geist Mono", "Cascadia Mono", monospace; font-size: 12px; font-weight: 700; }
.lp-flow-card--dark .lp-flow-number { color: #82caba; }
.lp-flow-icon { width: 64px; height: 64px; display: grid; place-items: center; margin-top: 40px; border-radius: 16px; color: var(--lp-cyan); background: #e0f1ef; }
.lp-flow-card--dark .lp-flow-icon { color: #8ed8c4; background: #184347; }
.lp-flow-icon svg { width: 32px; height: 32px; }
.lp-flow-card h3 { margin: 24px 0 0; font-size: 24px; line-height: 32px; font-weight: 700; letter-spacing: -0.03em; }
.lp-flow-card p { margin: 12px 0 0; color: var(--lp-muted); font-size: 16px; line-height: 24px; }
.lp-flow-card--dark p { color: #b4cbc8; }
.lp-scenario { padding: 96px 0; color: #ffffff; background: #123b3e; }
.lp-scenario-grid { display: grid; grid-template-columns: minmax(0, 0.9fr) minmax(420px, 1.1fr); align-items: center; gap: 96px; }
.lp-kicker--light { color: #8ed8c4; }
.lp-scenario-copy h2 { max-width: 560px; }
.lp-scenario-copy > p { max-width: 560px; color: #b8cfcc; }
.lp-home .lp-text-link { display: inline-flex; align-items: center; gap: 8px; margin-top: 32px; color: #8ed8c4; font-size: 16px; font-weight: 700; }
.lp-timeline { padding: 8px; border: 1px solid rgba(142, 216, 196, 0.18); border-radius: 24px; background: rgba(255, 255, 255, 0.04); }
.lp-timeline-item { position: relative; display: grid; grid-template-columns: 64px 1fr 24px; align-items: center; gap: 16px; min-height: 88px; padding: 16px 24px; border-radius: 16px; color: #b8cfcc; transition: background-color 300ms var(--lp-ease), color 300ms var(--lp-ease); }
.lp-timeline-item + .lp-timeline-item { margin-top: 4px; }
.lp-timeline-item.is-current { color: #ffffff; background: rgba(81, 181, 141, 0.13); }
.lp-timeline-time { color: #81a9a5; font-family: "Geist Mono", "Cascadia Mono", monospace; font-size: 12px; font-weight: 700; }
.lp-timeline-item strong { display: block; color: #ffffff; font-size: 16px; font-weight: 700; }
.lp-timeline-item p { margin: 4px 0 0; color: #9bb8b4; font-size: 14px; line-height: 20px; }
.lp-timeline-mark { width: 12px; height: 12px; border: 2px solid #719d98; border-radius: 50%; }
.lp-timeline-item.is-current .lp-timeline-mark { border-color: #8ed8c4; background: #8ed8c4; box-shadow: 0 0 0 6px rgba(142, 216, 196, 0.12); }
.lp-faq { background: var(--lp-bg); }
.lp-faq-grid { display: grid; grid-template-columns: minmax(280px, 0.7fr) minmax(0, 1.3fr); gap: 96px; }
.lp-faq-list { border-top: 1px solid rgba(16, 42, 43, 0.14); }
.lp-faq details { border-bottom: 1px solid rgba(16, 42, 43, 0.14); }
.lp-faq summary { display: flex; align-items: center; justify-content: space-between; gap: 24px; padding: 24px 0; font-size: 18px; line-height: 28px; font-weight: 700; cursor: pointer; list-style: none; }
.lp-faq summary::-webkit-details-marker { display: none; }
.lp-faq summary span { display: grid; place-items: center; flex: 0 0 32px; width: 32px; height: 32px; border: 1px solid rgba(16, 42, 43, 0.16); border-radius: 999px; color: var(--lp-cyan); font-family: "Geist Mono", "Cascadia Mono", monospace; font-size: 18px; transition: transform 300ms var(--lp-ease), background-color 300ms var(--lp-ease), color 300ms var(--lp-ease); }
.lp-faq details[open] summary span { color: #ffffff; background: var(--lp-dark); transform: rotate(45deg); }
.lp-faq details p { max-width: 680px; margin: 0; padding: 0 64px 24px 0; color: var(--lp-muted); font-size: 16px; line-height: 24px; }
.lp-final-cta { padding: 0 0 96px; background: var(--lp-bg); }
.lp-final-card { display: flex; align-items: center; justify-content: space-between; gap: 48px; padding: 48px; border-radius: 24px; background: var(--lp-mint-soft); }
.lp-final-card > div { max-width: 680px; }
.lp-final-card h2 { font-size: 36px; }
.lp-home .lp-primary-button--light { flex: 0 0 auto; background: #ffffff; color: var(--lp-dark); }
.lp-home .lp-primary-button--light:hover { background: #ffffff; color: var(--lp-dark); }
.lp-footer { padding: 64px 0 32px; color: #dbe8e5; background: #082328; }
.lp-footer-grid { display: grid; grid-template-columns: minmax(280px, 0.9fr) minmax(0, 1.1fr); gap: 96px; }
.lp-brand--footer .lp-brand-mark { color: #082328; background: #8ed8c4; }
.lp-brand--footer .lp-brand-copy strong { color: #ffffff; }
.lp-brand--footer .lp-brand-copy small { color: #8fb2ae; }
.lp-footer-brand > p { max-width: 360px; margin: 24px 0 0; color: #93b5b1; font-size: 14px; line-height: 20px; }
.lp-footer-links { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 32px; }
.lp-footer-links div { display: flex; flex-direction: column; gap: 12px; }
.lp-footer-links strong { margin-bottom: 4px; color: #ffffff; font-size: 14px; font-weight: 700; }
.lp-footer-links a { width: fit-content; color: #93b5b1; font-size: 14px; transition: color 300ms var(--lp-ease); }
.lp-footer-links a:hover { color: #ffffff; }
.lp-footer-bottom { display: flex; justify-content: space-between; gap: 24px; margin-top: 64px; padding-top: 24px; border-top: 1px solid rgba(219, 232, 229, 0.14); color: #789b97; font-size: 12px; }
.lp-reveal { opacity: 0; transform: translateY(32px); transition: opacity 600ms var(--lp-ease), transform 800ms var(--lp-ease); }
.lp-reveal.is-visible { opacity: 1; transform: translateY(0); }
.lp-capability-card:nth-child(2), .lp-flow-card:nth-child(2) { transition-delay: 80ms; }
.lp-capability-card:nth-child(3), .lp-flow-card:nth-child(3) { transition-delay: 160ms; }
.lp-anim, .lp-caption-tile .lp-anim, .lp-sync-tile .lp-anim, .lp-review-tile .lp-anim, .lp-timeline .lp-anim { animation-play-state: paused; }
.lp-capabilities.is-active .lp-anim, .lp-scenario.is-active .lp-anim { animation-play-state: running; }
.lp-caption-tile .lp-caption-line i { animation: lp-caption-fill 3.2s var(--lp-ease) infinite; }
.lp-caption-tile .lp-wave-bars i { animation: lp-wave 1.2s var(--lp-ease) infinite alternate; animation-delay: calc(var(--bar) * -80ms); }
.lp-sync-tile .lp-sync-link i, .lp-sync-tile .lp-sync-link::after { animation: lp-sync-nudge 2.4s var(--lp-ease) infinite; }
.lp-review-tile .lp-review-ai { animation: lp-badge-glow 2.4s var(--lp-ease) infinite; }
.lp-scenario .lp-timeline-item.is-current { animation: lp-current-row 5.6s var(--lp-ease) infinite; }
@keyframes lp-pulse { 0%, 100% { transform: scale(0.7); opacity: 0.8; } 50% { transform: scale(1.7); opacity: 0; } }
@keyframes lp-avatar-pulse { 0%, 100% { transform: scale(.75); opacity: .55; } 50% { transform: scale(1.12); opacity: 1; } }
@keyframes lp-avatar-orbit { from { transform: translate(-50%, -50%) rotate(0deg) scaleX(.72); } to { transform: translate(-50%, -50%) rotate(360deg) scaleX(.72); } }
@keyframes lp-avatar-orbit-reverse { from { transform: translate(-50%, -50%) rotate(360deg); } to { transform: translate(-50%, -50%) rotate(0deg); } }
@keyframes lp-avatar-scan { 0%, 100% { transform: translateX(-50%) scaleX(.35); opacity: .22; } 50% { transform: translateX(-50%) scaleX(1); opacity: .85; } }
@keyframes lp-avatar-badge { 0%, 100% { box-shadow: 0 0 0 0 rgba(81, 181, 141, 0); } 50% { box-shadow: 0 0 0 5px rgba(81, 181, 141, 0.12); } }
@keyframes lp-caption-fill { 0%, 10% { transform: scaleX(0.08); } 55%, 86% { transform: scaleX(1); } 100% { transform: scaleX(0.08); } }
@keyframes lp-wave { from { transform: scaleY(0.45); opacity: 0.45; } to { transform: scaleY(1); opacity: 1; } }
@keyframes lp-sync-nudge { 0%, 100% { transform: translateX(-4px); opacity: 0.55; } 50% { transform: translateX(4px); opacity: 1; } }
@keyframes lp-badge-glow { 0%, 100% { box-shadow: 0 0 0 0 rgba(226, 166, 37, 0); } 50% { box-shadow: 0 0 0 6px rgba(226, 166, 37, 0.12); } }
@keyframes lp-current-row { 0%, 15%, 100% { background: rgba(81, 181, 141, 0.13); } 30%, 82% { background: rgba(81, 181, 141, 0.24); } }
@media (max-width: 1100px) {
  .lp-hero-grid, .lp-scenario-grid { gap: 48px; }
  .lp-hero h1 { font-size: 48px; }
  .lp-section-heading h2, .lp-scenario-copy h2, .lp-tagline-copy { font-size: 48px; }
  .lp-proof-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .lp-proof-item:nth-child(2) { border-right: 0; }
  .lp-proof-item:nth-child(-n + 2) { border-bottom: 1px solid rgba(16, 42, 43, 0.1); }
  .lp-capability-grid, .lp-flow-grid { grid-template-columns: 1fr; }
  .lp-capability-visual { height: 320px; }
  .lp-flow-grid::before { display: none; }
  .lp-flow-card { min-height: 0; }
  .lp-flow-icon { margin-top: 24px; }
  .lp-section-heading--split, .lp-scenario-grid, .lp-faq-grid, .lp-footer-grid { grid-template-columns: 1fr; gap: 48px; }
  .lp-section-heading--split > p { max-width: 680px; }
  .lp-timeline { max-width: 680px; }
}
@media (max-width: 980px) {
  .lp-hero-grid { grid-template-columns: 1fr; gap: 40px; }
  .lp-hero-copy { max-width: 680px; }
  .lp-hero-visual { width: min(100%, 760px); }
}
@media (max-width: 767px) {
  .lp-container, .lp-nav-shell { width: min(100% - 24px, 1320px); }
  .lp-nav-shell { top: 8px; margin-top: 8px; }
  .lp-nav { min-height: 64px; grid-template-columns: 1fr auto; gap: 16px; padding: 8px 8px 8px 12px; border-radius: 16px; }
  .lp-nav-links { display: none; }
  .lp-brand-mark { width: 36px; height: 36px; }
  .lp-brand-copy { flex-direction: column; align-items: flex-start; gap: 0; }
  .lp-brand-copy strong { font-size: 16px; }
  .lp-brand-copy small { font-size: 12px; }
  .lp-login-button { min-height: 40px; padding: 8px 12px; font-size: 14px; }
  .lp-login-button svg { width: 16px; height: 16px; }
  .lp-hero { padding: 48px 0; }
  .lp-hero::before, .lp-hero::after { display: none; }
  .lp-hero-grid { grid-template-columns: 1fr; gap: 40px; }
  .lp-hero h1, .lp-section-heading h2, .lp-scenario-copy h2, .lp-tagline-copy { font-size: 36px; }
  .lp-hero h1 span { margin-top: 4px; }
  .lp-hero-lede, .lp-section-heading > p:last-child, .lp-scenario-copy > p, .lp-final-card p:last-child { font-size: 16px; line-height: 24px; }
  .lp-hero-actions { align-items: flex-start; flex-direction: column; }
  .lp-primary-button { width: 100%; }
  .lp-hero-visual { padding: 8px; }
  .lp-avatar-chrome { gap: 8px; padding: 8px 8px 8px 12px; }
  .lp-avatar-title > span:last-child { display: none; }
  .lp-avatar-status-chip { display: none; }
  .lp-avatar-replay { min-height: 32px; }
  .lp-avatar-canvas { height: 260px; }
  .lp-avatar-canvas::before { width: 230px; height: 230px; }
  .lp-avatar-canvas::after { width: 200px; }
  .lp-avatar-orbit--one { width: 200px; height: 200px; }
  .lp-avatar-orbit--two { width: 250px; height: 150px; }
  .lp-avatar-side-note { display: none; }
  .lp-avatar-caption { grid-template-columns: 1fr; gap: 8px; padding: 12px; }
  .lp-avatar-caption-label { justify-self: start; }
  .lp-avatar-caption p { font-size: 14px; line-height: 20px; }
  .lp-float-note { display: none; }
  .lp-proof-grid { grid-template-columns: 1fr; }
  .lp-proof-item { min-height: 88px; padding: 16px; border-right: 0; border-bottom: 1px solid rgba(16, 42, 43, 0.1); }
  .lp-proof-item:last-child { border-bottom: 0; }
  .lp-section, .lp-tagline, .lp-scenario { padding: 64px 0; }
  .lp-capability-grid, .lp-flow-grid { margin-top: 32px; }
  .lp-capability-visual { height: 240px; }
  .lp-card-copy, .lp-flow-card { padding: 24px; }
  .lp-tagline-kicker { margin-bottom: 24px; }
  .lp-tagline-copy { gap: 0 0.18em; }
  .lp-scenario-grid { gap: 40px; }
  .lp-timeline-item { grid-template-columns: 52px 1fr 16px; gap: 12px; padding: 16px 12px; }
  .lp-faq summary { font-size: 16px; line-height: 24px; }
  .lp-faq details p { padding-right: 0; }
  .lp-final-cta { padding-bottom: 64px; }
  .lp-final-card { align-items: flex-start; flex-direction: column; gap: 32px; padding: 32px 24px; }
  .lp-final-card h2 { font-size: 30px; }
  .lp-footer { padding-top: 48px; }
  .lp-footer-links { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 32px; }
  .lp-footer-bottom { flex-direction: column; margin-top: 48px; }
}
@media (prefers-reduced-motion: reduce) {
  html.lp-page-active { scroll-behavior: auto; }
  .lp-home *, .lp-home *::before, .lp-home *::after { animation-duration: 0.01ms !important; animation-iteration-count: 1 !important; transition-duration: 0.01ms !important; }
  .lp-enter, .lp-reveal { opacity: 1; filter: none; transform: none; }
  .lp-tagline-copy span { color: #ffffff; }
}
:root[data-contrast="high"] .lp-home { --lp-bg: #ffffff; --lp-surface: #ffffff; --lp-surface-muted: #ffffff; --lp-ink: #000000; --lp-muted: #111111; --lp-border: #000000; --lp-cyan: #003f5c; --lp-mint: #006b4f; --lp-mint-soft: #ffffff; --lp-dark: #000000; }
:root[data-contrast="high"] .lp-home .lp-capability-card, :root[data-contrast="high"] .lp-home .lp-flow-card, :root[data-contrast="high"] .lp-home .lp-final-card, :root[data-contrast="high"] .lp-home .lp-proof-item { border-color: #000000; background: #ffffff; color: #000000; }
:root[data-theme="dark"] .lp-home { --lp-bg: #0b1718; --lp-surface: #132425; --lp-surface-muted: #182e2f; --lp-ink: #f1f7f4; --lp-muted: #b3c6c1; --lp-border: #365353; --lp-mint-soft: #163d35; }
:root[data-theme="dark"] .lp-home .lp-hero h1 { color: #f1f7f4; }
:root[data-theme="dark"] .lp-home .lp-hero h1 span { background: linear-gradient(90deg, #ffffff 0%, #9b9b9b 100%); -webkit-background-clip: text; background-clip: text; }
:root[data-theme="dark"] .lp-home .lp-nav, :root[data-theme="dark"] .lp-home .lp-proof-strip, :root[data-theme="dark"] .lp-home .lp-capability-card, :root[data-theme="dark"] .lp-home .lp-flow, :root[data-theme="dark"] .lp-home .lp-flow-card, :root[data-theme="dark"] .lp-home .lp-faq, :root[data-theme="dark"] .lp-home .lp-final-cta, :root[data-theme="dark"] .lp-home .lp-final-card { background-color: #0f1f20; }
</style>
