<template>
  <header
    class="flex w-full border-b border-b-tides-200 py-5 px-6 justify-between gap-4 items-center"
  >
    <div class="flex gap-12 items-center">
      <GalleyLogo />
      <ProjectSwitcher />
    </div>
    <div class="flex gap-2">
      <Button :leading-addon="darkModeIcon" variant="neutral" @click="toggleDarkMode" ghost />
      <Button :leading-addon="MinimalisticMagnifer" variant="neutral" ghost />
      <Button :leading-addon="Logout" variant="neutral" ghost />
    </div>
  </header>
</template>
<script setup lang="ts">
import GalleyLogo from '@/assets/GalleyLogo.vue'
import ProjectSwitcher from '@/components/MainLayout/ProjectSwitcher.vue'
import { Logout, MinimalisticMagnifer, Moon, Sun2, SunFog } from '@solar-icons/vue'
import Button from '@/components/Button.vue'
import { ref, watch } from 'vue'

const darkMode = ref(localStorage.theme ?? 'auto')
const darkModeIcon = ref(SunFog)

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

document.documentElement.classList.toggle(
  'dark',
  localStorage.theme === 'dark' ||
    (!('theme' in localStorage) && window.matchMedia('(prefers-color-scheme: dark)').matches),
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
