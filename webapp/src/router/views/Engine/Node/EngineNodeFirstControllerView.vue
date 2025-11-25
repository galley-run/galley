<template>
  <LoadingIndicator v-if="isLoading" />
</template>

<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query'
import axios from 'axios'
import type { ApiResponse } from '@/types/api'
import type { EngineNodeSummary } from '@/types/api/engine'
import { useProjectsStore } from '@/stores/projects.ts'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { watch } from 'vue'
import LoadingIndicator from '@/assets/LoadingIndicator.vue'

const projectsStore = useProjectsStore()
const { selectedVesselId } = storeToRefs(projectsStore)

const router = useRouter()

const { isLoading, data } = useQuery({
  enabled: !!selectedVesselId?.value,
  queryKey: ['vessel', selectedVesselId?.value, 'engine', 'nodes'],
  queryFn: () =>
    axios.get<ApiResponse<EngineNodeSummary>[], ApiResponse<EngineNodeSummary>[]>(`/vessels/${selectedVesselId?.value}/engine/nodes`),
})

watch(data, (nodes) => {
  if (nodes) {
    const firstNode = nodes.find((node) => node.attributes.nodeType === 'controller')
    if (firstNode) {
      router.push(`/vessel/engine/node/${firstNode.id}`)
    } else {
      router.push(`/vessel/engine`)
    }
  } else {
    router.push(`/vessel/engine/node/add`)
  }
})
</script>
