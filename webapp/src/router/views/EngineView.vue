<template>
  <div class="flex flex-col gap-8">
    <div class="card">
      <h1>Engine</h1>
      <p>Here you can configure the engine of your platform for your tenants.</p>
      <div class="grid grid-cols-3 gap-8 items-start">
        <div
          class="border flex flex-col gap-2.5 rounded-2xl p-4 border-navy-200 bg-navy-50"
          :class="[mode !== 'managed_cloud' && 'opacity-30', isEngineDataLoading && 'animate-pulse']"
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
          :class="[mode !== 'managed_engine' && 'opacity-30', isEngineDataLoading && 'animate-pulse']"
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
          :class="[mode !== 'controlled_engine' && 'opacity-30', isEngineDataLoading && 'animate-pulse']"
        >
          <div class="flex justify-between">
            <h4 class="text-navy-700">Galley Controlled Engine</h4>
            <CheckCircle v-if="mode === 'controlled_engine'" />
          </div>
          <p>
            Already running k0s? Connect your cluster with the lightweight Galley Agent. Deploy apps
            and databases for your tenants without giving up server control.
          </p>
          <p class="italic">(Currently unavailable)</p>
        </div>
      </div>
    </div>
    <div class="grid grid-cols-2 md:grid-cols-4 gap-8">
      <DashboardCard title="Nodes">2</DashboardCard>
      <DashboardCard title="Active Regions">1</DashboardCard>
      <DashboardCard title="Total CPU">4</DashboardCard>
      <DashboardCard title="Total Memory">16.6 GB</DashboardCard>
    </div>
    <div class="grid grid-cols-2 gap-8 items-start">
      <div class="card">
        <div class="card__header">
          <h2>Nodes</h2>
          <div>
            <UIButton ghost :leading-addon="DocumentsMinimalistic" title="Visualise nodes" />
            <UIButton ghost :leading-addon="AddCircle" title="Add node" />
          </div>
        </div>
        <div class="stacked-list">
          <div class="stacked-list__item grid-cols-[1fr_0fr_0fr]">
            <div>
              <div class="flex items-center gap-2">
                <FlagIcon code="nl" :size="16" class="rounded-sm" />
                <div>app1.cloud.clicqo.nl</div>
                <div class="badge badge--small">Ready</div>
              </div>
              <p>worker &bullet; 2 CPU &bullet; 8.3 GB RAM</p>
            </div>
            <div class="text-end">
              <p>5.254.39.94</p>
              <p class="text-tides-700">AMS1</p>
            </div>
            <div>
              <UIDropDown
                :items="[
                  { label: 'clicqo.nl', value: '/edit', link: true },
                  { label: 'clicqo.nl', value: 'https://clicqo.nl', link: 'external' },
                  { label: 'galley.run', value: 'https://galley.run', link: true },
                ]"
                :icon="MenuDots"
                variant="icon"
                menu-position="right"
              />
            </div>
          </div>
          <div class="stacked-list__item grid-cols-[1fr_0fr_0fr]">
            <div>
              <div class="flex items-center gap-2">
                <FlagIcon code="nl" :size="16" class="rounded-sm" />
                <div>db1.cloud.clicqo.nl</div>
                <div class="badge badge--small badge--coral">Unreachable</div>
              </div>
              <p>worker &bullet; 2 CPU &bullet; 8.3 GB RAM</p>
            </div>
            <div class="text-end">
              <p>5.254.39.95</p>
              <p class="text-tides-700">AMS1</p>
            </div>
            <div>
              <UIDropDown
                :items="[
                  { label: 'clicqo.nl', value: '/edit', link: true },
                  { label: 'clicqo.nl', value: 'https://clicqo.nl', link: 'external' },
                  { label: 'galley.run', value: 'https://galley.run', link: true },
                ]"
                :icon="MenuDots"
                variant="icon"
                menu-position="right"
              />
            </div>
          </div>
        </div>
      </div>
      <div class="card">
        <div class="card__header">
          <h2>Regions</h2>
          <div>
            <UIButton ghost :leading-addon="AddCircle" title="Add region" />
          </div>
        </div>
        <div class="stacked-list">
          <div class="stacked-list__item grid-cols-[1fr_0fr_0fr]">
            <div>
              <div class="flex items-center gap-2">
                <FlagIcon code="nl" :size="16" class="rounded-sm" />
                <div>AMS1</div>
                <div class="badge badge--small badge--navy">EU</div>
              </div>
              <p>Amsterdam, The Netherlands</p>
            </div>
            <div class="text-end">
              <p class="text-tides-700">mijn.host</p>
            </div>
            <div>
              <UIDropDown
                :items="[
                  { label: 'clicqo.nl', value: '/edit', link: true },
                  { label: 'clicqo.nl', value: 'https://clicqo.nl', link: 'external' },
                  { label: 'galley.run', value: 'https://galley.run', link: true },
                ]"
                :icon="MenuDots"
                variant="icon"
                menu-position="right"
              />
            </div>
          </div>
          <div class="stacked-list__item grid-cols-[1fr_1fr_0fr]">
            <div>
              <div class="flex items-center gap-2">
                <FlagIcon code="de" :size="16" class="rounded-sm" />
                <div>FRA</div>
                <div class="badge badge--small badge--navy">EU</div>
              </div>
              <p>Frankfurt, Germany</p>
            </div>
            <div class="text-end">
              <p class="text-tides-700">AWS</p>
            </div>
            <div>
              <UIDropDown
                :items="[
                  { label: 'clicqo.nl', value: '/edit', link: true },
                  { label: 'clicqo.nl', value: 'https://clicqo.nl', link: 'external' },
                  { label: 'galley.run', value: 'https://galley.run', link: true },
                ]"
                :icon="MenuDots"
                variant="icon"
                menu-position="right"
              />
            </div>
          </div>
          <div class="stacked-list__item grid-cols-[1fr_1fr_0fr]">
            <div>
              <div class="flex items-center gap-2">
                <FlagIcon code="us" :size="16" class="rounded-sm" />
                <div>SFO1</div>
                <div class="badge badge--small badge--navy">USA</div>
              </div>
              <p>San Francisco, United States</p>
            </div>
            <div class="text-end">
              <p class="text-tides-700">Digital Ocean</p>
            </div>
            <div>
              <UIDropDown
                :items="[
                  { label: 'clicqo.nl', value: '/edit', link: true },
                  { label: 'clicqo.nl', value: 'https://clicqo.nl', link: 'external' },
                  { label: 'galley.run', value: 'https://galley.run', link: true },
                ]"
                :icon="MenuDots"
                variant="icon"
                menu-position="right"
              />
            </div>
          </div>
          <div class="stacked-list__item grid-cols-[1fr_1fr_0fr]">
            <div>
              <div class="flex items-center gap-2">
                <FlagIcon code="in" :size="16" class="rounded-sm" />
                <div>BLR1</div>
                <div class="badge badge--small badge--navy">APAC</div>
              </div>
              <p>Bangaluru, India</p>
            </div>
            <div class="text-end">
              <p class="text-tides-700">Azure</p>
            </div>
            <div>
              <UIDropDown
                :items="[
                  { label: 'clicqo.nl', value: '/edit', link: true },
                  { label: 'clicqo.nl', value: 'https://clicqo.nl', link: 'external' },
                  { label: 'galley.run', value: 'https://galley.run', link: true },
                ]"
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
import { computed, watchEffect } from 'vue'

const projectsStore = useProjectsStore()
const { selectedVesselId } = storeToRefs(projectsStore)

const { isLoading: isEngineDataLoading, data: engineData } = useQuery({
  enabled: !!selectedVesselId?.value,
  queryKey: ['vessel', selectedVesselId?.value, 'engine'],
  queryFn: () => axios.get(`/vessels/${selectedVesselId?.value}/engine`),
})

const mode = computed(() => engineData?.value?.data?.[0].attributes.mode)

watchEffect(() => {
  console.log(mode.value)
})
</script>
