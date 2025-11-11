<template>
  <div class="bg-navy-900 text-white rounded-2xl p-8">
    <div class="flex gap-2 items-center mb-4">
      <RoundDoubleAltArrowRight />

      <h3 v-if="title" class="select-none">
        {{ title }}
      </h3>
    </div>
    <div class="font-mono text-sm font-medium leading-relaxed text-navy-100">
      <slot />
    </div>
  </div>
</template>
<script setup lang="ts">
import { RoundDoubleAltArrowRight } from '@solar-icons/vue'

const { title } = defineProps<{ title?: string }>()
</script>
