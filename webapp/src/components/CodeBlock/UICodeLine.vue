<template>
  <div class="flex gap-2 items-start relative">
    <div class="text-navy-300 select-none" v-if="!comment && !empty">$</div>
    <div class="text-navy-300 select-none" v-if="comment">#</div>
    <div v-if="empty" class="h-5 select-none" />
    <div
      v-if="copied"
         class="absolute opacity-0 animate-fade-flash inset-y-0 left-4 bg-linear-to-r from-navy-900 from-70% to-navy-900/0 pl-4 pr-32 select-none"
    >
      Copied to clipboard</div>
    <button :class="comment && 'select-none pointer-events-none'" class="hover:text-white hover:cursor-pointer" @click="copyLine">
      <slot />
    </button>
  </div>
</template>
<script setup lang="ts">
import { ref } from 'vue'

const { comment, empty } = defineProps<{ comment?: boolean, empty?: boolean }>()

const copied = ref(false)

function copyLine(event: Event) {
  const text = (event.target as HTMLElement).textContent || ''
  navigator.clipboard.writeText(text)
  copied.value = true
  setTimeout(() => copied.value = false, 1600)
}
</script>
