<template>
  <Teleport to="body">
    <Transition :name="asDrawer ? 'dialog-from-right' : 'dialog-from-bottom'">
      <div
        class="overlay"
        :class="[asDrawer && 'overlay--drawer', stacked && 'overlay--stacked']"
        v-if="show"
      >
        <div
          class="dialog"
          :class="[asDrawer && 'dialog--drawer', stacked && 'dialog--stacked-'+ Number(stacked), $attrs.class]"
          role="dialog"
          ref="dialogEl"
        >
          <slot />
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { useClickOutside } from '@/composables/useClickOutside.ts'

const dialogEl = ref<HTMLElement | null>(null)
const { show, asDrawer, stacked } = defineProps<{
  show: boolean
  asDrawer?: boolean
  stacked?: boolean | number
}>()
const emit = defineEmits<{ (e: 'close'): void }>()

function onClickOutside(ev: Event) {
  const t = ev.target as Node | null
  if (!show) return

  if (t?.parentNode?.parentElement?.role === 'listbox') return
  if (t?.parentElement?.role === 'listbox') return

  const triggerNode = (dialogEl.value as HTMLElement & { $el?: HTMLElement })?.$el ?? dialogEl.value
  if (triggerNode instanceof Node && t && triggerNode.contains(t)) return
  emit('close')
}

useClickOutside(() => [dialogEl.value], onClickOutside)
</script>
