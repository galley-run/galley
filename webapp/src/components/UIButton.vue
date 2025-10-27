<template>
  <component
    :is="button"
    :tabindex="0"
    :class="[
      'flex rounded-full items-center cursor-pointer transition-all no-underline',
      variant === 'primary' &&
        !ghost &&
        'bg-seafoam-500 text-white hover:bg-seafoam-600 focus:bg-seafoam-600 active:bg-seafoam-700',
      variant === 'primary' &&
        ghost &&
        $slots.default &&
        'bg-transparent text-seafoam-700  hover:bg-seafoam-50 focus:bg-seafoam-50 active:bg-seafoam-100',
      variant === 'primary' && !ghost && 'focus:outline-1 outline-offset-1 outline-seafoam-400',
      variant === 'primary' && ghost && 'focus:outline-1 outline-offset-11 outline-seafoam-100',
      variant === 'primary' &&
        ghost &&
        !$slots.default &&
        'ring-seafoam-50 text-seafoam-700 hover:bg-seafoam-50 focus:bg-seafoam-50 active:ring-seafoam-100 active:bg-seafoam-100 mx-2.5',
      variant === 'neutral' &&
        !ghost &&
        'bg-navy-500 text-white hover:bg-navy-600 focus:bg-navy-600 active:bg-navy-700',
      variant === 'neutral' &&
        ghost &&
        $slots.default &&
        'bg-transparent text-navy-700 hover:bg-navy-50 focus:bg-navy-50 active:bg-navy-100',
      variant === 'neutral' &&
        ghost &&
        !$slots.default &&
        'bg-transparent  ring-navy-50 text-navy-700 hover:bg-navy-50 focus:bg-navy-50 active:bg-navy-100 active:ring-navy-100 mx-2.5',
      variant === 'neutral' && !ghost && 'focus:outline-1 outline-offset-1 outline-navy-400',
      variant === 'neutral' && ghost && 'focus:outline-1 outline-offset-11 outline-navy-100',
      (!ghost || $slots.default) && !large && !small && 'py-2.5 gap-1.5 px-4.25',
      (!ghost || $slots.default) && large && 'text-lg py-3.25 gap-2.5 px-5',
      (!ghost || $slots.default) && small && 'text-sm py-1.75 gap-1.5 px-3.5',
      ghost && !$slots.default && !large && !small && 'hover:ring-10 active:ring-10 focus:ring-10 size-6 px-1.75',
      ghost && !$slots.default && large && 'hover:ring-10 active:ring-10 focus:ring-10 size-6 px-1.75',
      ghost && !$slots.default && small && 'hover:ring-6 active:ring-6 focus:ring-6 size-6 px-2.5',
    ]"
    :to="to"
    :href="href"
    v-bind="$attrs"
    @click="emit('click', $event)"
  >
    <component
      :is="leadingAddon"
      :size="iconSize"
      v-if="leadingAddon"
      :class="[!$slots.default ? '-mx-1.75' : '-ml-0.5']"
    />
    <slot />
    <component
      :is="trailingAddon"
      :size="iconSize"
      v-if="trailingAddon"
      :class="[!$slots.default ? '-mx-1.75' : '-ml-0.5']"
    />
  </component>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
import type { FunctionalComponent } from 'vue'
import type { IconProps } from '@solar-icons/vue/lib'

const {
  to,
  href,
  large,
  small,
  ghost,
  leadingAddon,
  trailingAddon,
  variant = 'primary',
} = defineProps<{
  to?: string
  href?: string
  large?: boolean
  small?: boolean
  ghost?: boolean
  leadingAddon?: FunctionalComponent<IconProps>
  trailingAddon?: FunctionalComponent<IconProps>
  variant?: 'primary' | 'neutral' | 'custom' | 'icon'
}>()

const emit = defineEmits<{ (e: 'click', event: MouseEvent): void }>()

let iconSize = 24
if (large) {
  iconSize = 30
}
if (small) {
  iconSize = 20
}

let button: string | typeof RouterLink = 'button'
if (to) {
  button = RouterLink
} else if (href) {
  button = 'a'
}
</script>
