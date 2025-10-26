<script setup lang="ts">
import { DoubleAltArrowDown } from '@solar-icons/vue'
import { computed, type FunctionalComponent, onBeforeUnmount, onMounted, ref, watch, type ComponentPublicInstance } from 'vue'
import UIButton from '@/components/UIButton.vue'
import router from '@/router'
import { useClickOutside } from '@/composables/useClickOutside.ts'
import type { IconProps } from '@solar-icons/vue/lib'

type Item = {
  label: string
  value: string
  disabled?: boolean
  link?: 'external' | boolean
  variant?: 'danger' | undefined
}

const {
  items,
  modelValue = null,
  variant,
  placeholder,
  selectFirst = false,
  disabled,
  maxHeightPx,
  icon,
  menuPosition = 'left',
} = defineProps<{
  variant?: 'inline' | 'default' | 'leadingAddon' | 'trailingAddon' | 'both' | 'icon'
  modelValue?: string | null
  items: Item[]
  placeholder?: string
  disabled?: boolean
  selectFirst?: boolean
  maxHeightPx?: number
  icon?: FunctionalComponent<IconProps>
  menuPosition?: 'left' | 'right'
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | null): void
  (e: 'change', value: string | null): void
}>()

const isOpen = ref(false)
const triggerEl = ref<HTMLElement | ComponentPublicInstance | null>(null)
const listEl = ref<HTMLDivElement | null>(null)
const activeIndex = ref<number>(-1)

const triggerNode = computed<HTMLElement | null>(() => {
  const raw = triggerEl.value as any
  return (raw?.$el ?? raw) ?? null
})

const triggerRect = computed<DOMRect>(() => {
  if(!isOpen.value) return
  const el = triggerNode.value as any
  return el && typeof el.getBoundingClientRect === 'function'
    ? el.getBoundingClientRect()
    : new DOMRect(0, 0, 0, 0)
})

const listRect = computed<DOMRect>(() => {
  if(!isOpen.value) return
  const el = listEl.value as any
  return el && typeof el.getBoundingClientRect === 'function'
    ? el.getBoundingClientRect()
    : new DOMRect(0, 0, 0, 0)
})

const menuStyle = computed(() => {
  return ({
    position: 'absolute',
    top: `${triggerRect.value.bottom + window.scrollY}px`,
    left: menuPosition === 'left' ? `${triggerRect.value.left}px` : `${triggerRect.value.left - (listRect.value.width - triggerRect.value.width) + window.scrollX}px`,
    maxHeight: `${maxHeightPx ?? 260}px`,
  })
})

const selectedItem = computed(() => items.find((i) => i.value === modelValue) ?? null)

function open() {
  if (disabled) return
  isOpen.value = true

  // Set activeIndex to the selected
  activeIndex.value = items.findIndex((i) => i.value === modelValue && !i.disabled)
  if (activeIndex.value === -1 && selectFirst) {
    // Or select first enabled item
    activeIndex.value = items.findIndex((i) => !i.disabled)
  }
  requestAnimationFrame(() => listEl.value?.focus())
}

function close() {
  isOpen.value = false
  activeIndex.value = -1
  triggerEl?.value?.focus?.()
}

function toggle() {
  if (isOpen.value) {
    close()
  } else {
    open()
  }
}

function selectAt(index: number) {
  const item = items[index]

  if (!item || item.disabled) return

  if (item.link) {
    if (item.value.startsWith('http')) {
      window.open(item.value, item.link === 'external' ? '_blank' : '_self')
    } else {
      router.push(item.value)
    }
    close()
    return
  }

  emit('update:modelValue', item.value)
  emit('change', item.value)
  close()
}

function onKeydownList(e: KeyboardEvent) {
  if (!isOpen.value) return
  const enabledIndexes = items.map((i, idx) => (i.disabled ? -1 : idx)).filter((i) => i >= 0)
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

function onClickOutside(ev: Event) {
  const t = ev.target as Node | null
  if (!isOpen.value) return
  const triggerNode = (triggerEl.value as any)?.$el ?? triggerEl.value
  if (triggerNode instanceof Node && t && triggerNode.contains(t)) return
  const listNode = (listEl.value as any)?.$el ?? listEl.value
  if (listNode instanceof Node && t && listNode.contains(t)) return
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
useClickOutside(() => [triggerEl.value, listEl.value], onClickOutside)

// Keep the active index in sync if items change
watch(
  () => items,
  () => {
    if (!isOpen.value) return
    activeIndex.value = Math.max(0, Math.min(activeIndex.value, items.length - 1))
  },
)
</script>

<template>
  <div class="block contain-layout">
    <ui-button
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
    </ui-button>
    <UIButton
      v-else-if="variant === 'icon'"
      :leading-addon="icon"
      ref="triggerEl"
      @click="toggle"
      :aria-expanded="isOpen"
      :aria-haspopup="'listbox'"
      :disabled="disabled"
      ghost
    />
    <ui-button v-else>DROPDOWN NOT IMPLENTED YET</ui-button>

    <Teleport to="body">
      <div
        v-if="isOpen"
        ref="listEl"
        role="listbox"
        tabindex="0"
        :aria-activedescendant="activeIndex >= 0 ? `dd-opt-${activeIndex}` : undefined"
        class="absolute z-50 mt-1 focus:outline-none w-max min-w-max bg-white border border-tides-600 rounded-lg cursor-pointer divide-y divide-tides-200 shadow-md shadow-navy-200/25"
        :style="menuStyle"
        @keydown="onKeydownList"
      >
        <template v-for="(item, idx) in items" :key="item.value">
          <div
            :id="`dd-opt-${idx}`"
            role="option"
            :aria-selected="item.value === modelValue"
            :data-index="idx"
            :class="[
              'px-4 py-2.5 max-w-80 ',
              item.disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer',
              !item.variant && idx === activeIndex && 'bg-navy-50',
              item.variant === 'danger' && idx === activeIndex && 'bg-coral-100',
              item.variant === 'danger' &&
                'bg-coral-50 text-coral-500 hover:bg-coral-100 aria-selected:bg-coral-100 aria-selected:text-coral-600',
              !item.variant &&
                'hover:bg-navy-50 aria-selected:bg-navy-600 aria-selected:text-white',
            ]"
            @mouseenter="!item.disabled && (activeIndex = idx)"
            @mousedown.prevent="selectAt(idx)"
          >
            {{ item.label }}
          </div>
        </template>
      </div>
    </Teleport>
  </div>
</template>
