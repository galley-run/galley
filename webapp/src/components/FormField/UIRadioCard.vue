<template>
  <label
    :aria-label="title"
    :aria-description="description"
    class="group relative border flex flex-col gap-2.5 rounded-2xl p-4 not-has-checked:border-tides-200 has-checked:border-navy-200 has-checked:bg-navy-50 has-disabled:opacity-30 group has-focus-visible:outline-1 not-has-disabled:hover:outline-1 not-has-disabled:cursor-pointer outline-navy-100 outline-offset-1"
  >
    <span class="block flex-1">
      <span class="flex justify-between">
        <span class="block leading-relaxed heading-h4 text-navy-700">{{ title }}</span>
        <CheckCircle
          class="hidden group-has-checked:block group-has-focus-visible:block group-has-focus-visible:group-not-has-checked:opacity-30 group-not-has-disabled:group-hover:group-not-has-checked:block group-hover:group-not-has-checked:opacity-30"
        />
      </span>
      <span class="mt-1 block text-tides-800">{{ description }}</span>
    </span>
    <input
      type="radio"
      name="mailing-list"
      v-bind="$attrs"
      v-model="model"
      @input="onInput"
      @change="onChange"
      class="absolute appearance-none focus:outline-none"
    />
  </label>
</template>
<script setup lang="ts">
import { CheckCircle } from '@solar-icons/vue'

defineOptions({
  inheritAttrs: false,
})

const model = defineModel<string>({ default: '' })
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
  const v = (e.target as HTMLInputElement).value
  model.value = v
  emit('change', v)
}

const { title, description } = defineProps<{
  title: string
  description: string
}>()
</script>
