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

      </section>

      <section class="home-hero" aria-label="平台功能介绍">
        <el-carousel :interval="5200" height="clamp(240px, 27vh, 290px)" arrow="always" indicator-position="inside">
          <el-carousel-item v-for="slide in slides" :key="slide.key">
            <article class="hero-slide" :class="`hero-slide--${slide.theme}`">
              <div class="hero-copy">
                <span class="hero-eyebrow">{{ slide.eyebrow }}</span>
                <h2>{{ slide.title }}</h2>
                <p>{{ slide.description }}</p>
                <div class="hero-points">
                  <span v-for="point in slide.points" :key="point">{{ point }}</span>
                </div>
                <el-button type="primary" class="hero-action" @click="navigate(slide.action)">
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
                  <div class="material-mockup__top">
                    <span class="material-mark">{{ teacher ? 'AI' : '学' }}</span>
                    <span>{{ teacher ? 'AI 备课中心' : '视频学习助手' }}</span>
                  </div>
                  <h3>{{ teacher ? '教学目标' : '视频字幕' }}</h3>
                  <div class="material-line material-line--long"></div>
                  <div class="material-line"></div>
                  <h3>{{ teacher ? '重点与练习' : '学习笔记' }}</h3>
                  <div class="material-line material-line--long"></div>
                  <div class="material-line material-line--short"></div>
                  <div class="material-tags">
                    <template v-if="teacher"><span>目标</span><span>重点</span><span>练习</span></template>
                    <template v-else><span>字幕</span><span>笔记</span><span>回看</span></template>
                  </div>
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

  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Document, Folder, MagicStick, Microphone, Monitor, VideoCamera } from '@element-plus/icons-vue'
import AppHeader from '../components/AppHeader.vue'

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
    teacher ? {
      key: 'teacher-prepare',
      theme: 'support',
      eyebrow: 'AI 备课中心 · 高效教学',
      title: '把备课，\n交给智能助手',
      description: '从 PPT 自动提炼教学重点，生成备课资料与课堂内容，让教师把更多时间留给学生。',
      points: ['AI 备课资料', '教学重点', '练习设计'],
      action: '/teacher/prepare-center',
      actionLabel: '进入备课中心'
    } : {
      key: 'student-videos',
      theme: 'support',
      eyebrow: '视频学习 · 随时复习',
      title: '把视频，\n变成学习资料',
      description: '上传学习视频，生成带时间轴的字幕与结构化笔记，让每一次复习都有清晰依据。',
      points: ['视频字幕', '学习笔记', '随时回看'],
      action: '/student/videos',
      actionLabel: '开始视频学习'
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
.home-page { min-height: 0; height: auto; padding-bottom: 34px; overflow: visible; }
.home-welcome {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 16px;
  padding: clamp(18px, 2.2vw, 28px);
  overflow: hidden;
  border: 1px solid rgba(11, 110, 153, 0.14);
  border-radius: 22px;
  background:
    radial-gradient(circle at 0% 0%, rgba(37, 161, 142, 0.13), transparent 24rem),
    radial-gradient(circle at 100% 0%, rgba(240, 180, 41, 0.12), transparent 20rem),
    linear-gradient(135deg, rgba(255, 255, 255, 0.82), rgba(228, 245, 246, 0.76));
  box-shadow: 0 12px 32px rgba(16, 42, 67, 0.08);
}
.home-welcome::before {
  content: "";
  position: absolute;
  width: 220px;
  height: 220px;
  right: -90px;
  top: -130px;
  border: 1px solid rgba(11, 110, 153, 0.12);
  border-radius: 50%;
  pointer-events: none;
}
.home-welcome > * { position: relative; z-index: 1; }
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
  padding: clamp(16px, 2.6vw, 32px);
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
.hero-slide--support { background: linear-gradient(135deg, #0a304b 0%, #126f80 54%, #2a9a83 100%); }
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
  margin: 9px 0 7px;
  font-size: clamp(24px, 2.5vw, 34px);
  line-height: 1.1;
  letter-spacing: -0.06em;
  white-space: pre-line;
}
.hero-copy p { max-width: 620px; margin: 0; color: rgba(255,255,255,0.82); font-size: clamp(13px, 1.1vw, 15px); line-height: 1.5; }
.hero-points { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }
.hero-points span { padding: 4px 8px; border-radius: 999px; background: rgba(255,255,255,0.12); font-size: 11px; }
.hero-action { margin-top: 10px; border: 0; background: #fff !important; color: #0b4763 !important; font-weight: 800; }
.hero-action:hover { background: #e8f7fb !important; }
.hero-action .el-icon { margin-left: 6px; }
.hero-visual { position: relative; z-index: 1; display: flex; justify-content: center; }
.caption-mockup, .material-mockup, .slide-mockup {
  width: min(100%, 320px);
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: 20px;
  box-shadow: 0 24px 50px rgba(0,0,0,0.22);
}
.caption-mockup { padding: 13px; background: rgba(2, 15, 27, 0.52); backdrop-filter: blur(12px); }
.mockup-top, .material-mockup__top, .slide-mockup__top { display: flex; align-items: center; justify-content: space-between; gap: 12px; font-size: 13px; }
.mockup-top { justify-content: flex-start; color: #9be5d4; font-weight: 800; }
.caption-mockup p { margin: 12px 0 10px; font-size: clamp(16px, 1.5vw, 20px); line-height: 1.4; font-weight: 800; }
.wave-row { display: flex; align-items: center; gap: 5px; height: 34px; }
.wave-row span { width: 7px; border-radius: 999px; background: #7de0ce; animation: wave 1.2s ease-in-out infinite alternate; }
@keyframes wave { from { transform: scaleY(0.45); opacity: 0.65; } to { transform: scaleY(1); opacity: 1; } }
.slide-mockup { overflow: hidden; background: rgba(255,255,255,0.1); backdrop-filter: blur(10px); }
.slide-mockup__top { padding: 10px 13px; color: #bde9e2; border-bottom: 1px solid rgba(255,255,255,0.14); }
.slide-mockup__canvas { margin: 12px; padding: 16px; min-height: 155px; display: flex; flex-direction: column; justify-content: center; background: linear-gradient(145deg, #f7fbfc, #dceef1); color: #163c4d; }
.slide-kicker { color: #0b7b75; font-size: 11px; font-weight: 900; letter-spacing: 0.16em; }
.slide-mockup__canvas strong { margin-top: 7px; font-size: 21px; line-height: 1.2; letter-spacing: -0.04em; }
.slide-mockup__line { width: 45%; height: 5px; margin-top: 13px; border-radius: 99px; background: #84b9c0; }
.slide-mockup__footer { display: flex; justify-content: center; gap: 7px; padding: 0 18px 18px; }
.slide-mockup__footer span { width: 7px; height: 7px; border-radius: 50%; background: rgba(255,255,255,0.35); }
.slide-mockup__footer span.active { width: 20px; border-radius: 99px; background: #8de1d1; }
.material-mockup { padding: 16px; background: rgba(2, 15, 27, 0.5); color: #ffffff; backdrop-filter: blur(12px); }
.material-mockup__top { color: #9be5d4; font-weight: 800; }
.material-mark { display: grid; place-items: center; width: 30px; height: 30px; border-radius: 9px; background: #f0b429; color: #fff; font-size: 11px; }
.material-mockup h3 { margin: 14px 0 7px; font-size: 16px; color: #ffffff; }
.material-line { height: 6px; margin-top: 7px; border-radius: 99px; background: rgba(255,255,255,0.2); }
.material-line--long { width: 90%; }.material-line--short { width: 62%; }
.material-tags { display: flex; gap: 6px; margin-top: 13px; }
.material-tags span { padding: 5px 9px; border-radius: 999px; background: rgba(240,180,41,0.18); color: #ffe6a6; font-size: 11px; font-weight: 700; }
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
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.98), rgba(245, 251, 250, 0.95));
  border-color: var(--line-strong);
  box-shadow: var(--shadow-soft);
  color: var(--ink);
  text-align: left;
  cursor: pointer;
  transition: transform 180ms ease, box-shadow 180ms ease, border-color 180ms ease;
}
.shortcut-card:hover { transform: translateY(-3px); border-color: #8ac4d6; box-shadow: var(--shadow-raised); }
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
  .home-welcome { align-items: stretch; flex-direction: column; }
  .hero-slide { grid-template-columns: 1fr; padding: 18px 24px; }
  .hero-copy h2 { font-size: 30px; }
  .hero-visual { display: none; }
  .home-section-heading { align-items: flex-start; flex-direction: column; }
}
@media (max-width: 560px) {
  .home-page { padding-bottom: 20px; }
  .home-hero :deep(.el-carousel__arrow) { display: none; }
  .hero-slide { padding: 16px 18px; }
  .hero-copy h2 { margin-top: 7px; font-size: 26px; }
  .hero-copy p { font-size: 13px; line-height: 1.45; }
  .hero-points { display: none; }
  .shortcut-card { min-height: 118px; }
  .shortcut-grid { grid-template-columns: 1fr; }
}
</style>
