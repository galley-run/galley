<template>
  <UIDialog class="dialog--chef" :show="show" @close="$emit('close')">
    <div class="dialog__header">
      <InfoCircle />
      <h3>Resource recommendations</h3>
      <UIButton @click="$emit('close')" ghost variant="neutral" :trailing-addon="CloseCircle" />
    </div>
    <div class="dialog__body">
      <table class="table table--chef-recommendations">
        <thead>
          <tr>
            <td>Workload</td>
            <td>vCPU</td>
            <td>Memory</td>
          </tr>
        </thead>
        <tbody>
          <tr>
            <th>Web / API</th>
            <td>0.5 - 1</td>
            <td>512 MiB - 1 GiB</td>
          </tr>
          <tr>
            <td colspan="3">Good for a small Node, Python or JVM app.</td>
          </tr>
          <tr>
            <th>Worker / Queue</th>
            <td>1 - 2</td>
            <td>1 - 2 GiB</td>
          </tr>
          <tr>
            <td colspan="3">
              Extra headroom for batch jobs and bursts. Optionally enable Burst mode to allow your
              resource to temporary use some extra space when its needed.
            </td>
          </tr>
          <tr>
            <th>Postgres</th>
            <td>1+</td>
            <td>2 - 4 GiB</td>
          </tr>
          <tr>
            <td colspan="3">RAM is key on Postgres, shared buffers use ~25% of it.</td>
          </tr>
          <tr>
            <th>MongoDB</th>
            <td>2+</td>
            <td>2 - 8 GiB</td>
          </tr>
          <tr>
            <td colspan="3">WiredTiger (MongoDB’s engine) cache benefits from some more memory.</td>
          </tr>
          <tr>
            <th>Redis / SQLite</th>
            <td>0.5 - 1</td>
            <td>512 MiB - 2 GiB</td>
          </tr>
          <tr>
            <td colspan="3">Try to match your memory to your dataset size for Redis.</td>
          </tr>
          <tr>
            <th>Web / API  (Staging env)</th>
            <td>~50% of prod</td>
            <td>~50% of prod</td>
          </tr>
          <tr>
            <td colspan="3">
              Staging isn’t as intensive used as your production environment and an occasional
              outage can be okay for a cost benefit.
            </td>
          </tr>
        </tbody>
      </table>

      <div class="alert alert--info">
        <div class="alert__body">
          <h6>Keep in mind</h6>
          <p>
            These are general recommendations and in no way hard requirements of your needs. Please
            read the documentation of the tools you're using to have a better estimation of the CPU
            / Memory you need.
          </p>
        </div>
      </div>
      <!--      <div class="mt-6">-->
      <!--        <UIButton :leading-addon="ChatDots" target="_blank" href="https://chatgpt.com?q=">Ask the chef</UIButton>-->
      <!--      </div>-->
    </div>
  </UIDialog>
</template>
<script setup lang="ts">
import { CloseCircle, InfoCircle } from '@solar-icons/vue'
import UIButton from '@/components/UIButton.vue'
import UIDialog from '@/components/Dialog/UIDialog.vue'

const { show } = defineProps<{ show: boolean }>()
defineEmits<{ (e: 'close'): void }>()
</script>
