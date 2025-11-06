import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './style.css'
import 'vue3-flag-icons/styles'
import { SolarIconsPlugin } from '@solar-icons/vue/lib'

import App from './App.vue'
import router from './router'
import { VueQueryPlugin } from '@tanstack/vue-query'
import registerAxios from '@/utils/registerAxios.ts'
import { initAuthStore } from '@/stores/auth.ts'
import { initProjectsStore } from '@/stores/projects.ts'

document.documentElement.classList.toggle(
  'dark',
  localStorage.theme === 'dark' ||
    (!('theme' in localStorage) && window.matchMedia('(prefers-color-scheme: dark)').matches),
)

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(VueQueryPlugin)

registerAxios()
initAuthStore()
initProjectsStore()

app.use(SolarIconsPlugin, {
  weight: 'LineDuotone',
})

app.mount('#app')
