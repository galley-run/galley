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

      <div class="flex gap-2.5" :class="steps['1'] === 'waiting' && 'opacity-50'">
        <Forward2 v-if="steps['1'] === 'waiting'" />
        <LoadingIndicator v-if="steps['1'] === 'loading'" />
        <CheckCircle v-if="steps['1'] === 'ready'" />
        <div>
          <h4 class="text-navy-700">Launching your Vessel</h4>
          <p class="text-tides-900">
            Watch out for the splash zone! We are now creating your cluster and your first
            namespaces.
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
            We’ll add your first customer environment (Charter) and website (Project) to set sail
            with Galley.
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
import { CheckCircle, ConfettiMinimalistic, Forward2 } from '@solar-icons/vue'
import { onBeforeUnmount, onMounted, reactive } from 'vue'
import LoadingIndicator from '@/assets/LoadingIndicator.vue'
import UIButton from '@/components/UIButton.vue'
import UIConfetti from '@/components/UIConfetti.vue'

type StepId = 1 | 2 | 3
type StepState = 'waiting' | 'loading' | 'ready'

const steps = reactive<Record<StepId, StepState>>({
  1: 'waiting',
  2: 'waiting',
  3: 'waiting',
})

const timers: number[] = []

onMounted(() => {
  timers.push(
    window.setTimeout(() => {
      steps[1] = 'loading'
    }, 500),
  )
  timers.push(
    window.setTimeout(() => {
      steps[1] = 'ready'
      steps[2] = 'loading'
    }, 1200),
  )
  timers.push(
    window.setTimeout(() => {
      steps[2] = 'ready'
      steps[3] = 'loading'
    }, 2500),
  )
  timers.push(
    window.setTimeout(() => {
      steps[3] = 'ready'
    }, 3300),
  )
})

onBeforeUnmount(() => {
  timers.forEach(clearTimeout)
})
</script>
