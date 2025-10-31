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
import { useOnboardingStore } from '@/stores/onboarding.ts'
import router from '@/router'

const onboardingStore = useOnboardingStore()

switch(false) {
  case onboardingStore.completed.checkIn:
    router.push("/onboarding")
    break
  case onboardingStore.completed.firstCharter:
    router.push("/onboarding/first-charter")
    break
  case onboardingStore.completed.namingCeremony:
    router.push("/onboarding/naming-ceremony")
    break
  case onboardingStore.completed.securityScreening:
    router.push("/onboarding/security-screening")
    break
}
</script>
