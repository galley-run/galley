<template>
  <TopBar />
  <div class="bg-tides-50 xl:min-h-main grid grid-cols-1 xl:grid-cols-3 gap-8 p-8">
    <OnboardingProgress />
    <main class="xl:col-span-2 card rounded-4xl flex justify-center xl:py-12 relative">
      <router-view />
    </main>
  </div>
</template>
<script setup lang="ts">
import TopBar from '@/components/MainLayout/TopBar.vue'
import OnboardingProgress from '@/router/views/Onboarding/OnboardingProgress.vue'
</script>
