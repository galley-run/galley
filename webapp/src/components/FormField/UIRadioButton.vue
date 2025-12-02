<template>
  <label
    :aria-label="label"
    :aria-description="description"
    class="group/radio-button relative flex flex-row gap-2.5 rounded-2xl py-2 has-disabled:opacity-30 not-has-disabled:cursor-pointer items-center"
  >
    <span class="flex-initial group-not-has-checked/radio-button:bg-tides-50 group-hover/radio-button:group-not-has-checked/radio-button:border-navy-300 group-has-focus/radio-button:outline outline-offset-2 outline-navy-200 bg-navy-600 rounded-full size-5.5 border border-navy-700 group-not-has-checked/radio-button:border-tides-600 group-active/radio-button:group-not-has-checked/radio-button:border-tides-800 flex items-center justify-center transition-all group-has-[:user-invalid]:bg-red-50 group-has-[:user-invalid]:border-red-200">
      <span
        class="opacity-0 group-active/radio-button:group-not-has-checked/radio-button:ring-4 group-active/radio-button:group-not-has-checked/radio-button:bg-white ring-navy-300 group-has-checked/radio-button:opacity-100 block group-has-focus-visible/radio-button:opacity-100 group-has-focus-visible/radio-button:group-not-has-checked/radio-button:opacity-30 group-not-has-disabled/radio-button:group-hover/radio-button:group-not-has-checked/radio-button:opacity-100 group-hover/radio-button:group-not-has-checked/radio-button:bg-navy-200 bg-white size-2 rounded-full transition-all"
      />
    </span>
    <span class="block flex-1">
      <span class="flex">
        <span class="block form-field-label-bold text-navy-700">{{ label }}</span>
      </span>
      <span class="mt-1 block form-field-help text-tides-900">{{ description }}</span>
    </span>
    <input
      type="radio"
      v-bind="$attrs"
      v-model="model"
      @input="onInput"
      @change="onChange"
      class="absolute appearance-none focus:outline-none"
    />
  </label>
</template>
<script setup lang="ts">
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

const { label, description } = defineProps<{
  label: string
  description: string
}>()
</script>
