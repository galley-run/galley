<template>
  <RouterLink class="flex gap-3 border-b border-b-tides-300 -mx-8 px-8 -mt-8 pt-6 pb-5 mb-2 items-center-safe text-tides-800 hover:text-tides-900 hover:no-underline hover:bg-tides-50 transition-all">
    <UndoLeftRound class="text-navy-800" />
    <h6>Back to <slot /></h6>
  </RouterLink>
</template>

<script setup lang="ts">
import { UndoLeftRound } from '@solar-icons/vue'
</script>
