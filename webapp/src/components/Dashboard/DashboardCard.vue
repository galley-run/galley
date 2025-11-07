<template>
  <div class="card card--compact">
    <h6 class="text-seafoam-300">{{ title }}</h6>
    <div class="flex gap-2.5 items-center heading-h5">
      <slot v-if="!loading" />
      <UISkeleton class="max-w-10" v-else />
    </div>
  </div>
</template>
<script setup lang="ts">
import UISkeleton from '@/components/FormField/UISkeleton.vue'

const { title, loading } = defineProps<{ title: string, loading?: boolean }>()
</script>
