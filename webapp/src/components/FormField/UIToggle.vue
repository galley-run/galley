<template>
  <label
    :aria-label="label"
    class="group/checkbox-button relative flex flex-row gap-2.5 rounded-2xl py-2 has-disabled:opacity-30 not-has-disabled:cursor-pointer items-center"
  >
    <span
      class="relative w-10 h-6 flex-initial group-not-has-checked/checkbox-button:bg-tides-50 group-hover/checkbox-button:group-not-has-checked/checkbox-button:border-navy-300 group-hover/checkbox-button:group-not-has-checked/checkbox-button:bg-navy-50 bg-navy-600 rounded-full border border-navy-700 group-not-has-checked/checkbox-button:border-tides-600 group-active/checkbox-button:group-not-has-checked/checkbox-button:border-tides-800 flex items-center transition-all"
    >
      <span
        class="flex absolute left-0 group-has-checked/checkbox-button:not-group-hover/checkbox-button:left-3.75 group-has-checked/checkbox-button:group-hover/checkbox-button:left-2.75 inset-y items-center justify-center group-hover/checkbox-button:w-5.5 group-has-focus-visible/checkbox-button:group-not-has-checked/checkbox-button:opacity-30 bg-white size-4.5 rounded-full transition-all mx-0.75 shadow-[1px_1px_3px] group-has-checked/checkbox-button:shadow-[-1px_1px_3px] group-has-checked/checkbox-button:shadow-navy-900 shadow-navy-100"
      >
        <svg width="9" height="7" viewBox="0 0 9 7" fill="none" xmlns="http://www.w3.org/2000/svg" class="group-has-checked/checkbox-button:opacity-100 opacity-0 transition-opacity">
          <path
            d="M0.75 3.75L2.75 5.75L7.75 0.75"
            stroke="#334C68"
            stroke-width="1.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
      </span>
    </span>
    <span class="block flex-1">
      <span class="flex">
        <span class="block form-field-label text-navy-700">{{ label }}</span>
      </span>
    </span>
    <input
      type="checkbox"
      v-bind="$attrs"
      :value="value"
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

const model = defineModel<boolean | string[]>({ default: false })
const emit = defineEmits<{
  (e: 'input', v: boolean | string[]): void
  (e: 'change', v: boolean | string[]): void
}>()

const { label, value } = defineProps<{
  label: string
  value?: string
}>()

function onInput() {
  // v-model handles the update automatically for both boolean and array modes
  emit('input', model.value)
}

function onChange() {
  // v-model handles the update automatically for both boolean and array modes
  emit('change', model.value)
}
</script>
