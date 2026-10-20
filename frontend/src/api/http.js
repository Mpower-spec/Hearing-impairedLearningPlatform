import axios from 'axios'
import { ElMessage } from 'element-plus'
import { session, clearSession } from '../auth/session.js'

const http = axios.create({
  baseURL: '/api',
  timeout: 180000
})

http.interceptors.request.use((config) => {
  if (session.token) {
    config.headers.Authorization = `Bearer ${session.token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response.data?.data,
  (error) => {
    const silent = error.config?.silent
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || '请求失败'
    if (status === 401) {
      clearSession()
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    if (!silent) {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

export default http