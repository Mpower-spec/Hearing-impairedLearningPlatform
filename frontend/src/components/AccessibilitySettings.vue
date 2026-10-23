<template>
  <div class="accessibility-settings">
    <el-tooltip content="无障碍阅读设置" placement="bottom">
      <el-button class="accessibility-trigger" :class="{ 'is-customized': customized }" aria-label="打开无障碍阅读设置" :aria-expanded="open" @click="open = true">
        <el-icon><Setting /></el-icon>
        <span class="accessibility-trigger__label">阅读设置</span>
      </el-button>
    </el-tooltip>

    <el-drawer v-model="open" title="无障碍阅读设置" direction="rtl" size="min(430px, 94vw)" append-to-body class="accessibility-settings-drawer">
      <div class="accessibility-settings-content">
        <section class="settings-section">
          <div class="section-heading">
            <div><span class="section-kicker">字幕</span><h3>字号与行距</h3></div>
            <el-tag size="small" effect="plain">{{ settings.captionFontSize }} px</el-tag>
          </div>
          <div class="setting-control">
            <div class="setting-label"><label>字幕字号</label><small>同时调整实时字幕、课堂记录和视频字幕</small></div>
            <el-slider v-model="settings.captionFontSize" :min="28" :max="64" :step="2" aria-label="字幕字号" show-input :show-input-controls="false" />
          </div>
          <div class="setting-control">
            <div class="setting-label"><label>字幕行距</label><small>较大的行距有助于减少长句阅读疲劳</small></div>
            <el-slider v-model="settings.captionLineHeight" :min="1.2" :max="2" :step="0.1" aria-label="字幕行距" :format-tooltip="(value) => `${Number(value).toFixed(1)} 倍`" />
            <span class="line-height-value">{{ Number(settings.captionLineHeight).toFixed(1) }} 倍</span>
          </div>
          <div class="caption-preview" aria-live="polite">
            <span>字幕预览</span>
            <p>欢迎使用，祝你学习顺利。调整后所有课堂字幕会立即应用。</p>
          </div>
        </section>

        <section class="settings-section">
          <div class="section-heading"><div><span class="section-kicker">显示</span><h3>视觉与动效</h3></div></div>
          <div class="theme-setting">
            <div class="setting-label"><label>页面主题</label><small>主题切换会同步应用到字幕、卡片和弹窗</small></div>
            <el-radio-group v-model="settings.theme" class="theme-options" aria-label="页面主题">
              <el-radio-button label="light"><el-icon><Sunny /></el-icon>明亮</el-radio-button>
              <el-radio-button label="dark"><el-icon><Moon /></el-icon>深色</el-radio-button>
            </el-radio-group>
          </div>
          <div class="setting-row">
            <div class="setting-label"><strong>高对比度</strong><small>增强文字、边框和字幕背景的对比</small></div>
            <el-switch v-model="settings.highContrast" aria-label="高对比度" />
          </div>
          <div class="setting-row">
            <div class="setting-label"><strong>减少动画</strong><small>关闭轮播自动播放、过渡和装饰动画</small></div>
            <el-switch v-model="settings.reduceMotion" aria-label="减少动画" />
          </div>
        </section>
        <p class="settings-note">设置仅保存在当前浏览器，不需要登录权限，也不会写入数据库。刷新或重新登录后会自动恢复。</p>
      </div>
      <template #footer>
        <div class="settings-footer">
          <el-button @click="resetAccessibilitySettings">恢复默认</el-button>
          <el-button type="primary" @click="open = false">完成</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { Moon, Setting, Sunny } from '@element-plus/icons-vue'
import { accessibilitySettings as settings, resetAccessibilitySettings } from '../accessibility/settings.js'

const open = ref(false)
const customized = computed(() => settings.captionFontSize !== 42 || settings.captionLineHeight !== 1.35 || settings.highContrast || settings.theme !== 'light' || settings.reduceMotion)
</script>
<style scoped>
.accessibility-settings { display: inline-flex; }
.accessibility-trigger {
  --el-button-bg-color: rgba(245, 251, 251, 0.96);
  --el-button-border-color: #c8dde3;
  --el-button-text-color: #24536b;
  --el-button-hover-bg-color: #eaf6f6;
  --el-button-hover-border-color: #9dc8d3;
  --el-button-hover-text-color: #0b5f7d;
  min-height: 34px;
  gap: 6px;
}
.accessibility-trigger.is-customized { --el-button-border-color: #188f87; --el-button-text-color: #0b5f5b; box-shadow: inset 0 0 0 1px rgba(24, 143, 135, .18); }
.accessibility-trigger__label { white-space: nowrap; }
.accessibility-settings-content { display: flex; flex-direction: column; gap: 16px; }
.settings-section {
  padding: 16px;
  border: 1px solid var(--reading-card-border, var(--line));
  border-radius: 16px;
  color: var(--reading-card-text, var(--ink));
  background: var(--reading-card-bg);
  box-shadow: var(--reading-card-shadow, var(--shadow-soft));
}
.section-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 16px; }
.section-kicker { color: var(--primary); font-size: 10px; font-weight: 900; letter-spacing: .14em; }
.section-heading h3 { margin: 4px 0 0; font-size: 18px; }
.setting-control + .setting-control { margin-top: 18px; }
.setting-label { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.setting-label label, .setting-label strong { color: var(--reading-card-text, var(--ink)); font-size: 14px; font-weight: 800; }
.setting-label small { color: var(--ink-soft); font-size: 11px; line-height: 1.5; }
.setting-control :deep(.el-slider) { padding: 4px 8px 0; }
.setting-control :deep(.el-slider__runway) { margin: 10px 0; }
.setting-control :deep(.el-input-number) { width: 82px; }
.line-height-value { display: inline-block; margin-top: 2px; color: var(--primary); font-size: 12px; font-weight: 800; }
.caption-preview { margin-top: 18px; padding: 14px 16px; overflow: hidden; border: 1px solid var(--reading-card-border, var(--line)); border-radius: 14px; color: var(--caption-card-text, var(--ink)); background: var(--caption-card-bg); }
.caption-preview > span { display: block; margin-bottom: 7px; color: var(--ink-soft); font-size: 10px; font-weight: 800; letter-spacing: .08em; }
.caption-preview p { margin: 0; font-size: min(var(--caption-font-size, 42px), 42px); line-height: var(--caption-line-height, 1.35); font-weight: 800; overflow-wrap: anywhere; }
.theme-setting { padding-bottom: 15px; border-bottom: 1px solid var(--line); }
.theme-options { width: 100%; margin-top: 10px; }
.theme-options :deep(.el-radio-button) { flex: 1; }
.theme-options :deep(.el-radio-button__inner) { width: 100%; display: inline-flex; align-items: center; justify-content: center; gap: 5px; }
.setting-row { display: flex; align-items: center; justify-content: space-between; gap: 18px; padding-top: 15px; }
.settings-note { margin: 0; padding: 0 4px; color: var(--ink-soft); font-size: 11px; line-height: 1.65; }
.settings-footer { display: flex; justify-content: space-between; gap: 10px; }
.settings-footer .el-button { flex: 1; margin-left: 0; }
:global(.accessibility-settings-drawer .el-drawer__body) { padding: 18px; background: var(--paper); }
:global(.accessibility-settings-drawer .el-drawer__header) { margin-bottom: 0; padding: 18px 20px 14px; border-bottom: 1px solid var(--line); }
:global(.accessibility-settings-drawer .el-drawer__footer) { padding: 14px 18px calc(14px + env(safe-area-inset-bottom)); border-top: 1px solid var(--line); background: var(--surface); }
@media (max-width: 767px) {
  .accessibility-trigger { width: 42px; min-height: 42px; padding: 0; }
  .accessibility-trigger__label { display: none; }
  .settings-section { padding: 14px; }
  .caption-preview p { font-size: min(var(--caption-font-size, 42px), 34px); }
}
</style>