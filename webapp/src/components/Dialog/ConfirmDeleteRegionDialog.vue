<template>
  <UIDialog :show="show" @close="onClose">
    <div class="dialog__header">
      <ShieldCross class="text-red-500" />
      <h3 class="text-red-700">Delete this region</h3>
      <UIButton @click="onClose" ghost variant="neutral" :trailing-addon="CloseCircle" />
    </div>
    <div class="dialog__body">
      <div v-if="error" class="alert alert--error">
        {{ error }}
      </div>
      <p>Are you sure you want to delete this region? This action cannot be undone.</p>
    </div>
    <div class="dialog__footer">
      <UIButton ghost variant="neutral" :leading-addon="UndoLeftRound" @click="onClose"
        >Cancel & Close
      </UIButton>
      <UIButton @click="onDelete" :trailing-addon="MapPointRemove" variant="destructive"
        >Delete region
      </UIButton>
    </div>
  </UIDialog>
</template>

<script setup lang="ts">
import UIDialog from '@/components/Dialog/UIDialog.vue'
import { CloseCircle, MapPointRemove, ShieldCross, UndoLeftRound } from '@solar-icons/vue'
import UIButton from '@/components/UIButton.vue'
import { useRegionForm } from '@/composables/useRegionForm.ts'
import { ref } from 'vue'
import type { ApiError } from '@/utils/registerAxios.ts'

const { show, regionId } = defineProps<{ show: boolean; regionId: string | null }>()
const emit = defineEmits<{ (e: 'close'): void; (e: 'confirm'): void }>()

const error = ref<string | null>(null)

const { deleteRegion } = useRegionForm()

async function onClose() {
  error.value = null
  emit('close')
}

async function onDelete() {
  error.value = null

  if (!regionId) return

  try {
    await deleteRegion(regionId)
    emit('confirm')
  } catch (e) {
    const apiError = e as ApiError
    error.value = apiError?.message || 'Something went wrong. Please try again later.'
  }
}
</script>
