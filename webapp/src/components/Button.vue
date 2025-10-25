<template>
  <component
    :is="button"
    :class="[
      'flex rounded-full items-center cursor-pointer transition-all',
      variant === 'primary' && !ghost && 'bg-seafoam-500 text-white hover:bg-seafoam-600',
      variant === 'primary' && ghost && $slots.default && 'bg-transparent text-seafoam-700  hover:bg-seafoam-50',
      variant === 'primary' && ghost && !$slots.default && 'ring-seafoam-50 text-seafoam-700 hover:bg-seafoam-50 mx-2.5',
      variant === 'neutral' && !ghost && 'bg-navy-500 text-white hover:bg-navy-600',
      variant === 'neutral' && ghost && $slots.default && 'bg-transparent text-navy-700 hover:bg-navy-50',
      variant === 'neutral' && ghost && !$slots.default && 'bg-transparent  ring-navy-50 text-navy-700 hover:bg-navy-50 mx-2.5',
      (!ghost || $slots.default) && !large && !small && 'py-2.5 gap-1.5 px-4.25',
      (!ghost || $slots.default) && large && 'text-lg py-3.25 gap-2.5 px-5',
      (!ghost || $slots.default) && small && 'text-sm py-1.75 gap-1.5 px-3.5',
      ghost && !$slots.default && !large && !small && 'hover:ring-10 size-6 px-1.75',
      ghost && !$slots.default && large && 'hover:ring-10 size-6 px-1.75',
      ghost && !$slots.default && small && 'hover:ring-6 size-6 px-2.5',
    ]"
    :to="to"
    :href="href"
    @click="onClick"
  >
    <component
      :is="leadingAddon"
      :size="iconSize"
      :if="leadingAddon"
      :class="[!$slots.default ? '-mx-1.75' : '-ml-0.5']"
    />
    <slot />
    <component
      :is="trailingAddon"
      :size="iconSize"
      :if="trailingAddon"
      :class="[!$slots.default ? '-mx-1.75' : '-ml-0.5']"
    />
  </component>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
import type { PropType } from 'vue'
import type { IconProps } from '@solar-icons/vue/lib'

const {
  to,
  href,
  onClick,
  large,
  small,
  ghost,
  leadingAddon,
  trailingAddon,
  variant = 'primary',
} = defineProps<{
  to?: string
  href?: string
  onClick?: () => void
  large?: boolean
  small?: boolean
  ghost?: boolean
  leadingAddon?: PropType<IconProps>
  trailingAddon?: PropType<IconProps>
  variant?: 'primary' | 'neutral' | 'custom' | 'icon'
}>()

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
