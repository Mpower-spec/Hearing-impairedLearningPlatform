<template>
  <main class="login-page">
    <section class="login-hero">
      <div class="brand" style="margin-bottom: 38px">
        <div class="brand-mark" aria-hidden="true">声</div>
        <div>
          <span class="brand-title" style="color: #fff">声学无碍学习平台</span>
          <p class="brand-subtitle" style="color: #b9d9e6">Accessible Classroom Demo</p>
        </div>
      </div>
      <h1>让每一句话<br />都被看见</h1>
      <p>教师同步演示文稿与实时字幕，学生通过房间码进入课堂，课后自动沉淀摘要、重点与复习资料。</p>
      <div class="hero-points" aria-label="平台特点">
        <div class="hero-point">实时字幕优先</div>
        <div class="hero-point">PPT 页码同步</div>
        <div class="hero-point">AI 课后学习资料</div>
      </div>

      <div class="login-signal-board" aria-hidden="true">
        <div class="login-signal-card login-signal-card--caption">
          <div class="login-signal-card__top"><span class="signal-dot"></span><span>实时字幕通道</span><strong>LIVE</strong></div>
          <p>今天我们来学习光合作用，植物需要阳光、水和二氧化碳。</p>
          <div class="login-signal-wave"><span v-for="bar in 18" :key="bar" :style="{ '--bar': bar }"></span></div>
        </div>
        <div class="login-signal-card login-signal-card--ai">
          <span class="login-signal-label">AI STUDY FLOW</span>
          <strong>字幕 → 笔记 → 复习</strong>
          <small>让每一次学习都有迹可循</small>
        </div>
        <div class="login-scan-line"></div>
      </div>
    </section>

    <section class="login-panel-wrap">
      <div class="login-card panel">
        <h2>登录学习平台</h2>
        <p class="muted">使用预置演示账号进入教师端或学生端。</p>
        <el-form label-position="top" @submit.prevent="submit">
          <el-form-item label="用户名">
            <el-input v-model="form.username" size="large" autocomplete="username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" size="large" type="password" show-password
                      autocomplete="current-password" placeholder="请输入密码" @keyup.enter="submit" />
          </el-form-item>
          <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submit">
            登录
          </el-button>
        </el-form>
        <div class="demo-accounts">
          <p>演示账号，点击快速填入：</p>
          <div class="demo-buttons">
            <el-button size="small" @click="fill('teacher')">教师 teacher / 123456</el-button>
            <el-button size="small" @click="fill('student')">学生 student / 123456</el-button>
          </div>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http.js'
import { setSession, homeForRole } from '../auth/session.js'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

function fill(role) {
  form.username = role
  form.password = '123456'
}

async function submit() {
  if (!form.username.trim() || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const result = await http.post('/auth/login', {
      username: form.username.trim(),
      password: form.password
    })
    setSession(result.token, result.user)
    ElMessage.success(`欢迎，${result.user.name || result.user.username}`)
    router.push(homeForRole(result.user.role))
  } finally {
    loading.value = false
  }
}
</script>
