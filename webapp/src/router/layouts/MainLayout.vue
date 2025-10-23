<template>
  <TopBar />
  <div class="flex min-h-main">
    <MainNavigation />
    <main>
      <router-view />
    </main>
  </div>
</template>
<script setup lang="ts">
import TopBar from '@/components/TopBar.vue'
import MainNavigation from '@/components/MainNavigation.vue'
</script>
