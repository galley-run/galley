<template>
  <div class="card card--compact">
    <h6 class="text-seafoam-300">{{ title }}</h6>
    <div class="flex gap-2.5 items-center heading-h5">
      <slot />
    </div>
  </div>
</template>
<script setup lang="ts">
const { title } = defineProps<{ title: string }>()
</script>
