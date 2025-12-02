<template>
  <h1>Goodbye!</h1>

  <div class="space-y-4">
    <p class="text-seafoam-700">You've been signed out!</p>
    <UIButton class="inline-flex" to="/login">Sign into Galley again</UIButton>
  </div>
</template>
<script setup lang="ts">
import { onMounted } from 'vue'
import UIButton from '@/components/UIButton.vue'
import { useAuthStore } from '@/stores/auth.ts'

const authStore = useAuthStore()

onMounted(() => {
  authStore.signOut()
})
</script>
