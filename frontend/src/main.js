import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router/index.js'
import './styles/main.css'
import './styles/visual-refresh.css'
import './styles/teacher-views.css'
import { initAccessibilitySettings } from './accessibility/settings.js'

initAccessibilitySettings()

createApp(App).use(router).use(ElementPlus).mount('#app')