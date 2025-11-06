<template>
  <TopBar />
  <div class="bg-tides-50 xl:min-h-main grid grid-cols-1 xl:grid-cols-2 gap-8 p-8">
    <LoginHero />
    <main class="card rounded-4xl flex justify-center xl:py-12 relative">
      <router-view />
    </main>
  </div>
</template>
<script setup lang="ts">
import TopBar from '@/components/MainLayout/TopBar.vue'
import LoginHero from '@/router/views/Login/LoginHero.vue'
</script>
