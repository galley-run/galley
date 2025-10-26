<template>
  <div class="flex flex-col gap-8">
    <div class="card">
      <h1>Engine</h1>
      <p>Here you can configure the engine of your platform for your tenants.</p>
      
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
            <UiButton ghost :leading-addon="DocumentsMinimalistic" title="Visualise nodes" />
            <UiButton ghost :leading-addon="AddCircle" title="Add node" />
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
              <DropDown
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
              <DropDown
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
            <UiButton ghost :leading-addon="AddCircle" title="Add region" />
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
              <DropDown
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
              <DropDown
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
              <DropDown
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
              <DropDown
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
import { AddCircle, DocumentsMinimalistic, MenuDots } from '@solar-icons/vue'
import DropDown from '@/components/FormField/DropDown.vue'
import UiButton from '@/components/UIButton.vue'
import FlagIcon from 'vue3-flag-icons'
import DashboardCard from '@/components/Dashboard/DashboardCard.vue'
import HealthStatus from '@/components/HealthStatus.vue'
</script>
