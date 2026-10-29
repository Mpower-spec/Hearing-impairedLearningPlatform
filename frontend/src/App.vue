<template>
  <AmbientBackdrop v-if="!isPublicPage" />
  <router-view />
  <MobileBottomNav v-if="showStudentChrome" />
  <SignAvatarWidget
    v-if="showStudentChrome"
    ref="avatarRef"
    initial-text="欢迎使用，祝你学习顺利"
    sign-text="欢迎使用，祝你学习顺利"
    @state-change="updateAvatarState"
  />
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AmbientBackdrop from './components/AmbientBackdrop.vue'
import MobileBottomNav from './components/MobileBottomNav.vue'
import SignAvatarWidget from './components/SignAvatarWidget.vue'
import { bindAvatarController, resetAvatarState, updateAvatarState } from './avatar/avatarStore.js'
import { session } from './auth/session.js'

const route = useRoute()
const avatarRef = ref(null)
let unbindAvatar = null
const isPublicPage = computed(() => route.meta.public === true)
const showStudentChrome = computed(() => session.user?.role === 'STUDENT' && !isPublicPage.value)

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
