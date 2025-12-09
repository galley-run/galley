<template>
  <div class="text-input flex items-center">
    <component
      :is="leadingAddon"
      :size="20"
      v-if="leadingAddon && typeof leadingAddon !== 'string'"
    />
    <div v-if="leadingAddon && typeof leadingAddon === 'string'">{{ leadingAddon }}</div>
    <input v-bind="$attrs" v-model="model" @input="onInput" @change="onChange" />
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

defineOptions({
  inheritAttrs: false,
})

const model = defineModel<string>({ default: '' })

const { leadingAddon, trailingAddon, format } = defineProps<{
  leadingAddon?: FunctionalComponent<IconProps> | string
  trailingAddon?: FunctionalComponent<IconProps> | string
  format?: 'money'
}>()

const emit = defineEmits<{
  (e: 'input', v: string): void
  (e: 'change', v: string): void
}>()

function onInput(e: Event) {
  const v = (e.target as HTMLInputElement).value
  model.value = v
  emit('input', v)
}

function onChange(e: Event) {
  let v: string | number = (e.target as HTMLInputElement).value
  if (format === 'money') {
    v = v.replace(',', '.')
    if (isNaN(Number(v))) {
      v = 0
    }

    // TODO: use locale from user settings
    v = Intl.NumberFormat("nl-NL", { style: 'decimal', minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(Number(v))
  }

  model.value = v
  emit('change', v)
}
</script>
