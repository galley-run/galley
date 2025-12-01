<template>
  <Teleport to="body">
    <Transition name="dialog-from-bottom">
      <div class="overlay" v-if="show">
        <div class="dialog" role="dialog" ref="dialogEl">
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
const { show } = defineProps<{ show: boolean }>()
const emit = defineEmits<{ (e: 'close'): void }>()

function onClickOutside(ev: Event) {
  const t = ev.target as Node | null
  if (!show) return

  if (t?.parentNode?.parentElement?.role === 'listbox') return
  if (t?.parentElement?.role === 'listbox') return

  const triggerNode = (dialogEl.value as any)?.$el ?? dialogEl.value
  if (triggerNode instanceof Node && t && triggerNode.contains(t)) return
  emit('close')
}

useClickOutside(() => [dialogEl.value], onClickOutside)
</script>
