<template>
  <TopBar />
  <div class="flex min-h-main">
    <MainNavigation />
    <main class="bg-tides-50 w-full p-8">
      <router-view v-if="isAuthenticated" />
      <div v-else class="flex justify-center items-center h-full">
        <LoadingIndicator class="size-12" />
      </div>
    </main>

  </div>
</template>
<script setup lang="ts">
import TopBar from '@/components/MainLayout/TopBar.vue'
import MainNavigation from '@/components/MainLayout/MainNavigation.vue'
import { useAuthStore } from '@/stores/auth.ts'
import { storeToRefs } from 'pinia'
import LoadingIndicator from '@/assets/LoadingIndicator.vue'

const authStore = useAuthStore()

const { isAuthenticated } = storeToRefs(authStore)
</script>
