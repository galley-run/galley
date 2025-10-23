<template>
  <header class="flex w-full border-b border-b-tides-200 py-5 px-6 justify-between gap-4 items-center">
    <div class="flex gap-12 items-center">
      <GalleyLogo />
      <ProjectSwitcher />
    </div>
    <div class="flex gap-6">
      <MinimalisticMagnifer weight="LineDuotone" />
      <Logout weight="LineDuotone" />
    </div>
  </header>
</template>
<script setup lang="ts">
import GalleyLogo from '@/assets/GalleyLogo.vue'
import ProjectSwitcher from '@/components/ProjectSwitcher.vue'
import { MinimalisticMagnifer, Logout } from '@solar-icons/vue'
</script>
