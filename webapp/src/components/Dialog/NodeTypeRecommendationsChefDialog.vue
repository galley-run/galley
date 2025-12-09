<template>
  <UIDialog class="dialog--chef" :show="show" @close="$emit('close')">
    <div class="dialog__header">
      <InfoCircle />
      <h3>Node recommendations</h3>
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
            <th>Controller</th>
            <td>2</td>
            <td>2 - 4 GB</td>
          </tr>
          <tr>
            <td colspan="3">
              A controller node runs the cluster control plane (API, scheduler, datastore)
            </td>
          </tr>
          <tr>
            <th>Worker</th>
            <td>4</td>
            <td>8 - 16 GB</td>
          </tr>
          <tr>
            <td colspan="3">
              The worker actually runs your workloads (applications & databases). Add more CPU,
              Memory and Workers when you need more applications to run.
            </td>
          </tr>
          <tr>
            <th>Controller & Worker</th>
            <td>4+</td>
            <td>8 - 16 GB</td>
          </tr>
          <tr>
            <td colspan="3">
              By combining the controller and worker nodes, you can run in single-node cluster mode.
              It’s a relatively cheap options to run your applications when downtime every now and
              then is acceptable.
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
import { InfoCircle, CloseCircle } from '@solar-icons/vue'
import UIButton from '@/components/UIButton.vue'
import UIDialog from '@/components/Dialog/UIDialog.vue'

const { show } = defineProps<{ show: boolean }>()
defineEmits<{ (e: 'close'): void }>()
</script>
