import { reactive, watch } from 'vue'

export const ACCESSIBILITY_STORAGE_KEY = 'hp_accessibility_settings_v1'

function prefersReducedMotion() {
  return typeof window !== 'undefined' && window.matchMedia?.('(prefers-reduced-motion: reduce)').matches === true
}

const defaults = Object.freeze({
  captionFontSize: 42,
  captionLineHeight: 1.35,
  highContrast: false,
  reduceMotion: prefersReducedMotion(),
  theme: 'light'
})

function clamp(value, min, max, fallback) {
  const number = Number(value)
  return Number.isFinite(number) ? Math.min(max, Math.max(min, number)) : fallback
}

function readSettings() {
  try {
    const saved = JSON.parse(localStorage.getItem(ACCESSIBILITY_STORAGE_KEY) || 'null')
    if (!saved || typeof saved !== 'object') return {}
    return {
      captionFontSize: clamp(saved.captionFontSize, 28, 64, defaults.captionFontSize),
      captionLineHeight: clamp(saved.captionLineHeight, 1.2, 2, defaults.captionLineHeight),
      highContrast: Boolean(saved.highContrast),
      reduceMotion: saved.reduceMotion == null ? defaults.reduceMotion : Boolean(saved.reduceMotion),
      theme: saved.theme === 'dark' ? 'dark' : 'light'
    }
  } catch {
    return {}
  }
}

export const accessibilitySettings = reactive({ ...defaults, ...readSettings() })

function applySettings() {
  if (typeof document === 'undefined') return
  const root = document.documentElement
  const fontSize = clamp(accessibilitySettings.captionFontSize, 28, 64, defaults.captionFontSize)
  const lineHeight = clamp(accessibilitySettings.captionLineHeight, 1.2, 2, defaults.captionLineHeight)

  root.dataset.theme = accessibilitySettings.theme === 'dark' ? 'dark' : 'light'
  root.dataset.contrast = accessibilitySettings.highContrast ? 'high' : 'normal'
  root.dataset.reduceMotion = accessibilitySettings.reduceMotion ? 'true' : 'false'
  root.style.colorScheme = accessibilitySettings.theme === 'dark' ? 'dark' : 'light'
  root.style.setProperty('--caption-font-size', `${fontSize}px`)
  root.style.setProperty('--caption-line-height', String(lineHeight))
  root.style.setProperty('--caption-history-font-size', `${Math.max(15, Math.round(fontSize * 0.4))}px`)
}

function persistSettings() {
  try {
    localStorage.setItem(ACCESSIBILITY_STORAGE_KEY, JSON.stringify(accessibilitySettings))
  } catch {
    // Browsers can deny storage in private or restricted contexts; live settings still work.
  }
}

let initialized = false

export function initAccessibilitySettings() {
  if (initialized) return
  initialized = true
  applySettings()
  watch(accessibilitySettings, () => {
    applySettings()
    persistSettings()
  }, { deep: true })

  window.addEventListener('storage', (event) => {
    if (event.key !== ACCESSIBILITY_STORAGE_KEY || !event.newValue) return
    try {
      Object.assign(accessibilitySettings, readSettings())
    } catch {
      // Ignore malformed settings from another tab.
    }
  })
}

export function resetAccessibilitySettings() {
  Object.assign(accessibilitySettings, {
    ...defaults,
    reduceMotion: prefersReducedMotion()
  })
}