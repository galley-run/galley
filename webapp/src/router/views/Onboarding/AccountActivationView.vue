<template>
  <h1>Activate your account.</h1>

  <LoadingIndicator v-if="isPending" class="size-12" />
  <div v-if="isSuccess" class="space-y-4">
    <p class="text-seafoam-700">Your account has been activated!</p>
    <UIButton class="inline-flex" to="/">Continue to Galley</UIButton>
  </div>
  <div v-if="isError" class="space-y-4">
    <p class="text-coral-500">Something went wrong</p>
    <UIButton class="inline-flex" @click="$router.go(0)">Try again</UIButton>
  </div>

</template>
<script setup lang="ts">
import LoadingIndicator from '@/assets/LoadingIndicator.vue'
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useMutation } from '@tanstack/vue-query'
import axios from 'axios'
import UIButton from '@/components/UIButton.vue'

const route = useRoute()

interface AccountActivation {
  crewId: string | undefined
  vesselId: string | undefined
  userId: string | undefined
  activationSalt: string | undefined
}

const { isPending, isSuccess, isError, mutateAsync } = useMutation({
  mutationFn: ({ crewId, vesselId, userId, activationSalt }: AccountActivation) =>
    axios.post('/auth/activate', { crewId, vesselId, userId, activationSalt }),
})

onMounted(() => {
  const hash = (route.params.hash ?? '').toString()
  const [crewId, vesselId, userId, activationSalt] = atob(hash).split('.')

  mutateAsync({ crewId, vesselId, userId, activationSalt })
})
</script>
