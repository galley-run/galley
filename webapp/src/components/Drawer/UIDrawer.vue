<template>
  <Transition name="drawer-from-right">
    <div class="overlay" v-if="show">
      <div class="drawer" role="dialog" ref="drawerEl">
        <slot />
      </div>
    </div>
  </Transition>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { useClickOutside } from '@/composables/useClickOutside.ts'

const drawerEl = ref<HTMLElement | null>(null)
const { show } = defineProps<{ show: boolean }>()
const emit = defineEmits<{ (e: 'close'): void }>()

function onClickOutside(ev: Event) {
  const t = ev.target as Node | null
  if (!show) return

  if(t?.parentNode?.parentElement?.role === 'listbox') return
  if(t?.parentElement?.role === 'listbox') return

  const triggerNode = (drawerEl.value as any)?.$el ?? drawerEl.value
  if (triggerNode instanceof Node && t && triggerNode.contains(t)) return
  emit('close')
}

useClickOutside(() => [drawerEl.value], onClickOutside)
</script>
