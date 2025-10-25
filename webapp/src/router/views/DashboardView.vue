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
      <div class="card__header">
        <h6>Applications</h6>
        <Button :leading-addon="AddCircle" ghost />
      </div>
      <div
        class="stacked-list__list-item grid-cols-[minmax(200px,_1fr)_minmax(75px,100px)_minmax(75px,125px)_minmax(100px,_1fr)_minmax(150px,_1fr)_0fr]"
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
          <DropDown
            :items="[
              { label: 'clicqo.nl', value: '/edit', link: true },
              { label: 'clicqo.nl', value: 'https://clicqo.nl', link: 'external' },
              { label: 'galley.run', value: 'https://galley.run', link: true },
            ]"
            :icon="MenuDotsCircle"
            variant="icon"
            menu-position="right"
          />
        </div>
      </div>
    </div>

    <div class="grid grid-cols-2 gap-8 items-start">
      <div class="card">
        <div class="card__header">
          <h6>Databases</h6>
          <Button :leading-addon="AddCircle" ghost />
        </div>
        <div class="stacked-list__list-item grid-cols-[minmax(100px,1fr)_minmax(75px,100px)_minmax(75px,125px)_minmax(100px,1fr)_min-content]">
          <div>
            <p>Postgres</p>
            <p>15-alpine</p>
          </div>
          <div>
            <h6>Ram</h6>
            <p>33%</p>
          </div>
          <div>
            <h6>CPU</h6>
            <p>33%</p>
          </div>
          <div>
            <h6>Last updated</h6>
            <p>15 days ago</p>
          </div>
          <div>
            <DropDown
              :items="[
                { label: 'clicqo.nl', value: '/edit', link: true },
                { label: 'clicqo.nl', value: 'https://clicqo.nl', link: 'external' },
                { label: 'galley.run', value: 'https://galley.run', link: true },
              ]"
              :icon="MenuDotsCircle"
              variant="icon"
              menu-position="right"
            />
          </div>
        </div>
        <div class="stacked-list__list-item grid-cols-[minmax(100px,1fr)_minmax(75px,100px)_minmax(75px,125px)_minmax(100px,1fr)_min-content]">
          <div>
            <p>Redis</p>
            <p>2.23.15-alpine</p>
          </div>
          <div>
            <h6>Ram</h6>
            <p>14%</p>
          </div>
          <div>
            <h6>CPU</h6>
            <p>3%</p>
          </div>
          <div>
            <h6>Last updated</h6>
            <p>1 hour ago</p>
          </div>
          <div>
            <DropDown
              :items="[]"
              :icon="MenuDotsCircle"
              variant="icon"
              menu-position="right"
            />
          </div>
        </div>
        <div class="stacked-list__list-item grid-cols-[minmax(100px,1fr)_minmax(75px,100px)_minmax(75px,125px)_minmax(100px,1fr)_min-content]">
          <div>
            <p>Postgres</p>
            <p>15-alpine</p>
          </div>
          <div>
            <h6>Ram</h6>
            <p>33%</p>
          </div>
          <div>
            <h6>CPU</h6>
            <p>33%</p>
          </div>
          <div>
            <h6>Last updated</h6>
            <p>15 days ago</p>
          </div>
          <div>
            <DropDown
              :items="[
                { label: 'clicqo.nl', value: '/edit', link: true },
                { label: 'clicqo.nl', value: 'https://clicqo.nl', link: 'external' },
                { label: 'galley.run', value: 'https://galley.run', link: true },
              ]"
              :icon="MenuDotsCircle"
              variant="icon"
              menu-position="right"
            />
          </div>
        </div>
      </div>
      <div class="card">
        <div class="card__header">
          <h6>Object Storage</h6>
          <Button :leading-addon="AddCircle" ghost />
        </div>
        <div class="stacked-list__list-item grid-cols-[repeat(4,auto)_min-content]">
          <div>
            <p>Postgres</p>
            <p>15-alpine</p>
          </div>
          <div>
            <h6>Ram</h6>
            <p>33%</p>
          </div>
          <div>
            <h6>CPU</h6>
            <p>33%</p>
          </div>
          <div>
            <h6>Last updated</h6>
            <p>15 days ago</p>
          </div>
          <div>
            <DropDown
              :items="[
                { label: 'clicqo.nl', value: '/edit', link: true },
                { label: 'clicqo.nl', value: 'https://clicqo.nl', link: 'external' },
                { label: 'galley.run', value: 'https://galley.run', link: true },
              ]"
              :icon="MenuDotsCircle"
              variant="icon"
              menu-position="right"
            />
          </div>
        </div>
      </div>
      <div class="card">
        <div class="card__header">
          <h6>Object Storage</h6>
          <Button :leading-addon="AddCircle" ghost />
        </div>
      </div>
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
