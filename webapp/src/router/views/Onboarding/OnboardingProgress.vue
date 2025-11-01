<template>
  <aside class="xl:bg-navy-500 xl:dark:bg-navy-50 rounded-4xl flex justify-center xl:p-8 xl:flex-col">
    <ul class="flex xl:flex-col">
      <li
        v-for="step in steps"
        :key="step.title"
        class="flex flex-col group text-white dark:text-navy-600 relative transition-all"
        :class="$route.fullPath != step.page && 'opacity-50 hover:opacity-100'"
      >
        <div class="relative not-group-last:mr-8 xl:not-group-last:mb-8 xl:mr-0">
          <span
            class="group-last:hidden absolute w-full h-0.5 top-4 left-6 xl:top-4 xl:left-4 -ml-px xl:h-full xl:w-0.5 bg-navy-400 dark:bg-navy-100"
            aria-hidden="true"
          />
          <div class="relative flex space-x-2 xl:mb-2">
            <div class="bg-navy-400 rounded-full p-1 ring-tides-50 xl:ring-navy-500 xl:dark:ring-navy-50 dark:bg-navy-100 ring-8">
              <component :is="step.icon" />
            </div>
            <div class="min-w-0 flex-1 justify-between py-0.75 hidden xl:flex">
              <h3>{{ step.title }}</h3>
            </div>
          </div>
          <div class="ml-10 hidden xl:block">{{ step.description }}</div>
        </div>
        <RouterLink :to="step.page" class="absolute inset-0" />
      </li>
    </ul>
  </aside>
</template>
<script setup lang="ts">
import { Clipboard, ConfettiMinimalistic, ShieldStar, SuitcaseTag, UserId } from '@solar-icons/vue'

const steps = [
  {
    icon: UserId,
    title: 'Check-in & Registration',
    description: 'Introduce yourself and choose how you want to start.',
    page: '/onboarding'
  },
  {
    icon: Clipboard,
    title: 'Security Screening',
    description: 'Tell us about your role, industry and crew size.',
    page: '/onboarding/security-screening'
  },
  {
    icon: ShieldStar,
    title: 'Naming Ceremony',
    description: 'Provide your business details and name your vessel.',
    page: '/onboarding/naming-ceremony'
  },
  {
    icon: ConfettiMinimalistic,
    title: 'Your First Charter',
    description: 'Create your first customer and project environment.',
    page: '/onboarding/first-charter'
  },
  {
    icon: SuitcaseTag,
    title: 'Boarding the Vessel',
    description: 'We’ll set everything up and welcome you aboard.',
    page: '/onboarding/boarding'
  },
]
</script>
