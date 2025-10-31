<template>
  <div class="absolute inset-0">
    <UIConfetti v-if="steps['3'] === 'ready'" autoplay />
  </div>
  <div class="relative flex flex-col gap-8 max-w-4xl">
    <div class="space-y-2">
      <h1>Boarding the Vessel</h1>
      <p class="text-tides-900">
        This is the moment we will launch your Vessel into Galley. We’ll set up your account, your
        business profile vessel, first charter & project. This may take a minute. Once we’re ready
        we welcome you onboard into Galley.
      </p>
    </div>

    <div
      class="flex gap-2.5"
      :class="!isSignUpPending && !isSignUpSuccess && !isSignUpError && 'opacity-50'"
    >
      <Forward2 v-if="!isSignUpPending && !isSignUpSuccess && !isSignUpError" />
      <LoadingIndicator v-if="isSignUpPending" />
      <CheckCircle v-if="isSignUpSuccess" />
      <DangerCircle v-if="isSignUpError" />
      <div>
        <h4 class="text-navy-700">Launching your Vessel</h4>
        <p class="text-tides-900" v-if="!signUpError?.message">
          Watch out for the splash zone! We are now creating your cluster and your first namespaces.
        </p>
        <p v-else class="text-coral-500">
          {{ signUpError }}
        </p>
      </div>
    </div>

    <div class="flex gap-2.5" :class="steps['2'] === 'waiting' && 'opacity-50'">
      <Forward2 v-if="steps['2'] === 'waiting'" />
      <LoadingIndicator v-if="steps['2'] === 'loading'" />
      <CheckCircle v-if="steps['2'] === 'ready'" />
      <div>
        <h4 class="text-navy-700">Creating your account</h4>
        <p class="text-tides-900">
          We’ll create your account and assign you the Captain of the Vessel
        </p>
      </div>
    </div>

    <div class="flex gap-2.5" :class="steps['3'] === 'waiting' && 'opacity-50'">
      <Forward2 v-if="steps['3'] === 'waiting'" />
      <LoadingIndicator v-if="steps['3'] === 'loading'" />
      <CheckCircle v-if="steps['3'] === 'ready'" />
      <div>
        <h4 class="text-navy-700">Embark with your first Charter and Project</h4>
        <p class="text-tides-900">
          We’ll add your first customer environment (Charter) and website (Project) to set sail with
          Galley.
        </p>
      </div>
    </div>
    <div class="form-footer justify-end">
      <UIButton
        to="/"
        :aria-disabled="steps['3'] !== 'ready'"
        :leading-addon="ConfettiMinimalistic"
      >
        Step aboard Galley
      </UIButton>
    </div>
  </div>
</template>

<script setup lang="ts">
import { CheckCircle, ConfettiMinimalistic, DangerCircle, Forward2 } from '@solar-icons/vue'
import { onBeforeUnmount, onMounted, reactive } from 'vue'
import LoadingIndicator from '@/assets/LoadingIndicator.vue'
import UIButton from '@/components/UIButton.vue'
import UIConfetti from '@/components/UIConfetti.vue'
import { useMutation } from '@tanstack/vue-query'
import axios from 'axios'
import {
  type Inquiry,
  type Intent,
  useOnboardingStore,
  type User,
  type Vessel,
} from '@/stores/onboarding.ts'
import { storeToRefs } from 'pinia'

type StepId = 1 | 2 | 3
type StepState = 'waiting' | 'loading' | 'ready'

const onboardingStore = useOnboardingStore()

const { user, inquiry, intent, vessel } = storeToRefs(onboardingStore)

const steps = reactive<Record<StepId, StepState>>({
  1: 'waiting',
  2: 'waiting',
  3: 'waiting',
})

const {
  isPending: isSignUpPending,
  isError: isSignUpError,
  error: signUpError,
  isSuccess: isSignUpSuccess,
  mutateAsync: mutateSignUp,
} = useMutation({
  mutationFn: (signUp: { vessel: Vessel; user: User; inquiry: Inquiry; intent: Intent }) =>
    axios.post('/auth/sign-up', signUp),
})

const timers: number[] = []

onMounted(async () => {
  await mutateSignUp({
    intent: intent.value,
    user: user.value,
    inquiry: inquiry.value,
    vessel: vessel.value,
  })
})

onBeforeUnmount(() => {
  timers.forEach(clearTimeout)
})
</script>
