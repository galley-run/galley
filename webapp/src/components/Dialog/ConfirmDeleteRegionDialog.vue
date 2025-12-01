<template>
  <UIDialog :show="show" @close="$emit('close')">
    <div class="dialog__header">
      <ShieldCross class="text-red-500" />
      <h3 class="text-red-700">Delete this region</h3>
      <UIButton @click="$emit('close')" ghost variant="neutral" :trailing-addon="CloseCircle" />
    </div>
    <div class="dialog__body">
      <p>Are you sure you want to delete this region? This action cannot be undone.</p>
    </div>
    <div class="dialog__footer">
      <UIButton ghost variant="neutral" :leading-addon="UndoLeftRound" @click="$emit('close')"
        >Cancel & Close
      </UIButton>
      <UIButton @click="$emit('confirm')" :trailing-addon="MapPointRemove" variant="destructive"
        >Delete region
      </UIButton>
    </div>
  </UIDialog>
</template>

<script setup lang="ts">
import UIDialog from '@/components/Dialog/UIDialog.vue'
import { CloseCircle, MapPointRemove, ShieldCross, UndoLeftRound } from '@solar-icons/vue'
import UIButton from '@/components/UIButton.vue'

const { show } = defineProps<{ show: boolean }>()
defineEmits<{ (e: 'close'): void; (e: 'confirm'): void }>()
</script>
