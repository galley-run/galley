<template>
  <div class="flex flex-col gap-8">
    <div class="grid grid-cols-2 md:grid-cols-4 gap-8">
      <DashboardCard title="Monthly Running Cost">€ 25,00</DashboardCard>
      <DashboardCard title="Gateway">https://analytics.clicqo.nl</DashboardCard>
      <DashboardCard title="Region">
        <FlagIcon code="nl" :size="16" class="rounded-sm" />
        <div>AMS1</div>
      </DashboardCard>
      <DashboardCard title="Health Check">
        <SmileSquare class="text-seafoam-600" />
        OK
      </DashboardCard>
    </div>

    <div class="card">
      <div class="flex items-center">
        <h6 class="flex-1 text-seafoam-300">Applications</h6>
        <div>
          <Button :icon="AddCircle" ghost />
        </div>
      </div>
      <div
        class="grid grid-cols-[minmax(200px,_1fr)_minmax(75px,_0fr)_minmax(75px,_0fr)_minmax(100px,_1fr)_minmax(150px,_1fr)_0fr] items-center gap-4"
      >
        <div>
          <p class="text-seafoam-800">Umami</p>
          <p class="text-tides-700 text-sm">postgresql-v2.19.0</p>
        </div>
        <div>
          <h6 class="text-navy-200">Ram</h6>
          <p>33%</p>
        </div>
        <div>
          <h6 class="text-navy-200">CPU</h6>
          <p>18%</p>
        </div>
        <div>
          <h6 class="text-navy-200">Health check</h6>
          <p class="flex gap-4 items-center truncate">
            <SmileSquare class="text-seafoam-600" />
            OK
          </p>
        </div>
        <div>
          <h6 class="text-navy-200">Last deployed</h6>
          <p class="truncate">2 days ago</p>
        </div>
        <div>
          <Button :icon="MenuDotsCircle" ghost />
        </div>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import FlagIcon from 'vue3-flag-icons'
import { AddCircle, MenuDotsCircle, SmileSquare } from '@solar-icons/vue'

import DashboardCard from '@/components/Dashboard/DashboardCard.vue'
import Button from '@/components/Button.vue'
</script>
