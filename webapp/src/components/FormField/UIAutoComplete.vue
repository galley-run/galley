<script setup lang="ts">
import { computed, type FunctionalComponent, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useClickOutside } from '@/composables/useClickOutside.ts'
import type { IconProps } from '@solar-icons/vue/lib'

type Item = {
  label: string
  value?: string
  disabled?: boolean
}

const {
  items,
  modelValue = '',
  placeholder,
  disabled,
  maxHeightPx,
  icon,
  autoOpen = false,
  menuPosition = 'left',
  id,
} = defineProps<{
  modelValue?: string
  items: Item[]
  placeholder?: string
  id?: string
  disabled?: boolean
  autoOpen?: boolean
  maxHeightPx?: number
  icon?: FunctionalComponent<IconProps>
  menuPosition?: 'left' | 'right'
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'change', value: string): void
}>()

const isOpen = ref(false)
const inputEl = ref<HTMLInputElement | null>(null)
const listEl = ref<HTMLDivElement | null>(null)
const activeIndex = ref<number>(-1)
const inputValue = ref(modelValue)

const inputRect = computed<DOMRect>(() => {
  if (!isOpen.value) return new DOMRect(0, 0, 0, 0)
  const el = inputEl.value?.parentNode || inputEl.value
  return el && typeof el.getBoundingClientRect === 'function'
    ? el.getBoundingClientRect()
    : new DOMRect(0, 0, 0, 0)
})

const listRect = computed<DOMRect>(() => {
  if (!isOpen.value) return new DOMRect(0, 0, 0, 0)
  const el = listEl.value
  return el && typeof el.getBoundingClientRect === 'function'
    ? el.getBoundingClientRect()
    : new DOMRect(0, 0, 0, 0)
})

const menuStyle = computed(() => {
  return {
    position: 'absolute',
    top: `${inputRect.value.bottom + window.scrollY}px`,
    width: `${inputRect.value.width}px`,
    left:
      menuPosition === 'left'
        ? `${inputRect.value.left}px`
        : `${inputRect.value.left - (listRect.value.width - inputRect.value.width) + window.scrollX}px`,
    maxHeight: `${maxHeightPx ?? 260}px`,
  }
})

const filteredItems = computed(() => {
  if (!inputValue.value && autoOpen) return items
  if (!inputValue.value) return []
  const search = inputValue.value.toLowerCase()

  // Filter items that contain the search term
  const matching = items.filter((item) => item.label.toLowerCase().includes(search))

  // Sort: items that start with search term first, then the rest
  return matching.sort((a, b) => {
    const aStarts = a.label.toLowerCase().startsWith(search)
    const bStarts = b.label.toLowerCase().startsWith(search)

    if (aStarts && !bStarts) return -1
    if (!aStarts && bStarts) return 1
    return 0
  })
})

const bestMatch = computed(() => {
  if (!inputValue.value || filteredItems.value.length === 0) return -1
  const search = inputValue.value.toLowerCase()

  // Find first item that starts with the search term
  const startsWithIndex = filteredItems.value.findIndex(
    (item) => !item.disabled && item.label.toLowerCase().startsWith(search),
  )

  if (startsWithIndex >= 0) return startsWithIndex

  // If no match starts with search, return -1 (no selection)
  return -1
})

function open() {
  if (disabled) return
  isOpen.value = true
  activeIndex.value = bestMatch.value
}

function close() {
  isOpen.value = false
  activeIndex.value = -1
}

function selectAt(index: number) {
  const item = filteredItems.value[index]
  if (!item || item.disabled) return

  const value = item.value ?? item.label
  inputValue.value = item.label
  emit('update:modelValue', value)
  emit('change', value)
  close()
}

function onInput(e: Event) {
  const value = (e.target as HTMLInputElement).value
  inputValue.value = value
  emit('update:modelValue', value)

  // Open dropdown if there are items to show
  if ((value || autoOpen) && filteredItems.value.length > 0) {
    if (!isOpen.value) {
      open()
    } else {
      activeIndex.value = bestMatch.value
    }
  } else {
    close()
  }
}

function onFocus() {
  // Open dropdown on focus to show all items
  if (autoOpen && filteredItems.value.length > 0) {
    open()
  }
}

function onKeydown(e: KeyboardEvent) {
  if (!isOpen.value) {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      open()
    }
    return
  }

  const enabledIndexes = filteredItems.value
    .map((i, idx) => (i.disabled ? -1 : idx))
    .filter((i) => i >= 0)

  const currentPos = enabledIndexes.indexOf(activeIndex.value)

  switch (e.key) {
    case 'ArrowDown':
      e.preventDefault()
      activeIndex.value = enabledIndexes[(currentPos + 1) % enabledIndexes.length] ?? -1
      ensureActiveVisible()
      break
    case 'ArrowUp':
      e.preventDefault()
      activeIndex.value =
        enabledIndexes[(currentPos - 1 + enabledIndexes.length) % enabledIndexes.length] ?? -1
      ensureActiveVisible()
      break
    case 'Home':
      e.preventDefault()
      activeIndex.value = enabledIndexes[0] ?? -1
      ensureActiveVisible()
      break
    case 'End':
      e.preventDefault()
      activeIndex.value = enabledIndexes[enabledIndexes.length - 1] ?? -1
      ensureActiveVisible()
      break
    case 'Enter':
      e.preventDefault()
      if (activeIndex.value >= 0) {
        selectAt(activeIndex.value)
      } else {
        // Accept the current input value
        emit('update:modelValue', inputValue.value)
        emit('change', inputValue.value)
        close()
      }
      break
    case 'Escape':
      e.preventDefault()
      close()
      break
    case 'Tab':
      if (activeIndex.value >= 0) {
        e.preventDefault()
        selectAt(activeIndex.value)
      } else {
        close()
      }
      break
  }
}

function ensureActiveVisible() {
  const list = listEl.value
  if (!list) return
  const opt = list.querySelector<HTMLElement>(`[data-index="${activeIndex.value}"]`)
  opt?.scrollIntoView({ block: 'nearest' })
}

function onClickOutside(ev: Event) {
  const t = ev.target as Node | null
  if (!isOpen.value) return
  if (inputEl.value instanceof Node && t && inputEl.value.contains(t)) return
  if (listEl.value instanceof Node && t && listEl.value.contains(t)) return
  close()
}

function handleResize() {
  if (isOpen.value) close()
}

onMounted(() => {
  window.addEventListener('resize', handleResize, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})

useClickOutside(() => [inputEl.value, listEl.value], onClickOutside)

// Sync inputValue with modelValue when it changes externally
watch(
  () => modelValue,
  (newValue) => {
    if (newValue !== inputValue.value) {
      inputValue.value = newValue
      // Try to find matching item to display label
      const matchingItem = items.find((i) => i.value === newValue || i.label === newValue)
      if (matchingItem) {
        inputValue.value = matchingItem.label
      }
    }
  },
)

// Update active index to best match when filtered items change
watch(
  () => filteredItems.value,
  () => {
    if (!isOpen.value) return
    activeIndex.value = bestMatch.value
  },
)
</script>

<template>
  <div class="flex contain-layout group items-center">
    <div class="text-input flex items-center w-full relative">
      <component
        :is="icon"
        :size="20"
        class="absolute left-3 pointer-events-none text-navy-600"
        v-if="icon"
      />
      <input
        ref="inputEl"
        v-bind="$attrs"
        :id="id"
        type="text"
        :value="inputValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :aria-expanded="isOpen"
        :aria-haspopup="'listbox'"
        :aria-autocomplete="'list'"
        role="combobox"
        class="w-full"
        :class="[icon && 'pl-10']"
        @input="onInput"
        @focus="onFocus"
        @keydown="onKeydown"
      />
    </div>

    <Teleport to="body">
      <div
        v-if="isOpen && filteredItems.length > 0"
        ref="listEl"
        role="listbox"
        tabindex="-1"
        :aria-activedescendant="activeIndex >= 0 ? `ac-opt-${activeIndex}` : undefined"
        class="absolute z-50 mt-1 focus:outline-none bg-white border border-tides-600 rounded-lg cursor-pointer divide-y divide-tides-200 shadow-md shadow-navy-200/25 overflow-scroll"
        :style="menuStyle"
      >
        <template v-for="(item, idx) in filteredItems" :key="item.value || idx">
          <div
            :id="`ac-opt-${idx}`"
            role="option"
            :aria-selected="item.value === modelValue || item.label === modelValue"
            :data-index="idx"
            :class="[
              'px-4 py-2.5 grid grid-cols-[auto_1fr_0fr] gap-2.5 items-center',
              item.disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer',
              idx === activeIndex && 'bg-navy-50',
              'hover:bg-navy-50 aria-selected:bg-navy-600 aria-selected:text-white',
            ]"
            @mouseenter="!item.disabled && (activeIndex = idx)"
            @mousedown.prevent="selectAt(idx)"
          >
            <slot name="leadingAddon" :item="item" />
            <div :class="!$slots.leadingAddon && 'col-span-2'">{{ item.label }}</div>
            <slot name="trailingAddon" :item="item" />
          </div>
        </template>
      </div>
    </Teleport>
  </div>
</template>
