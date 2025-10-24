<template>
  <component
    :is="button"
    :class="[
      'flex rounded-full items-center cursor-pointer transition-all',
      variant === 'primary' && !ghost && 'bg-seafoam-500 text-white hover:bg-seafoam-600',
      variant === 'primary' && ghost && 'bg-transparent text-seafoam-700 hover:bg-seafoam-50',
      variant === 'neutral' && !ghost && 'bg-navy-500 text-white hover:bg-navy-600',
      variant === 'neutral' && ghost && 'bg-transparent text-navy-700 hover:bg-navy-50',
      !large && !small && 'py-2.5 gap-1.5 px-4.25',
      large && 'text-lg py-3.25 gap-2.5 px-5',
      small && 'text-sm py-1.75 gap-1.5 px-3.5',
    ]"
    :to="to"
    :href="href"
    @click="onClick"
  >
    <component
      :is="icon"
      :size="iconSize"
      :if="icon"
      :class="[!$slots.default ? '-mx-1.75' : '-ml-0.5']"
    />
    <slot />
  </component>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'

const { to, href, onClick, large, small, ghost, icon, variant = 'primary' } = defineProps<{
  to?: string
  href?: string
  onClick?: () => void
  large?: boolean
  small?: boolean
  ghost?: boolean
  icon?: object
  variant?: 'primary' | 'neutral' | 'custom',
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
