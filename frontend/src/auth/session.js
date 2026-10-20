import { reactive } from 'vue'

function readUser() {
  try {
    return JSON.parse(localStorage.getItem('hp_user') || 'null')
  } catch {
    return null
  }
}

export const session = reactive({
  token: localStorage.getItem('hp_token') || '',
  user: readUser()
})

export function setSession(token, user) {
  session.token = token
  session.user = user
  localStorage.setItem('hp_token', token)
  localStorage.setItem('hp_user', JSON.stringify(user))
}

export function clearSession() {
  session.token = ''
  session.user = null
  localStorage.removeItem('hp_token')
  localStorage.removeItem('hp_user')
}

export function homeForRole() {
  return '/home'
}
