<template>
  <TopBar />
  <div class="flex min-h-main">
    <MainNavigation />
    <main class="bg-tides-50 w-full p-8">
      <router-view />
    </main>
  </div>
</template>
<script setup lang="ts">
import TopBar from '@/components/MainLayout/TopBar.vue'
import MainNavigation from '@/components/MainLayout/MainNavigation.vue'
</script>
