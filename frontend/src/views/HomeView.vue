<template>
  <div>
    <AppHeader />

    <main class="page-shell home-page">
      <section class="home-welcome">
        <div>
          <span class="home-role">{{ roleLabel }}</span>
          <h1>你好，{{ displayName }}</h1>
          <p>字幕优先，让课堂内容更清晰；协作同步，让每一次学习都有迹可循。</p>
        </div>
        <div class="home-welcome-note">
          <span class="welcome-note-dot"></span>
          <span>为听障学生打造的课堂学习空间</span>
        </div>
      </section>

      <section class="home-hero" aria-label="平台功能介绍">
        <el-carousel :interval="5200" height="clamp(400px, 46vh, 480px)" arrow="always" indicator-position="inside">
          <el-carousel-item v-for="slide in slides" :key="slide.key">
            <article class="hero-slide" :class="`hero-slide--${slide.theme}`">
              <div class="hero-copy">
                <span class="hero-eyebrow">{{ slide.eyebrow }}</span>
                <h2>{{ slide.title }}</h2>
                <p>{{ slide.description }}</p>
                <div class="hero-points">
                  <span v-for="point in slide.points" :key="point">{{ point }}</span>
                </div>
                <el-button type="primary" size="large" class="hero-action" @click="navigate(slide.action)">
                  {{ slide.actionLabel }}
                  <el-icon><ArrowRight /></el-icon>
                </el-button>
              </div>

              <div class="hero-visual" aria-hidden="true">
                <div v-if="slide.key === 'captions'" class="caption-mockup">
                  <div class="mockup-top"><span class="live-dot"></span>实时字幕</div>
                  <p>今天我们来学习光合作用，植物需要阳光、水和二氧化碳。</p>
                  <div class="wave-row">
                    <span v-for="bar in 14" :key="bar" :style="{ height: `${14 + (bar * 17) % 46}px`, animationDelay: `${bar * 0.08}s` }"></span>
                  </div>
                </div>

                <div v-else-if="slide.key === 'sync'" class="slide-mockup">
                  <div class="slide-mockup__top">
                    <span>课堂同步</span>
                    <span>第 2 / 8 页</span>
                  </div>
                  <div class="slide-mockup__canvas">
                    <span class="slide-kicker">LESSON 02</span>
                    <strong>课堂内容<br />跟着老师一起走</strong>
                    <div class="slide-mockup__line"></div>
                  </div>
                  <div class="slide-mockup__footer"><span></span><span></span><span></span><span class="active"></span><span></span></div>
                </div>

                <div v-else class="material-mockup">
                  <div class="material-mockup__top"><span class="material-mark">AI</span><span>课后学习资料</span></div>
                  <h3>课堂摘要</h3>
                  <div class="material-line material-line--long"></div>
                  <div class="material-line"></div>
                  <h3>重点与术语</h3>
                  <div class="material-line material-line--long"></div>
                  <div class="material-line material-line--short"></div>
                  <div class="material-tags"><span>摘要</span><span>术语</span><span>测验</span></div>
                </div>
              </div>
            </article>
          </el-carousel-item>
        </el-carousel>
      </section>

      <section class="home-shortcuts">
        <div class="home-section-heading">
          <div>
            <span class="section-kicker">快捷入口</span>
            <h2>从这里开始</h2>
          </div>
          <p>选择你接下来要完成的事情</p>
        </div>
        <div class="shortcut-grid" :class="{ 'shortcut-grid--two': shortcuts.length === 2 }">
          <button v-for="item in shortcuts" :key="item.title" class="shortcut-card" :class="`shortcut-card--${item.tone}`" @click="navigate(item.path)">
            <span class="shortcut-icon"><el-icon><component :is="item.icon" /></el-icon></span>
            <span class="shortcut-copy">
              <span v-if="item.badge" class="shortcut-badge">{{ item.badge }}</span>
              <strong>{{ item.title }}</strong>
              <small>{{ item.description }}</small>
            </span>
            <el-icon class="shortcut-arrow"><ArrowRight /></el-icon>
          </button>
        </div>
      </section>
    </main>
    <SignAvatarWidget initial-text="你好，欢迎使用听障学习平台" sign-text="你好，欢迎使用，听障学习平台" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Document, Folder, MagicStick, Microphone, Monitor, VideoCamera } from '@element-plus/icons-vue'
import AppHeader from '../components/AppHeader.vue'
import SignAvatarWidget from '../components/SignAvatarWidget.vue'
import { session } from '../auth/session.js'

const router = useRouter()
const role = computed(() => session.user?.role)
const displayName = computed(() => session.user?.name || session.user?.username || '同学')
const roleLabel = computed(() => role.value === 'TEACHER' ? '教师端' : '学生端')

const slides = computed(() => {
  const teacher = role.value === 'TEACHER'
  return [
    {
      key: 'captions',
      theme: 'captions',
      eyebrow: '字幕优先 · 无障碍课堂',
      title: '让每一句话，\n都被看见',
      description: '把教师语音实时转化为大字号字幕，让听障学生不依赖声音，也能完整参与课堂。',
      points: ['实时字幕', '高对比度', '字幕记录'],
      action: teacher ? '/teacher/lessons' : '/student/join',
      actionLabel: teacher ? '进入课程' : '加入课堂'
    },
    {
      key: 'sync',
      theme: 'sync',
      eyebrow: '同步课堂 · 一页都不落下',
      title: '课堂进度，\n始终同步',
      description: '教师切换 PPT，学生端自动跟随；字幕、弹幕和课堂状态在同一个房间里实时呈现。',
      points: ['PPT 页码同步', '房间码加入', '静态弹幕'],
      action: teacher ? '/teacher/classrooms' : '/student/join',
      actionLabel: teacher ? '进入课堂列表' : '输入房间码'
    },
    {
      key: 'materials',
      theme: 'materials',
      eyebrow: 'AI 学习支持 · 课后可复习',
      title: '把课堂，\n沉淀成资料',
      description: '课堂结束后，学生可以自行生成摘要、重点、术语和测验，也能从视频中整理学习笔记。',
      points: ['AI 摘要', '术语与测验', '视频笔记'],
      action: teacher ? '/teacher/prepare-center' : '/student/videos',
      actionLabel: teacher ? '进入备课中心' : '开始视频学习'
    }
  ]
})

const shortcuts = computed(() => role.value === 'TEACHER' ? [
  { title: '课程与课件', description: '上传 PPTX，管理课程与课件内容', path: '/teacher/lessons', icon: Folder, tone: 'blue', badge: '课程库' },
  { title: '实时课堂', description: '查看正在同步和历史课堂记录', path: '/teacher/classrooms', icon: VideoCamera, tone: 'teal', badge: 'LIVE' },
  { title: '备课中心', description: '选择课程，生成教学目标与练习', path: '/teacher/prepare-center', icon: MagicStick, tone: 'amber', badge: 'AI' }
] : [
  { title: '加入课堂', description: '输入房间码，进入同步课堂', path: '/student/join', icon: Monitor, tone: 'blue' },
  { title: '视频学习', description: '上传视频，生成字幕与学习笔记', path: '/student/videos', icon: VideoCamera, tone: 'teal' }
])
function navigate(path) {
  router.push(path)
}
</script>

<style scoped>
.home-page { padding-bottom: 34px; }
.home-welcome {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 16px;
}
.home-role {
  display: inline-flex;
  padding: 5px 10px;
  border-radius: 999px;
  background: #e4f3f8;
  color: var(--primary-strong);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
}
.home-welcome h1 { margin: 8px 0 0; font-size: clamp(26px, 3vw, 38px); letter-spacing: -0.045em; }
.home-welcome p { margin: 8px 0 0; color: var(--ink-soft); line-height: 1.7; }
.home-welcome-note {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 10px 14px;
  border: 1px solid #cde3ec;
  border-radius: 999px;
  color: var(--primary-strong);
  background: rgba(255, 255, 255, 0.65);
  font-size: 13px;
  white-space: nowrap;
}
.welcome-note-dot, .live-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 5px rgba(34, 197, 94, 0.14);
}
.home-hero { border-radius: 26px; overflow: hidden; box-shadow: 0 20px 55px rgba(16, 42, 67, 0.18); }
.home-hero :deep(.el-carousel), .home-hero :deep(.el-carousel__container) { border-radius: 26px; }
.home-hero :deep(.el-carousel__arrow) { background: rgba(255,255,255,0.18); backdrop-filter: blur(8px); }
.home-hero :deep(.el-carousel__arrow:hover) { background: rgba(255,255,255,0.3); }
.home-hero :deep(.el-carousel__indicator button) { width: 26px; border-radius: 99px; }
.hero-slide {
  position: relative;
  height: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(300px, 0.92fr);
  align-items: center;
  gap: clamp(25px, 5vw, 75px);
  padding: clamp(28px, 5vw, 68px);
  overflow: hidden;
  color: #fff;
}
.hero-slide::before, .hero-slide::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(255,255,255,0.15);
  pointer-events: none;
}
.hero-slide::before { width: 380px; height: 380px; right: -100px; top: -150px; }
.hero-slide::after { width: 250px; height: 250px; left: 38%; bottom: -170px; }
.hero-slide--captions { background: linear-gradient(135deg, #071e32 0%, #0b5b7c 57%, #1b8f86 100%); }
.hero-slide--sync { background: linear-gradient(135deg, #102a43 0%, #24506b 52%, #257c78 100%); }
.hero-slide--materials { background: linear-gradient(135deg, #231b13 0%, #744e12 52%, #d9941a 100%); }
.hero-copy { position: relative; z-index: 1; }
.hero-eyebrow {
  display: inline-flex;
  padding: 7px 12px;
  border: 1px solid rgba(255,255,255,0.28);
  border-radius: 999px;
  background: rgba(255,255,255,0.1);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.06em;
}
.hero-copy h2 {
  margin: 18px 0 12px;
  font-size: clamp(36px, 4vw, 60px);
  line-height: 1.1;
  letter-spacing: -0.06em;
  white-space: pre-line;
}
.hero-copy p { max-width: 620px; margin: 0; color: rgba(255,255,255,0.82); font-size: clamp(15px, 1.5vw, 19px); line-height: 1.8; }
.hero-points { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 20px; }
.hero-points span { padding: 6px 11px; border-radius: 999px; background: rgba(255,255,255,0.12); font-size: 13px; }
.hero-action { margin-top: 24px; border: 0; background: #fff !important; color: #0b4763 !important; font-weight: 800; }
.hero-action:hover { background: #e8f7fb !important; }
.hero-action .el-icon { margin-left: 6px; }
.hero-visual { position: relative; z-index: 1; display: flex; justify-content: center; }
.caption-mockup, .material-mockup, .slide-mockup {
  width: min(100%, 430px);
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: 20px;
  box-shadow: 0 24px 50px rgba(0,0,0,0.22);
}
.caption-mockup { padding: 22px; background: rgba(2, 15, 27, 0.52); backdrop-filter: blur(12px); }
.mockup-top, .material-mockup__top, .slide-mockup__top { display: flex; align-items: center; justify-content: space-between; gap: 12px; font-size: 13px; }
.mockup-top { justify-content: flex-start; color: #9be5d4; font-weight: 800; }
.caption-mockup p { margin: 28px 0 24px; font-size: clamp(21px, 2.1vw, 29px); line-height: 1.55; font-weight: 800; }
.wave-row { display: flex; align-items: center; gap: 6px; height: 62px; }
.wave-row span { width: 7px; border-radius: 999px; background: #7de0ce; animation: wave 1.2s ease-in-out infinite alternate; }
@keyframes wave { from { transform: scaleY(0.45); opacity: 0.65; } to { transform: scaleY(1); opacity: 1; } }
.slide-mockup { overflow: hidden; background: rgba(255,255,255,0.1); backdrop-filter: blur(10px); }
.slide-mockup__top { padding: 15px 18px; color: #bde9e2; border-bottom: 1px solid rgba(255,255,255,0.14); }
.slide-mockup__canvas { margin: 18px; padding: 28px 24px; min-height: 230px; display: flex; flex-direction: column; justify-content: center; background: linear-gradient(145deg, #f7fbfc, #dceef1); color: #163c4d; }
.slide-kicker { color: #0b7b75; font-size: 11px; font-weight: 900; letter-spacing: 0.16em; }
.slide-mockup__canvas strong { margin-top: 12px; font-size: 27px; line-height: 1.25; letter-spacing: -0.04em; }
.slide-mockup__line { width: 45%; height: 6px; margin-top: 22px; border-radius: 99px; background: #84b9c0; }
.slide-mockup__footer { display: flex; justify-content: center; gap: 7px; padding: 0 18px 18px; }
.slide-mockup__footer span { width: 7px; height: 7px; border-radius: 50%; background: rgba(255,255,255,0.35); }
.slide-mockup__footer span.active { width: 20px; border-radius: 99px; background: #8de1d1; }
.material-mockup { padding: 24px; background: rgba(255, 250, 240, 0.96); color: #352917; }
.material-mockup__top { color: #8a5a00; font-weight: 800; }
.material-mark { display: grid; place-items: center; width: 30px; height: 30px; border-radius: 9px; background: #f0b429; color: #fff; font-size: 11px; }
.material-mockup h3 { margin: 24px 0 10px; font-size: 18px; }
.material-line { height: 8px; margin-top: 9px; border-radius: 99px; background: #e4d6bf; }
.material-line--long { width: 90%; }.material-line--short { width: 62%; }
.material-tags { display: flex; gap: 7px; margin-top: 22px; }
.material-tags span { padding: 5px 9px; border-radius: 6px; background: #f5e6b7; color: #7a5200; font-size: 11px; font-weight: 700; }
.home-shortcuts { margin-top: 24px; }
.home-section-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; margin-bottom: 14px; }
.section-kicker { color: var(--primary); font-size: 12px; font-weight: 900; letter-spacing: 0.14em; }
.home-section-heading h2 { margin: 4px 0 0; font-size: 26px; letter-spacing: -0.04em; }
.home-section-heading p { margin: 0; color: var(--ink-soft); font-size: 13px; }
.shortcut-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 14px; }
.shortcut-card {
  min-height: 132px;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  border: 1px solid var(--line);
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 10px 30px rgba(16, 42, 67, 0.07);
  color: var(--ink);
  text-align: left;
  cursor: pointer;
  transition: transform 180ms ease, box-shadow 180ms ease, border-color 180ms ease;
}
.shortcut-card:hover { transform: translateY(-4px); border-color: #8ac4d6; box-shadow: 0 16px 34px rgba(16, 42, 67, 0.13); }
.shortcut-icon { flex: 0 0 58px; width: 58px; height: 58px; display: grid; place-items: center; border-radius: 16px; font-size: 27px; }
.shortcut-card--blue .shortcut-icon { background: #e4f3f8; color: #0b6e99; }
.shortcut-card--teal .shortcut-icon { background: #e2f5ef; color: #1f8a70; }
.shortcut-card--amber .shortcut-icon { background: #fff1c7; color: #a96c00; }
.shortcut-card--teal .shortcut-badge { background: #e2f5ef; color: #1f8a70; }
.shortcut-card--amber .shortcut-badge { background: #fff1c7; color: #a96c00; }
.shortcut-copy { display: flex; flex-direction: column; gap: 7px; min-width: 0; }
.shortcut-badge { align-self: flex-start; padding: 2px 7px; border-radius: 999px; font-size: 10px; font-weight: 900; letter-spacing: .08em; background: #eef4f8; color: var(--primary-strong); }
.shortcut-copy strong { font-size: 18px; }
.shortcut-copy small { color: var(--ink-soft); font-size: 13px; line-height: 1.55; }
.shortcut-arrow { margin-left: auto; color: #8aa2b2; font-size: 19px; }
@media (max-width: 900px) {
  .home-welcome { align-items: flex-start; flex-direction: column; }
  .home-welcome-note { white-space: normal; }
  .hero-slide { grid-template-columns: 1fr; padding: 30px; }
  .hero-copy h2 { font-size: 42px; }
  .hero-visual { display: none; }
  .home-section-heading { align-items: flex-start; flex-direction: column; }
}
@media (max-width: 560px) {
  .home-page { padding-bottom: 20px; }
  .home-hero :deep(.el-carousel__arrow) { display: none; }
  .hero-slide { padding: 24px; }
  .hero-copy h2 { font-size: 34px; }
  .hero-copy p { font-size: 15px; }
  .shortcut-card { min-height: 118px; }
  .shortcut-grid { grid-template-columns: 1fr; }
}
</style>