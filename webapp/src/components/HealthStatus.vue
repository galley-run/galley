<template>
  <div class="flex gap-2 items-center truncate">
    <SmileSquare v-if="variant === 'ok'" class="text-seafoam-600" />
    <ExpressionlessSquare v-else-if="variant === 'unknown'" class="text-cliona-600" />
    <SadSquare v-else-if="variant === 'warning'" class="text-cliona-600" />
    <ConfoundedSquare v-else class="text-coral-600" />
    <h5>{{ variant.toUpperCase() }}</h5>
  </div>
</template>
<script setup lang="ts">
import { SmileSquare, SadSquare, ExpressionlessSquare, ConfoundedSquare } from '@solar-icons/vue'

const { variant } = defineProps<{ variant: 'ok' | 'warning' | 'error' | 'unknown' }>()
</script>
