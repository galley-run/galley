<script setup lang="ts">
import { DoubleAltArrowDown } from '@solar-icons/vue'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

type Item = { label: string; value: string; disabled?: boolean }

const props = defineProps<{
  variant?: 'inline' | 'default' | 'prefix/icon' | 'suffix/icon' | 'both'
  modelValue: string | null
  items: Item[]
  placeholder?: string
  disabled?: boolean
  maxHeightPx?: number
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | null): void
  (e: 'change', value: string | null): void
}>()

const isOpen = ref(false)
const triggerEl = ref<HTMLButtonElement | null>(null)
const listEl = ref<HTMLDivElement | null>(null)
const activeIndex = ref<number>(-1)

const selectedItem = computed(() => props.items.find((i) => i.value === props.modelValue) ?? null)

function open() {
  if (props.disabled) return
  isOpen.value = true
  // Set activeIndex to the selected or first enabled item

  activeIndex.value = Math.max(
    props.items.findIndex((i) => i.value === props.modelValue && !i.disabled),
    props.items.findIndex((i) => !i.disabled),
  )
  requestAnimationFrame(() => listEl.value?.focus())
}

function close() {
  isOpen.value = false
  activeIndex.value = -1
  triggerEl.value?.focus()
}

function toggle() {
  if (isOpen.value) {
    close()
  } else {
    open()
  }
}

function selectAt(index: number) {
  const item = props.items[index]
  if (!item || item.disabled) return
  emit('update:modelValue', item.value)
  emit('change', item.value)
  close()
}

function onKeydownList(e: KeyboardEvent) {
  if (!isOpen.value) return
  const enabledIndexes = props.items.map((i, idx) => (i.disabled ? -1 : idx)).filter((i) => i >= 0)
  const currentPos = enabledIndexes.indexOf(activeIndex.value)
  switch (e.key) {
    case 'ArrowDown':
      e.preventDefault()
      activeIndex.value = enabledIndexes[(currentPos + 1) % enabledIndexes.length]
      ensureActiveVisible()
      break
    case 'ArrowUp':
      e.preventDefault()
      activeIndex.value =
        enabledIndexes[(currentPos - 1 + enabledIndexes.length) % enabledIndexes.length]
      ensureActiveVisible()
      break
    case 'Home':
      e.preventDefault()
      activeIndex.value = enabledIndexes[0]
      ensureActiveVisible()
      break
    case 'End':
      e.preventDefault()
      activeIndex.value = enabledIndexes[enabledIndexes.length - 1]
      ensureActiveVisible()
      break
    case 'Enter':
    case ' ':
      e.preventDefault()
      selectAt(activeIndex.value)
      break
    case 'Escape':
      e.preventDefault()
      close()
      break
  }
}

function ensureActiveVisible() {
  const list = listEl.value
  if (!list) return
  const opt = list.querySelector<HTMLElement>(`[data-index="${activeIndex.value}"]`)
  opt?.scrollIntoView({ block: 'nearest' })
}

function onClickOutside(ev: MouseEvent) {
  const t = ev.target as Node
  if (!isOpen.value) return
  if (triggerEl.value?.contains(t)) return
  if (listEl.value?.contains(t)) return
  close()
}

onMounted(() => document.addEventListener('mousedown', onClickOutside))
onBeforeUnmount(() => document.removeEventListener('mousedown', onClickOutside))

// Keep the active index in sync if items change
watch(
  () => props.items,
  () => {
    if (!isOpen.value) return
    activeIndex.value = Math.max(0, Math.min(activeIndex.value, props.items.length - 1))
  },
)
</script>

<template>
  <div class="relative inline-block contain-layout">
    <button
      ref="triggerEl"
      type="button"
      :aria-expanded="isOpen"
      :aria-haspopup="'listbox'"
      :disabled="disabled"
      class="flex items-center py-1.5 px-3 gap-1.5 text-navy-700 text-base cursor-pointer focus:outline-none focus:bg-navy-50 focus:text-navy-900 hover:bg-navy-50 hover:text-navy-900 rounded-lg"
      @click="toggle"
      v-if="variant === 'inline'"
    >
      <span class="font-semibold tracking-tight truncate max-w-52">
        {{ selectedItem?.label ?? placeholder ?? 'Select...' }}
      </span>
      <DoubleAltArrowDown size="20" :class="isOpen ? 'rotate-180' : ''" />
    </button>
    <button v-else>DROPDOWN NOT IMPLENTED YET</button>

    <div
      v-if="isOpen"
      ref="listEl"
      role="listbox"
      tabindex="0"
      :aria-activedescendant="activeIndex >= 0 ? `dd-opt-${activeIndex}` : undefined"
      class="absolute left-0 mt-1 focus:outline-none w-max min-w-max top-full bg-white border border-tides-600 rounded-lg cursor-pointer divide-y divide-tides-200 shadow-md shadow-navy-200/25"
      :style="{ maxHeight: (maxHeightPx ?? 260) + 'px', overflowY: 'auto' }"
      @keydown="onKeydownList"
    >
      <template v-for="(item, idx) in items" :key="item.value">
        <div
          :id="`dd-opt-${idx}`"
          role="option"
          :aria-selected="item.value === modelValue"
          :data-index="idx"
          :class="[
            'px-4 py-2.5 max-w-80 hover:bg-navy-50 aria-selected:bg-navy-600 aria-selected:text-white',
            item.disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer',
            idx === activeIndex ? 'bg-navy-50' : '',
          ]"
          @mouseenter="!item.disabled && (activeIndex = idx)"
          @mousedown.prevent="selectAt(idx)"
        >
          {{ item.label }}
        </div>
      </template>
    </div>
  </div>
</template>
