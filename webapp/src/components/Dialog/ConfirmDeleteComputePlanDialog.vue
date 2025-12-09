<template>
  <UIDialog :show="show" @close="onClose">
    <div class="dialog__header">
      <ShieldCross class="text-red-500" />
      <h3 class="text-red-700">Delete this compute plan</h3>
      <UIButton @click="onClose" ghost variant="neutral" :trailing-addon="CloseCircle" />
    </div>
    <div class="dialog__body">
      <div v-if="error" class="alert alert--destructive">
        <div class="alert__body">
          {{ error }}
        </div>
      </div>
      <p>Are you sure you want to delete this compute plan? This action cannot be undone.</p>
    </div>
    <div class="dialog__footer">
      <UIButton ghost variant="neutral" :leading-addon="UndoLeftRound" @click="onClose">
        Cancel & Close
      </UIButton>
      <UIButton @click="onDelete" :trailing-addon="TrashBinTrash" variant="destructive">
        Delete compute plan
      </UIButton>
    </div>
  </UIDialog>
</template>

<script setup lang="ts">
import UIDialog from '@/components/Dialog/UIDialog.vue'
import { CloseCircle, ShieldCross, TrashBinTrash, UndoLeftRound } from '@solar-icons/vue'
import UIButton from '@/components/UIButton.vue'
import { useDeleteComputePlan } from '@/composables/useComputePlan.ts'
import { ref } from 'vue'
import type { ApiError } from '@/utils/registerAxios.ts'

const { show, computePlanId } = defineProps<{ show: boolean; computePlanId: string | undefined }>()
const emit = defineEmits<{ (e: 'close'): void; (e: 'confirm'): void }>()

const error = ref<string | null>(null)

const { deleteComputePlan } = useDeleteComputePlan()

async function onClose() {
  error.value = null
  emit('close')
}

async function onDelete() {
  error.value = null

  if (!computePlanId) return

  try {
    await deleteComputePlan(computePlanId)
    emit('confirm')
  } catch (e) {
    const apiError = e as ApiError
    error.value = apiError?.message || 'Something went wrong. Please try again later.'
  }
}
</script>
