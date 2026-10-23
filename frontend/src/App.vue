<template>
  <router-view />
  <MobileBottomNav v-if="session.user?.role === 'STUDENT' && route.path !== '/login'" />
  <SignAvatarWidget
    v-if="session.user?.role === 'STUDENT' && route.path !== '/login'"
    ref="avatarRef"
    initial-text="欢迎使用，祝你学习顺利"
    sign-text="欢迎使用，祝你学习顺利"
    @state-change="updateAvatarState"
  />
</template>

<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import MobileBottomNav from './components/MobileBottomNav.vue'
import SignAvatarWidget from './components/SignAvatarWidget.vue'
import { bindAvatarController, resetAvatarState, updateAvatarState } from './avatar/avatarStore.js'
import { session } from './auth/session.js'

const route = useRoute()
const avatarRef = ref(null)
let unbindAvatar = null

watch(avatarRef, (api) => {
  unbindAvatar?.()
  unbindAvatar = null
  if (api) {
    unbindAvatar = bindAvatarController(api)
  } else {
    resetAvatarState()
  }
}, { flush: 'post' })

onBeforeUnmount(() => {
  unbindAvatar?.()
  resetAvatarState()
})
</script>
