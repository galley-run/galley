<template>
  <div class="flex flex-col gap-8">
    <div class="grid grid-cols-2 md:grid-cols-4 gap-8">
      <DashboardCard title="Monthly Running Cost">€ 25,00</DashboardCard>
      <DashboardCard title="Gateway">
        <a href="https://analytics.clicqo.nl" target="_blank">https://analytics.clicqo.nl</a>
      </DashboardCard>
      <DashboardCard title="Region">
        <FlagIcon code="nl" :size="16" class="rounded-sm" />
        <div>AMS1</div>
      </DashboardCard>
      <DashboardCard title="Health Check">
        <HealthStatus variant="ok" />
      </DashboardCard>
    </div>

    <div class="card">
      <div class="flex items-center">
        <h6 class="flex-1 text-seafoam-300">Applications</h6>
        <div>
          <Button :leading-addon="AddCircle" ghost />
        </div>
      </div>
      <div
        class="stacked-list__list-item grid-cols-[minmax(200px,_1fr)_minmax(75px,_0fr)_minmax(75px,_0fr)_minmax(100px,_1fr)_minmax(150px,_1fr)_0fr]"
      >
        <div>
          <p>Umami</p>
          <p>postgresql-v2.19.0</p>
        </div>
        <div>
          <h6>Ram</h6>
          <p>33%</p>
        </div>
        <div>
          <h6>CPU</h6>
          <p>18%</p>
        </div>
        <div>
          <h6>Health check</h6>
          <HealthStatus variant="ok" />
        </div>
        <div>
          <h6>Last deployed</h6>
          <p class="truncate">2 days ago</p>
        </div>
        <div>
          <DropDown :items="[{ label: 'clicqo.nl', value: '/edit', link: true }, { label: 'clicqo.nl', value: 'https://clicqo.nl', link: 'external' }, { label: 'galley.run', value: 'https://galley.run', link: true }]" :icon="MenuDotsCircle" variant="icon" menu-position="right" />
        </div>
      </div>
    </div>

    <div class="card">

    </div>
  </div>
</template>
<script setup lang="ts">
import FlagIcon from 'vue3-flag-icons'
import { AddCircle, MenuDotsCircle } from '@solar-icons/vue'

import DashboardCard from '@/components/Dashboard/DashboardCard.vue'
import Button from '@/components/Button.vue'
import HealthStatus from '@/components/HealthStatus.vue'
import DropDown from '@/components/FormField/DropDown.vue'
</script>
