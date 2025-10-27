<template>
  <div class="text-input flex items-center">
    <component
      :is="leadingAddon"
      :size="20"
      v-if="leadingAddon && typeof leadingAddon !== 'string'"
    />
    <div v-if="leadingAddon && typeof leadingAddon === 'string'">{{ leadingAddon }}</div>
    <input v-bind="$attrs" :value="modelValue" @input="onInput" @change="onChange" />
    <component
      :is="trailingAddon"
      :size="20"
      v-if="trailingAddon && typeof trailingAddon !== 'string'"
    />
    <div v-if="trailingAddon && typeof trailingAddon === 'string'">{{ trailingAddon }}</div>
  </div>
</template>
<script setup lang="ts">
import type { FunctionalComponent } from 'vue'
import type { IconProps } from '@solar-icons/vue/lib'

const { leadingAddon, trailingAddon } = withDefaults(
  defineProps<{
    modelValue: string
    leadingAddon?: FunctionalComponent<IconProps> | string
    trailingAddon?: FunctionalComponent<IconProps> | string
  }>(),
  {
    modelValue: '',
  },
)

const emit = defineEmits<{
  (e: 'update:modelValue', v: string): void
  (e: 'input', v: string): void
  (e: 'change', v: string): void
}>()

function onInput(e: Event) {
  const v = (e.target as HTMLInputElement).value
  emit('update:modelValue', v)
  emit('input', v)
}

function onChange(e: Event) {
  const v = (e.target as HTMLInputElement).value
  emit('update:modelValue', v)
  emit('change', v)
}
</script>
