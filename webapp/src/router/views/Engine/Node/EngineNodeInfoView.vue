<template>
  <div class="flex flex-col gap-8">
    <div class="card">
      <EngineNodeTabBar />

      <h1>Server information</h1>
      <p>
        The Galley Cluster Agent receives various information from your servers. All we know is
        viewable below here.
      </p>
    </div>
    <div class="grid grid-cols-2 md:grid-cols-4 gap-8">
      <DashboardCard title="IP Address" :loading="isLoading"
        >{{ data?.attributes?.ipAddress }}
      </DashboardCard>
      <DashboardCard title="CPU" :loading="isLoading">{{ data?.attributes?.cpu }} </DashboardCard>
      <DashboardCard title="Memory" :loading="isLoading || !data?.attributes?.memory"
        >{{ format(data.attributes.memory) }}
      </DashboardCard>
      <DashboardCard title="Storage" :loading="isLoading || !data?.attributes?.storage">
        {{ format(data.attributes.storage) }}
        <div class="text-sm text-tides-800" v-if="data?.attributes?.osMetadata?.storageUsed">
          / {{ format(data?.attributes?.osMetadata?.storageUsed) }} in use
        </div>
      </DashboardCard>
      <DashboardCard title="Node Type" :loading="isLoading"
        >{{ getNodeType(data?.attributes?.nodeType) }}
      </DashboardCard>
      <DashboardCard
        title="Operating System"
        v-if="data?.attributes?.osMetadata?.os"
        :loading="isLoading"
      >
        <span class="first-letter:uppercase">{{
          getNodeType(data?.attributes?.osMetadata?.os)
        }}</span>
      </DashboardCard>
      <DashboardCard
        title="Distribution"
        v-if="data?.attributes?.osMetadata?.distro"
        :loading="isLoading"
      >
        {{ getNodeType(data?.attributes?.osMetadata?.distro) }}
        <div class="text-sm text-tides-800" v-if="data?.attributes?.osMetadata?.version">
          {{ data?.attributes?.osMetadata?.version }}
        </div>
      </DashboardCard>
      <DashboardCard
        title="Architecture"
        v-if="data?.attributes?.osMetadata?.arch"
        :loading="isLoading"
      >
        <span class="uppercase">{{ getNodeType(data?.attributes?.osMetadata?.arch) }}</span>
      </DashboardCard>
    </div>
  </div>
</template>
<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query'
import axios from 'axios'
import type { ApiResponse } from '@/types/api'
import type { EngineNodeSummary } from '@/types/api/engine'
import { useRoute } from 'vue-router'
import EngineNodeTabBar from '@/router/views/Engine/Node/EngineNodeTabBar.vue'
import DashboardCard from '@/components/Dashboard/DashboardCard.vue'
import { useBytes } from '@/utils/bytes.ts'
import getNodeType from '@/utils/getNodeType.ts'

const { format } = useBytes()

const route = useRoute()
const { nodeId, vesselId } = route.params as { nodeId: string | null, vesselId: string | null }

const { isLoading, data } = useQuery({
  enabled: !!vesselId && !!nodeId,
  queryKey: ['vessel', vesselId, 'engine', 'nodes', nodeId],
  queryFn: () =>
    axios.get<ApiResponse<EngineNodeSummary>, ApiResponse<EngineNodeSummary>>(
      `/vessels/${vesselId}/engine/nodes/${nodeId}`,
    ),
})
</script>
