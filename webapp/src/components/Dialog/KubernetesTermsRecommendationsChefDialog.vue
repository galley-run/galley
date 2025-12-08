<template>
  <UIDialog class="dialog--chef" :show="show" :stacked="stacked" @close="$emit('close')">
    <div class="dialog__header">
      <QuestionCircle />
      <h3 v-if="kubernetesTerm === KubernetesTerms.pods">Pods</h3>
      <h3 v-else-if="kubernetesTerm === KubernetesTerms.oomkilled">OOMKilled</h3>
      <UIButton @click="$emit('close')" ghost variant="neutral" :trailing-addon="CloseCircle" />
    </div>
    <div class="dialog__body" v-if="kubernetesTerm === KubernetesTerms.pods">
      <p>
        A Pod is the smallest deployable unit in Kubernetes. It represents one running instance of
        your application.
      </p>

      <h3>A Pod contains:</h3>
      <ul>
        <li>One or more containers (usually just one)</li>
        <li>A shared network (same IP and ports)</li>
        <li>Shared storage volumes</li>
        <li>The resource configuration (CPU, memory, etc.)</li>
      </ul>

      <h3>All containers inside a Pod:</h3>
      <ul>
        <li>Start and stop together</li>
        <li>Share the same localhost</li>
        <li>Are always scheduled on the same node</li>
      </ul>
    </div>

    <div class="dialog__body" v-else-if="kubernetesTerm === KubernetesTerms.oomkilled">
      <p class="text-lg">
        <strong>OOMKilled</strong> means <strong>Out Of Memory Killed</strong>.</p>
      <p>
        It happens when a container uses more memory than its configured memory limit, and
        Kubernetes immediately terminates the container to protect the node.
      </p>

      <div class="alert alert--warning">
        <div class="alert__body">
          <h6>Important to understand</h6>
          <p>OOMKilled is not a graceful shutdown. Your app does not get a chance to clean up.</p>
        </div>
      </div>

      <h3>When OOMKilled is triggered:</h3>
      <ul>
        <li>Your app exceeded its memory limit</li>
        <li>Kubernetes force-stopped the container</li>
        <li>The Pod is usually restarted automatically</li>
      </ul>

    </div>
  </UIDialog>
</template>
<script setup lang="ts">
import { CloseCircle, QuestionCircle } from '@solar-icons/vue'
import UIButton from '@/components/UIButton.vue'
import UIDialog from '@/components/Dialog/UIDialog.vue'
import { KubernetesTerms } from '@/utils/kubernetes.ts'

const { show, stacked, kubernetesTerm } = defineProps<{
  show: boolean
  stacked?: number
  kubernetesTerm?: KubernetesTerms
}>()
defineEmits<{ (e: 'close'): void }>()
</script>
