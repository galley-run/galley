<template>
  <header
    class="bg-white flex w-full border-b border-b-tides-200 py-5 px-6 justify-between gap-4 items-center"
  >
    <div class="flex gap-12 items-center">
      <GalleyLogo />
      <ProjectSwitcher v-if="isAuthenticated" />
    </div>
    <div class="flex gap-2">
      <UIButton :leading-addon="darkModeIcon" variant="neutral" @click="toggleDarkMode" ghost />
      <UIButton :leading-addon="MinimalisticMagnifer" v-if="isAuthenticated" variant="neutral" ghost />
      <UIButton :leading-addon="Logout" to="/logout" v-if="isAuthenticated" variant="neutral" ghost />
    </div>
  </header>
</template>
<script setup lang="ts">
import GalleyLogo from '@/assets/GalleyLogo.vue'
import ProjectSwitcher from '@/components/MainLayout/ProjectSwitcher.vue'
import { Logout, MinimalisticMagnifer, Moon, Sun2, SunFog } from '@solar-icons/vue'
import UIButton from '@/components/UIButton.vue'
import { ref, watch } from 'vue'
import { useAuthStore } from '@/stores/auth.ts'
import { storeToRefs } from 'pinia'

const darkMode = ref(localStorage.theme ?? 'auto')
const darkModeIcon = ref(SunFog)

const authStore = useAuthStore()
const { isAuthenticated } = storeToRefs(authStore)

watch(
  darkMode,
  (newValue) => {
    switch (newValue) {
      case 'light':
        darkModeIcon.value = Sun2
        break
      case 'dark':
        darkModeIcon.value = Moon
        break
      default:
        darkModeIcon.value = SunFog
    }
  },
  { immediate: true },
)

function toggleDarkMode() {
  if (!localStorage.theme) {
    localStorage.theme = 'dark'
  } else if (localStorage.theme === 'dark') {
    localStorage.theme = 'light'
  } else {
    localStorage.removeItem('theme')
  }

  darkMode.value = localStorage.theme ?? 'auto'

  document.documentElement.classList.toggle(
    'dark',
    localStorage.theme === 'dark' ||
      (!('theme' in localStorage) && window.matchMedia('(prefers-color-scheme: dark)').matches),
  )
}
</script>
