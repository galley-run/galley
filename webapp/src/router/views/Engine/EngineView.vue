<template>
  <div class="flex flex-col gap-8">
    <div class="card">
      <h1>Engine</h1>
      <p>Here you can configure the engine of your platform for your tenants.</p>
      <div class="grid grid-cols-3 gap-8 items-start">
        <div
          class="border flex flex-col gap-2.5 rounded-2xl p-4 border-navy-200 bg-navy-50"
          :class="[mode !== 'managed_cloud' && 'opacity-30', isEngineLoading && 'animate-pulse']"
        >
          <div class="flex justify-between">
            <h4 class="text-navy-700">Galley Managed Cloud</h4>
            <CheckCircle v-if="mode === 'managed_cloud'" />
          </div>
          <p>
            Run Galley in fully managed mode. Add nodes from the Galley UI and let Galley provision
            everything automatically on supported cloud providers.
          </p>
          <p class="italic">(Currently unavailable)</p>
        </div>
        <div
          class="border flex flex-col gap-2.5 rounded-2xl p-4 border-navy-200 bg-navy-50"
          :class="[mode !== 'managed_engine' && 'opacity-30', isEngineLoading && 'animate-pulse']"
        >
          <div class="flex justify-between">
            <h4 class="text-navy-700">Galley Managed Engine</h4>
            <CheckCircle v-if="mode === 'managed_engine'" />
          </div>
          <p>
            Bring your own servers and let Galley handle the cluster setup. With one install
            command, Galley provisions k0s, secures the connection, and keeps nodes patched.
          </p>
        </div>
        <div
          class="border flex flex-col gap-2.5 rounded-2xl p-4 border-navy-200 bg-navy-50"
          :class="[
            mode !== 'controlled_engine' && 'opacity-30',
            isEngineLoading && 'animate-pulse',
          ]"
        >
          <div class="flex justify-between">
            <h4 class="text-navy-700">Galley Controlled Engine</h4>
            <CheckCircle v-if="mode === 'controlled_engine'" />
          </div>
          <p>
            Already running k0s? Connect your cluster with the lightweight Galley Agent. Deploy apps
            and databases for your tenants without giving up server control.
          </p>
        </div>
      </div>
    </div>
    <div class="grid grid-cols-2 md:grid-cols-4 gap-8">
      <DashboardCard title="Nodes" :loading="isNodesLoading">{{
        engineNodes?.length ?? 0
      }}</DashboardCard>
      <DashboardCard title="Connection status" class="">
        {{ connectionStatus }}
      </DashboardCard>
      <DashboardCard title="Total CPU">{{ totalCpu }}</DashboardCard>
      <DashboardCard title="Total Memory">{{ totalMemory }}</DashboardCard>
    </div>
    <div class="grid grid-cols-2 gap-8 items-start">
      <div class="card">
        <div class="card__header">
          <h2>Nodes</h2>
          <div>
            <UIButton
              ghost
              :leading-addon="DocumentsMinimalistic"
              disabled
              title="Visualise nodes"
            />
            <UIButton
              ghost
              :leading-addon="AddCircle"
              title="Add node"
              to="/vessel/engine/node/add"
            />
          </div>
        </div>
        <div class="stacked-list" v-if="engineNodes">
          <div
            class="stacked-list__item grid-cols-[1fr_0fr_0fr]"
            v-for="node in engineNodes"
            :key="node.id"
          >
            <div class="relative">
              <div class="flex items-center gap-2">
                <FlagIcon v-if="nodeRegions?.[node.id]" :code="nodeRegions?.[node.id]" :size="16" />
                <div>{{ node.attributes.name }}</div>
                <div
                  class="badge badge--cliona badge--small"
                  v-if="node.attributes.provisioningStatus === 'open'"
                >
                  Setup required
                </div>
              </div>
              <p>
                {{ getNodeType(node.attributes.nodeType) }} &bullet; {{ node.attributes.cpu }} CPU
                &bullet;
                {{ node.attributes.memory ? format(node.attributes.memory) : '--' }}
                RAM
              </p>
              <RouterLink
                :to="`/vessel/engine/node/${node.id}/server-info`"
                class="absolute inset-0"
              ></RouterLink>
            </div>
            <div class="text-end">
              <p>{{ node.attributes.ipAddress }}</p>
              <p class="text-tides-700" v-if="regions">
                <!--                AMS1-->
                {{
                  node.attributes.vesselEngineRegionId &&
                  regions[node.attributes.vesselEngineRegionId].name
                }}
              </p>
            </div>
            <div>
              <UIDropDown
                :items="[
                  { label: 'Edit this node', value: `/vessel/engine/node/${node.id}`, link: true },
                  {
                    label: 'Delete this node',
                    value: '/delete',
                    link: true,
                    variant: 'destructive',
                  },
                ]"
                :icon="MenuDots"
                variant="icon"
                menu-position="right"
              />
            </div>
          </div>
        </div>
        <div v-else>No nodes</div>
      </div>
      <div class="card">
        <div class="card__header">
          <h2>Regions</h2>
          <div>
            <UIButton ghost disabled :leading-addon="AddCircle" title="Add region" />
          </div>
        </div>
        <div class="stacked-list">
          <div
            class="stacked-list__item grid-cols-[1fr_1fr_0fr]"
            v-for="region in engineRegionsSorted"
            :key="region.id"
          >
            <div>
              <div class="flex items-center gap-2">
                <FlagIcon :code="region.attributes.locationCountry as CountryCode" :size="16" />
                <div>{{ region.attributes.name }}</div>
                <div class="badge badge--small badge--navy">{{ region.attributes.geoRegion }}</div>
              </div>
              <p>
                {{ region.attributes.locationCity }},
                {{ countries[region.attributes.locationCountry]?.name }}
              </p>
            </div>
            <div class="text-end">
              <p class="text-tides-700">{{ region.attributes.providerName }}</p>
            </div>
            <div>
              <UIDropDown
                :items="[{ label: 'Edit region', value: '/edit', link: true }]"
                :icon="MenuDots"
                variant="icon"
                menu-position="right"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { AddCircle, CheckCircle, DocumentsMinimalistic, MenuDots } from '@solar-icons/vue'
import UIDropDown from '@/components/FormField/UIDropDown.vue'
import UIButton from '@/components/UIButton.vue'
import FlagIcon from 'vue3-flag-icons'
import DashboardCard from '@/components/Dashboard/DashboardCard.vue'
import { useQuery } from '@tanstack/vue-query'
import { useProjectsStore } from '@/stores/projects.ts'
import { storeToRefs } from 'pinia'
import axios from 'axios'
import { computed } from 'vue'
import { useBytes } from '@/utils/bytes.ts'
import type { ApiResponse } from '@/types/api'
import type { EngineNodeSummary, EngineRegionSummary, EngineSummary } from '@/types/api/engine'
import dayjs from 'dayjs'
import getNodeType from '@/utils/getNodeType.ts'
import { CountryCode } from 'vue3-flag-icons/types'
import countries from '@/utils/countries.ts'

const { formatBytes, sumByteSizes, format } = useBytes()
const projectsStore = useProjectsStore()
const { selectedVesselId } = storeToRefs(projectsStore)

const { isLoading: isEngineLoading, data: engine } = useQuery({
  enabled: computed(() => !!selectedVesselId?.value),
  queryKey: computed(() => ['vessel', selectedVesselId?.value, 'engine']),
  queryFn: () =>
    axios.get<ApiResponse<EngineSummary>[], ApiResponse<EngineSummary>[]>(
      `/vessels/${selectedVesselId?.value}/engine`,
    ),
})

const { isLoading: isNodesLoading, data: engineNodes } = useQuery({
  enabled: computed(() => !!selectedVesselId?.value),
  queryKey: computed(() => ['vessel', selectedVesselId?.value, 'engine', 'nodes']),
  queryFn: () =>
    axios.get<ApiResponse<EngineNodeSummary>[], ApiResponse<EngineNodeSummary>[]>(
      `/vessels/${selectedVesselId?.value}/engine/nodes`,
    ),
})

const { isLoading: isRegionsLoading, data: engineRegions } = useQuery({
  enabled: computed(() => !!selectedVesselId?.value),
  queryKey: computed(() => ['vessel', selectedVesselId?.value, 'engine', 'regions']),
  queryFn: () =>
    axios.get<ApiResponse<EngineRegionSummary>[], ApiResponse<EngineRegionSummary>[]>(
      `/vessels/${selectedVesselId?.value}/engine/regions`,
    ),
})

const nodeRegions = computed(() =>
  (engineNodes.value ?? []).reduce<Record<string, string>>((acc, node) => {
    const region = engineRegions.value?.find(
      (r) => r.id === node.attributes.vesselEngineRegionId,
    )
    acc[node.id] = region?.attributes.locationCountry ?? ''
    return acc
  }, {}),
)

console.log(nodeRegions.value)

const engineRegionsSorted = computed(() => {
  if (!engineRegions.value) return []

  return [...engineRegions.value].sort((a, b) => {
    const geoA = a.attributes.geoRegion ?? ''
    const geoB = b.attributes.geoRegion ?? ''
    if (geoA !== geoB) {
      return geoA.localeCompare(geoB)
    }

    const countryA = a.attributes.locationCountry ?? ''
    const countryB = b.attributes.locationCountry ?? ''
    if (countryA !== countryB) {
      return countryA.localeCompare(countryB)
    }

    const nameA = a.attributes.name ?? ''
    const nameB = b.attributes.name ?? ''

    return nameA.localeCompare(nameB)
  })
})

const mode = computed(() => engine.value?.[0]?.attributes?.mode)
const regions = computed(() => {
  return engineRegions.value?.reduce((acc, region) => {
    acc[region.id] = region.attributes
    return acc
  }, {})
})
const totalCpu = computed(() =>
  engineNodes.value?.reduce((acc, node) => acc + Number(node.attributes.cpu ?? 0), 0),
)
const totalMemoryBytes = computed(() =>
  sumByteSizes(engineNodes.value?.map((node) => node.attributes.memory) ?? []),
)
const totalMemory = computed(() =>
  formatBytes(totalMemoryBytes.value, { iec: false, decimals: 1, unit: 'GB' }),
) // "MB/GB"

const connectionStatus = computed(() => {
  if (engine?.value?.[0]?.attributes?.agentConnectionStatus !== 'connected') return 'Disconnected'
  if (
    dayjs(engine?.value?.[0]?.attributes?.lastAgentConnectionAt).isBefore(
      dayjs().subtract(1, 'hour'),
    )
  ) {
    return 'Last connected more than an hour ago'
  }

  return 'Connected'
})
</script>
