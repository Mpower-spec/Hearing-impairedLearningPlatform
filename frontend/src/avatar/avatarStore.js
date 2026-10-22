import { reactive } from 'vue'

export const avatarState = reactive({
  ready: false,
  hidden: false,
  playing: false,
  hasError: false
})

let controller = null

export function bindAvatarController(api) {
  controller = api
  return () => {
    if (controller === api) controller = null
  }
}

export function updateAvatarState(state) {
  Object.assign(avatarState, state)
}

export function resetAvatarState() {
  Object.assign(avatarState, {
    ready: false,
    hidden: false,
    playing: false,
    hasError: false
  })
}

export function pushSignCaption(text, captionId) {
  controller?.pushCaption(text, captionId)
}

export function clearSignCaptions() {
  controller?.clearCaptions()
}

export function replayAvatar() {
  controller?.replay()
}

export function toggleAvatar() {
  if (avatarState.hidden) controller?.showAvatar()
  else controller?.hideAvatar()
}

export function retryAvatar() {
  controller?.retry()
}
