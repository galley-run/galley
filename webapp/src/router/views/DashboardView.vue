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

    <div class="card card--compact">
      <div class="card__header">
        <h6>Applications</h6>
        <UIButton :leading-addon="AddCircle" ghost />
      </div>
      <div class="stacked-list">
        <div
          class="stacked-list__item grid-cols-[minmax(200px,_1fr)_minmax(75px,100px)_minmax(75px,125px)_minmax(100px,_1fr)_minmax(150px,_1fr)_0fr]"
        >
          <div>
            <a>Umami</a>
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

    <div class="grid grid-cols-2 gap-8 items-start">
      <div class="card card--compact">
        <div class="card__header">
          <h6>Databases</h6>
          <UIButton :leading-addon="AddCircle" ghost />
        </div>
        <div class="stacked-list">
          <div
            class="stacked-list__item grid-cols-[minmax(100px,1fr)_minmax(75px,100px)_minmax(75px,125px)_minmax(100px,1fr)_min-content]"
          >
            <div>
              <a>Postgres</a>
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
            <div class="items-center">
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
          <div
            class="stacked-list__item grid-cols-[minmax(100px,1fr)_minmax(75px,100px)_minmax(75px,125px)_minmax(100px,1fr)_min-content]"
          >
            <div class="relative">
              <RouterLink to="/redis"
                >Redis
                <div class="absolute inset-0"></div
              ></RouterLink>
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
              <UIDropDown
                :items="[
                  { label: 'Edit', value: '/edit' },
                  { label: 'Delete', value: '/edit', variant: 'danger' },
                ]"
                :icon="MenuDots"
                variant="icon"
                menu-position="right"
              />
            </div>
          </div>
          <div
            class="stacked-list__item grid-cols-[minmax(100px,1fr)_minmax(75px,100px)_minmax(75px,125px)_minmax(100px,1fr)_min-content]"
          >
            <div>
              <a>Postgres</a>
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
      <div class="card card--compact">
        <div class="card__header">
          <h6>Object Storage</h6>
          <UIButton :leading-addon="AddCircle" ghost />
        </div>
        <div class="text-tides-700">Not configured</div>
      </div>
      <div class="card card--compact">
        <div class="card__header">
          <h6>Releases</h6>
        </div>
        <div class="stacked-list">
          <div class="stacked-list__item grid-cols-[2fr_2fr_1fr_1fr_min-content]">
            <div class="relative">
              <a class="truncate" href="https://github.com/galley-run" target="_blank"
                >Today, 13:15
                <div class="absolute inset-0"
              /></a>
            </div>
            <div>
              <h6>By</h6>
              <p class="truncate">marsbergen</p>
            </div>
            <div>
              <h6>Git tag</h6>
              <p class="truncate">v6</p>
            </div>
            <div>
              <h6>Git branch</h6>
              <p class="truncate">main</p>
            </div>
            <div>
              <UIButton
                :leading-addon="Notebook"
                ghost
                href="https://github.com/galley-run"
                target="_blank"
              />
            </div>
          </div>
          <div class="stacked-list__item grid-cols-[2fr_2fr_1fr_1fr_min-content]">
            <div class="relative">
              <a class="truncate" href="https://github.com/galley-run" target="_blank"
                >Yesterday, 21:56
                <div class="absolute inset-0"
              /></a>
            </div>
            <div>
              <h6>By</h6>
              <p class="truncate">marsbergen</p>
            </div>
            <div>
              <h6>Git tag</h6>
              <p class="truncate">v5</p>
            </div>
            <div>
              <h6>Git branch</h6>
              <p class="truncate">main</p>
            </div>
            <div>
              <UIButton
                :leading-addon="Notebook"
                ghost
                href="https://github.com/galley-run"
                target="_blank"
              />
            </div>
          </div>
        </div>
      </div>
      <div class="card card--compact">
        <div class="card__header">
          <h6>Alerts</h6>
          <UIButton :leading-addon="AddCircle" ghost />
        </div>
        <div class="text-tides-700">Not configured</div>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import FlagIcon from 'vue3-flag-icons'
import { AddCircle, MenuDots, Notebook } from '@solar-icons/vue'

import DashboardCard from '@/components/Dashboard/DashboardCard.vue'
import UIButton from '@/components/UIButton.vue'
import HealthStatus from '@/components/HealthStatus.vue'
import UIDropDown from '@/components/FormField/UIDropDown.vue'
</script>
