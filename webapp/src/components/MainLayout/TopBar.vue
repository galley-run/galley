<template>
  <header class="flex w-full border-b border-b-tides-200 py-5 px-6 justify-between gap-4 items-center">
    <div class="flex gap-12 items-center">
      <GalleyLogo />
      <ProjectSwitcher />
    </div>
    <div class="flex gap-2">
      <Button :leading-addon="MinimalisticMagnifer" variant="neutral" ghost />
      <Button :leading-addon="Logout" variant="neutral" ghost />
    </div>
  </header>
</template>
<script setup lang="ts">
import GalleyLogo from '@/assets/GalleyLogo.vue'
import ProjectSwitcher from '@/components/MainLayout/ProjectSwitcher.vue'
import { MinimalisticMagnifer, Logout, MenuDotsCircle } from '@solar-icons/vue'
import Button from '@/components/Button.vue'
</script>
