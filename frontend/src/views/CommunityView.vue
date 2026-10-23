<template>
  <div>
    <AppHeader />

    <main class="page-shell community-page">
      <div class="page-title-row">
        <div>
          <span class="page-kicker">LEARNING COMMUNITY</span>
          <h1 class="page-title">学习社区</h1>
          <p class="page-subtitle">用文字分享学习方法、提出疑问、交流课程经验，让每一次讨论都能被看见。</p>
        </div>
        <el-button type="primary" size="large" @click="openComposer">
          发布讨论
        </el-button>
      </div>

      <section class="community-banner" aria-label="社区介绍">
        <div class="community-banner-icon">
          <el-icon><ChatDotRound /></el-icon>
        </div>
        <div class="community-banner-copy">
          <strong>听障学生学习交流空间</strong>
          <p>教师和学生都可以发帖与评论。请尊重彼此，围绕学习内容展开讨论。</p>
        </div>
        <div class="community-stats">
          <span><strong>{{ total }}</strong> 个讨论</span>
          <span>教师与学生共同参与</span>
        </div>
      </section>

      <section class="panel community-panel" v-loading="loading">
        <div class="community-toolbar">
          <el-select v-model="category" placeholder="全部分类" clearable style="width: 160px" @change="search">
            <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
          </el-select>
          <el-input
            v-model="keyword"
            placeholder="搜索标题或帖子内容"
            clearable
            style="max-width: 360px"
            @keyup.enter="search"
            @clear="search"
          />
          <el-button type="primary" plain @click="search">搜索</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </div>

        <div v-if="posts.length" class="community-post-list">
          <article
            v-for="post in posts"
            :key="post.id"
            class="community-post-card"
            tabindex="0"
            role="button"
            :aria-label="`查看帖子：${post.title}`"
            @click="openDetail(post)"
            @keyup.enter="openDetail(post)"
          >
            <div class="post-card-topline">
              <el-tag effect="plain">{{ post.category }}</el-tag>
              <span class="post-time">{{ formatTime(post.createdAt) }}</span>
            </div>
            <h2>{{ post.title }}</h2>
            <p class="post-excerpt">{{ excerpt(post.content) }}</p>
            <div class="post-card-footer">
              <div class="post-author">
                <span class="author-avatar">{{ post.authorName.slice(0, 1) }}</span>
                <span>
                  <strong>{{ post.authorName }}</strong>
                  <small>{{ roleLabel(post.authorRole) }}</small>
                </span>
              </div>
              <div class="post-card-actions">
                <span class="post-comment-count">
                  <el-icon><ChatLineRound /></el-icon>{{ post.commentCount }} 条评论
                </span>
                <el-button v-if="post.canDelete" text type="danger" @click.stop="deletePost(post)">
                  删除
                </el-button>
              </div>
            </div>
          </article>
        </div>

        <el-empty v-else-if="!loading" description="还没有讨论，发布第一个学习话题吧" />

        <el-pagination
          v-if="totalPages > 1"
          class="community-pagination"
          background
          layout="prev, pager, next"
          :current-page="page"
          :page-size="size"
          :total="total"
          @current-change="changePage"
        />
      </section>
    </main>

    <el-dialog v-model="composerVisible" title="发布学习讨论" width="min(680px, 94vw)" destroy-on-close>
      <el-form label-position="top" @submit.prevent="submitPost">
        <el-form-item label="讨论分类" required>
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="form.title" maxlength="120" show-word-limit placeholder="用一句话概括你想讨论的问题" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            maxlength="5000"
            show-word-limit
            resize="vertical"
            placeholder="详细描述你的问题、经验或观点"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="composerVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingPost" @click="submitPost">发布帖子</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" width="min(860px, 95vw)" class="community-detail-dialog" destroy-on-close>
      <template #header>
        <div class="detail-dialog-header">
          <span>学习讨论</span>
          <el-button v-if="detail?.post.canDelete" text type="danger" @click="deletePost(detail.post)">
            删除帖子
          </el-button>
        </div>
      </template>

      <div v-if="detail" class="post-detail">
        <div class="post-card-topline">
          <el-tag effect="plain">{{ detail.post.category }}</el-tag>
          <span class="post-time">{{ formatTime(detail.post.createdAt) }}</span>
        </div>
        <h2>{{ detail.post.title }}</h2>
        <div class="post-author detail-author">
          <span class="author-avatar">{{ detail.post.authorName.slice(0, 1) }}</span>
          <span>
            <strong>{{ detail.post.authorName }}</strong>
            <small>{{ roleLabel(detail.post.authorRole) }}</small>
          </span>
        </div>
        <div class="post-full-content">{{ detail.post.content }}</div>

        <section class="comment-section">
          <div class="comment-heading">
            <h3>评论讨论</h3>
            <span>{{ detail.comments.length }} 条</span>
          </div>
          <div v-if="detail.comments.length" class="comment-list">
            <article v-for="comment in detail.comments" :key="comment.id" class="comment-card">
              <div class="comment-meta">
                <div>
                  <strong>{{ comment.authorName }}</strong>
                  <el-tag size="small" effect="plain">{{ roleLabel(comment.authorRole) }}</el-tag>
                  <time>{{ formatTime(comment.createdAt) }}</time>
                </div>
                <el-button v-if="comment.canDelete" text type="danger" size="small" @click="deleteComment(comment)">
                  删除
                </el-button>
              </div>
              <p>{{ comment.content }}</p>
            </article>
          </div>
          <el-empty v-else description="还没有评论，加入讨论吧" :image-size="70" />

          <div class="comment-composer">
            <el-input
              v-model="commentText"
              type="textarea"
              :rows="3"
              maxlength="2000"
              show-word-limit
              placeholder="写下你的想法或回答"
            />
            <div class="comment-composer-footer">
              <span>请围绕学习内容友善交流</span>
              <el-button type="primary" :loading="submittingComment" :disabled="!commentText.trim()" @click="submitComment">
                发表评论
              </el-button>
            </div>
          </div>
        </section>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ChatDotRound, ChatLineRound } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppHeader from '../components/AppHeader.vue'
import http from '../api/http.js'

const categories = ['学习讨论', '提问求助', '经验分享', '课程交流']
const posts = ref([])
const total = ref(0)
const page = ref(1)
const size = 8
const totalPages = computed(() => total.value === 0 ? 0 : Math.ceil(total.value / size))
const category = ref('')
const keyword = ref('')
const loading = ref(false)
const composerVisible = ref(false)
const detailVisible = ref(false)
const detail = ref(null)
const submittingPost = ref(false)
const submittingComment = ref(false)
const commentText = ref('')
const form = reactive({ category: '学习讨论', title: '', content: '' })

async function load() {
  loading.value = true
  try {
    const result = await http.get('/community/posts', {
      params: {
        page: page.value,
        size,
        category: category.value || undefined,
        keyword: keyword.value.trim() || undefined
      }
    })
    posts.value = result.items || []
    total.value = result.total || 0
    if (page.value > 1 && posts.value.length === 0 && total.value > 0) {
      page.value -= 1
      await load()
    }
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function resetFilters() {
  category.value = ''
  keyword.value = ''
  search()
}

function changePage(value) {
  page.value = value
  load()
}

function openComposer() {
  form.category = '学习讨论'
  form.title = ''
  form.content = ''
  composerVisible.value = true
}

async function submitPost() {
  if (!form.category || !form.title.trim() || !form.content.trim()) {
    ElMessage.warning('请填写分类、标题和内容')
    return
  }
  submittingPost.value = true
  try {
    await http.post('/community/posts', {
      category: form.category,
      title: form.title.trim(),
      content: form.content.trim()
    })
    composerVisible.value = false
    page.value = 1
    await load()
    ElMessage.success('帖子已发布')
  } finally {
    submittingPost.value = false
  }
}

async function openDetail(post) {
  detailVisible.value = true
  detail.value = null
  commentText.value = ''
  try {
    detail.value = await http.get(`/community/posts/${post.id}`)
  } catch {
    detailVisible.value = false
  }
}

async function submitComment() {
  if (!detail.value || !commentText.value.trim()) return
  submittingComment.value = true
  try {
    const postId = detail.value.post.id
    await http.post(`/community/posts/${postId}/comments`, { content: commentText.value.trim() })
    commentText.value = ''
    detail.value = await http.get(`/community/posts/${postId}`)
    await load()
    ElMessage.success('评论已发布')
  } finally {
    submittingComment.value = false
  }
}

async function deletePost(post) {
  try {
    await ElMessageBox.confirm('确定删除这个帖子吗？帖子和全部评论将无法恢复。', '删除帖子', {
      type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await http.delete(`/community/posts/${post.id}`)
  detailVisible.value = false
  detail.value = null
  await load()
  ElMessage.success('帖子已删除')
}

async function deleteComment(comment) {
  try {
    await ElMessageBox.confirm('确定删除这条评论吗？', '删除评论', {
      type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消'
    })
  } catch {
    return
  }
  const postId = detail.value.post.id
  await http.delete(`/community/comments/${comment.id}`)
  detail.value = await http.get(`/community/posts/${postId}`)
  await load()
  ElMessage.success('评论已删除')
}

function excerpt(content) {
  const text = String(content || '').replace(/\s+/g, ' ').trim()
  return text.length > 150 ? `${text.slice(0, 150)}…` : text
}

function formatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

function roleLabel(role) {
  return role === 'TEACHER' ? '教师' : '学生'
}

onMounted(load)
</script>

<style scoped>
.community-page { padding-bottom: 42px; }
.page-kicker { color: var(--primary); font-size: 12px; font-weight: 900; letter-spacing: 0.15em; }
.community-banner {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
  padding: 18px 20px;
  border: 1px solid var(--line-strong);
  border-radius: 20px;
  background:
    radial-gradient(circle at 0% 0%, rgba(37, 161, 142, 0.13), transparent 22rem),
    radial-gradient(circle at 100% 0%, rgba(240, 180, 41, 0.1), transparent 20rem),
    linear-gradient(135deg, rgba(250, 254, 253, 0.97), rgba(232, 246, 245, 0.95));
  box-shadow: var(--shadow-soft);
}
.community-banner-icon { display: grid; place-items: center; flex: 0 0 52px; width: 52px; height: 52px; border-radius: 16px; color: #fff; background: linear-gradient(135deg, #0b6e99, #25a18e); font-size: 25px; box-shadow: 0 8px 18px rgba(11,110,153,.2); }
.community-banner-copy { min-width: 0; }
.community-banner-copy strong { color: var(--ink); font-size: 17px; }
.community-banner-copy p { margin: 5px 0 0; color: var(--ink-soft); line-height: 1.6; }
.community-stats { margin-left: auto; display: flex; align-items: center; gap: 12px; color: var(--ink-soft); font-size: 12px; white-space: nowrap; }
.community-stats span:first-child { padding: 7px 12px; border-radius: 999px; background: rgba(255,255,255,.65); color: var(--primary-strong); }
.community-stats strong { font-size: 18px; }
.community-toolbar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 18px; }
.community-post-list { display: grid; gap: 13px; }
.community-post-card { padding: 17px 18px; border: 1px solid var(--line-strong); border-radius: 16px; background: linear-gradient(145deg, rgba(251,254,253,.98), rgba(237,247,246,.95)); cursor: pointer; transition: transform .16s ease, border-color .16s ease, box-shadow .16s ease; }
.community-post-card:hover, .community-post-card:focus-visible { transform: translateY(-2px); border-color: #86bcc8; box-shadow: var(--shadow-soft); }
.post-card-topline { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.post-time { color: var(--ink-soft); font-size: 11px; }
.community-post-card h2 { margin: 10px 0 7px; font-size: 19px; line-height: 1.4; }
.post-excerpt { margin: 0; color: var(--ink-soft); line-height: 1.65; }
.post-card-footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 14px; }
.post-author { display: flex; align-items: center; gap: 9px; min-width: 0; }
.author-avatar { display: grid; place-items: center; flex: 0 0 32px; width: 32px; height: 32px; border-radius: 50%; color: #fff; background: linear-gradient(135deg, #0b6e99, #25a18e); font-weight: 800; }
.post-author strong, .post-author small { display: block; }
.post-author strong { color: var(--ink); font-size: 13px; }
.post-author small { margin-top: 2px; color: var(--ink-soft); font-size: 10px; }
.post-card-actions { display: flex; align-items: center; gap: 8px; }
.post-comment-count { display: inline-flex; align-items: center; gap: 5px; color: var(--primary-strong); font-size: 12px; }
.community-pagination { justify-content: center; margin-top: 22px; }
.detail-dialog-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; color: var(--ink); font-size: 17px; font-weight: 800; }
.post-detail h2 { margin: 12px 0 10px; font-size: clamp(23px, 3vw, 30px); line-height: 1.4; }
.detail-author { margin-bottom: 18px; }
.post-full-content { padding: 18px; border: 1px solid #d2e5e9; border-radius: 14px; background: linear-gradient(145deg, #f5fafb, #edf7f5); color: var(--ink); line-height: 1.85; white-space: pre-wrap; overflow-wrap: anywhere; }
.comment-section { margin-top: 24px; border-top: 1px solid var(--line); padding-top: 20px; }
.comment-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.comment-heading h3 { margin: 0; }
.comment-heading span { color: var(--ink-soft); font-size: 12px; }
.comment-list { display: grid; gap: 10px; }
.comment-card { padding: 13px 14px; border: 1px solid #d5e5e9; border-radius: 13px; background: rgba(247, 252, 251, 0.9); }
.comment-meta { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.comment-meta > div { display: flex; align-items: center; gap: 7px; min-width: 0; flex-wrap: wrap; }
.comment-meta strong { color: var(--ink); font-size: 13px; }
.comment-meta time { color: var(--ink-soft); font-size: 10px; }
.comment-card p { margin: 8px 0 0; color: var(--ink); line-height: 1.7; white-space: pre-wrap; overflow-wrap: anywhere; }
.comment-composer { margin-top: 16px; padding: 14px; border: 1px solid #cfe3e7; border-radius: 14px; background: rgba(241, 249, 248, 0.9); }
.comment-composer-footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 10px; }
.comment-composer-footer span { color: var(--ink-soft); font-size: 11px; }
@media (max-width: 760px) {
  .community-banner { align-items: flex-start; flex-wrap: wrap; }
  .community-stats { width: 100%; margin-left: 68px; flex-wrap: wrap; }
  .community-toolbar > * { width: 100% !important; }
  .post-card-footer { align-items: flex-start; flex-direction: column; }
  .post-card-actions { width: 100%; justify-content: space-between; }
  .comment-composer-footer { align-items: flex-start; flex-direction: column; }
}
</style>
