import { createRouter, createWebHistory } from 'vue-router'
import { session, homeForRole } from '../auth/session.js'

const routes = [
  { path: '/', redirect: '/home' },
  { path: '/login', component: () => import('../views/LoginView.vue') },
  { path: '/home', component: () => import('../views/HomeView.vue') },
  {
    path: '/teacher/lessons',
    component: () => import('../views/TeacherLessonsView.vue'),
    meta: { role: 'TEACHER' }
  },
  {
    path: '/teacher/classrooms',
    component: () => import('../views/TeacherClassroomsView.vue'),
    meta: { role: 'TEACHER' }
  },
  {
    path: '/teacher/prepare-center',
    component: () => import('../views/TeacherPrepareCenterView.vue'),
    meta: { role: 'TEACHER' }
  },  {
    path: '/teacher/prepare/:id',
    component: () => import('../views/TeacherPrepareView.vue'),
    meta: { role: 'TEACHER' }
  },
  {
    path: '/teacher/classroom/:id',
    component: () => import('../views/TeacherClassroomView.vue'),
    meta: { role: 'TEACHER' }
  },
  {
    path: '/student/join',
    component: () => import('../views/StudentJoinView.vue'),
    meta: { role: 'STUDENT' }
  },
  {
    path: '/student/classroom/:id',
    component: () => import('../views/StudentClassroomView.vue'),
    meta: { role: 'STUDENT' }
  },
  {
    path: '/student/videos',
    component: () => import('../views/StudentVideosView.vue'),
    meta: { role: 'STUDENT' }
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.path === '/login') {
    return session.token ? homeForRole() : true
  }
  if (!session.token || !session.user) {
    return '/login'
  }
  if (to.meta.role && to.meta.role !== session.user.role) {
    return homeForRole()
  }
  return true
})

export default router