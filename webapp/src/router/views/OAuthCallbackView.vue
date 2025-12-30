<template>
  <div class="flex min-h-screen items-center justify-center">
    <div class="card max-w-md">
      <div class="flex flex-col items-center gap-4 text-center">
        <div v-if="isProcessing" class="flex flex-col items-center gap-4">
          <div class="h-12 w-12 animate-spin rounded-full border-4 border-navy-200 border-t-navy-600"></div>
          <h2>Connecting your {{ providerName }}</h2>
          <p class="text-navy-500">Please wait while we complete the authentication...</p>
        </div>

        <div v-else-if="error" class="flex flex-col items-center gap-4">
          <div class="flex h-12 w-12 items-center justify-center rounded-full bg-red-100">
            <span class="text-2xl text-red-600">✕</span>
          </div>
          <h2 class="text-red-600">Connection Failed</h2>
          <p class="text-navy-500">{{ error }}</p>
          <UIButton to="/application/add">Try Again</UIButton>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMutation } from '@tanstack/vue-query'
import { useVesselId, useCharterId } from '@/composables/useResourceHelpers'
import { useAuthStore } from '@/stores/auth'
import axios from 'axios'
import UIButton from '@/components/UIButton.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const vesselId = useVesselId()
const charterId = useCharterId()

const isProcessing = ref(true)
const error = ref<string | null>(null)

const provider = computed(() => route.params.provider as string)
const providerName = computed(() => {
  const name = provider.value
  return name.charAt(0).toUpperCase() + name.slice(1)
})

// Check if we're in a popup
const isPopup = computed(() => {
  return window.opener && window.opener !== window
})

// Mutation to approve the OAuth connection
const approveConnection = useMutation({
  mutationFn: async (params: Record<string, string>) => {
    return axios.post(
      `/vessels/${vesselId.value}/charters/${charterId.value}/connections/approval`,
      {
        provider: provider.value,
        ...params,
      }
    )
  },
  onSuccess: () => {
    if (isPopup.value) {
      // Send message to parent window and close popup
      window.opener.postMessage({ type: 'oauth-success' }, window.location.origin)
      // Give time for message to be received
      setTimeout(() => window.close(), 100)
    } else {
      // Fallback: navigate to step 3
      router.push('/application/add?step=3')
    }
  },
  onError: (err: any) => {
    console.error('OAuth approval failed:', err)
    const errorMessage = err.response?.data?.message || 'Failed to complete authentication. Please try again.'

    if (isPopup.value) {
      // Send error to parent window
      window.opener.postMessage({ type: 'oauth-error', error: errorMessage }, window.location.origin)
      setTimeout(() => window.close(), 100)
    } else {
      error.value = errorMessage
      isProcessing.value = false
    }
  },
})

onMounted(async () => {
  console.log('OAuth callback mounted')
  console.log('Is popup?', isPopup.value)

  // Extract all query parameters from the OAuth redirect
  const queryParams = route.query as Record<string, string>

  console.log('Query params:', queryParams)

  if (!queryParams || Object.keys(queryParams).length === 0) {
    error.value = 'No authentication data received from provider.'
    isProcessing.value = false
    return
  }

  if (isPopup.value) {
    // If in popup, send params to parent window and let it handle the approval call
    console.log('Sending OAuth params to parent window')
    window.opener.postMessage(
      {
        type: 'oauth-callback',
        provider: provider.value,
        params: queryParams,
      },
      window.location.origin
    )
    // Close popup after sending data
    setTimeout(() => window.close(), 100)
  } else {
    // Fallback: if not in popup, do the approval call here
    // Ensure auth token is set in axios headers
    if (authStore.accessToken) {
      axios.defaults.headers.common['Authorization'] = `Bearer ${authStore.accessToken}`
    }

    // Post the query parameters to the backend
    approveConnection.mutate(queryParams)
  }
})
</script>
