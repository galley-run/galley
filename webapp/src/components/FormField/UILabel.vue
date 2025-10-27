<template>
  <label :for="labelFor">
    <slot />
    <span v-if="required" class="required">*</span>
    <UIButton
      ghost
      :leading-addon="InfoCircle"
      v-if="hasInfoClick"
      variant="neutral"
      @click="emit('infoClick', $event)"
    />
  </label>
</template>
<script setup lang="ts">
import { InfoCircle } from '@solar-icons/vue'
import UIButton from '@/components/UIButton.vue'
import { computed, getCurrentInstance } from 'vue'

const { for: labelFor, required } = defineProps<{ for: string; required?: boolean }>()
const emit = defineEmits<{ (e: 'infoClick', event: MouseEvent): void }>()

const i = getCurrentInstance()

const hasInfoClick = computed(() => {
  const p = i?.vnode.props as Record<string, unknown> | null | undefined
  if (!p) return false
  return (
    typeof (p.onInfoClick as unknown) === 'function' ||
    typeof (p['onInfo-click'] as unknown) === 'function'
  )
})
</script>
