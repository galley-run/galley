<template>
  <div class="flex flex-col gap-8">
    <div class="card">
      <h1>Compute plans</h1>
      <p>
        This configuration can be used to bind applications and/or databases to. You can change
        compute plans until they are in used. You can’t edit compute plans when there are
        applications deployed with this configuration.
      </p>
    </div>
    <div class="grid grid-cols-2 gap-8 items-start">
      <div class="card">
        <div class="card__header">
          <h2>Applications</h2>
          <div>
            <!--          <UIButton ghost :leading-addon="Import" title="Import compute configurations" />-->
            <UIButton
              ghost
              :to="`/charter/${selectedCharterId}/compute-plan/add`"
              :leading-addon="AddCircle"
              title="Add compute configuration"
            />
          </div>
        </div>
        <div class="stacked-list" v-if="computePlans && computePlans.length > 0">
          <div
            class="stacked-list__item grid-cols-[1fr_max-content_max-content]"
            v-for="computePlan in computePlansApplications"
            :key="computePlan.id"
          >
            <div class="relative">
              <div class="flex items-center gap-2">
                <div>{{ computePlan.attributes.name }}</div>
                <div
                  class="badge badge--cliona badge--small"
                  v-if="
                    computePlan.attributes.requests.cpu !== computePlan.attributes?.limits?.cpu ||
                    computePlan.attributes.requests.memory !==
                      computePlan.attributes?.limits?.memory
                  "
                >
                  Burstable
                </div>
              </div>
              <p>
                {{ computePlan.attributes.requests.cpu }} vCPU &bullet;
                {{ computePlan.attributes.requests.memory }} GB RAM
              </p>
              <RouterLink
                :to="`/charter/${selectedCharterId}/compute-plan/${computePlan.id}`"
                class="absolute inset-0"
              ></RouterLink>
            </div>
            <!--          TODO: Enable this when we have billing figured out .-->
            <!--          <div>-->
            <!--            <p>€{{ shortCurrencyFormat(computePlan.attributes.runningCost) }} p/m</p>-->
            <!--          </div>-->
            <div>
              <UIDropDown
                :items="[
                  { label: 'Edit', value: 'edit' },
                  {
                    label: 'Delete configuration',
                    onClick: () => {
                      confirmDeleteDialog = computePlan.id
                      return
                    },
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
        <div v-else>Add your first compute configuration to get started.</div>
      </div>
      <div class="card">
        <div class="card__header">
          <h2>Databases</h2>
          <div>
            <!--          <UIButton ghost :leading-addon="Import" title="Import compute configurations" />-->
            <UIButton
              ghost
              :to="`/charter/${selectedCharterId}/compute-plan/add`"
              :leading-addon="AddCircle"
              title="Add compute configuration"
            />
          </div>
        </div>
        <div class="stacked-list" v-if="computePlans && computePlans.length > 0">
          <div
            class="stacked-list__item grid-cols-[1fr_max-content_max-content]"
            v-for="computePlan in computePlansDatabases"
            :key="computePlan.id"
          >
            <div class="relative">
              <div class="flex items-center gap-2">
                <div>{{ computePlan.attributes.name }}</div>
                <div
                  class="badge badge--cliona badge--small"
                  v-if="
                    computePlan.attributes.requests.cpu !== computePlan.attributes?.limits?.cpu ||
                    computePlan.attributes.requests.memory !==
                      computePlan.attributes?.limits?.memory
                  "
                >
                  Burstable
                </div>
              </div>
              <p>
                {{ computePlan.attributes.requests.cpu }} vCPU &bullet;
                {{ computePlan.attributes.requests.memory }} GB RAM
              </p>
              <RouterLink
                :to="`/charter/${selectedCharterId}/compute-plan/${computePlan.id}`"
                class="absolute inset-0"
              ></RouterLink>
            </div>
            <!--          TODO: Enable this when we have billing figured out .-->
            <!--          <div>-->
            <!--            <p>€{{ shortCurrencyFormat(computePlan.attributes.runningCost) }} p/m</p>-->
            <!--          </div>-->
            <div>
              <UIDropDown
                :items="[
                  { label: 'Edit', value: 'edit' },
                  {
                    label: 'Delete configuration',
                    onClick: () => {
                      confirmDeleteDialog = computePlan.id
                      return
                    },
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
        <div v-else>Add your first compute configuration to get started.</div>
      </div>
    </div>
  </div>
  <ConfirmDeleteComputePlanDialog
    :show="!!confirmDeleteDialog"
    @close="confirmDeleteDialog = undefined"
    @confirm="onDelete"
    :compute-plan-id="confirmDeleteDialog"
  />
</template>
<script setup lang="ts">
import { AddCircle, MenuDots } from '@solar-icons/vue'
import UIDropDown from '@/components/FormField/UIDropDown.vue'
import UIButton from '@/components/UIButton.vue'
import { useProjectsStore } from '@/stores/projects.ts'
import { storeToRefs } from 'pinia'
import { useComputePlans } from '@/composables/useComputePlan.ts'
import { computed, ref } from 'vue'
import ConfirmDeleteComputePlanDialog from '@/components/Dialog/ConfirmDeleteComputePlanDialog.vue'
import { toBytes } from '@/utils/bytes.ts'

const projectStore = useProjectsStore()
const { selectedCharterId } = storeToRefs(projectStore)

const { computePlans } = useComputePlans()

const computePlansApplications = computed(() =>
  computePlans?.value
    ?.filter((cp) => cp.attributes.application === 'applications' || cp.attributes.application === 'applications_databases')
    .sort((a, b) => {
      const cpuA = parseFloat(a.attributes.requests.cpu)
      const cpuB = parseFloat(b.attributes.requests.cpu)
      if (cpuA !== cpuB) {
        return cpuA - cpuB
      }
      const memA = toBytes(a.attributes.requests.memory)
      const memB = toBytes(b.attributes.requests.memory)
      return memA - memB
    }),
)
const computePlansDatabases = computed(() =>
  computePlans?.value
    ?.filter((cp) => cp.attributes.application === 'databases' || cp.attributes.application === 'applications_databases')
    .sort((a, b) => {
      const cpuA = parseFloat(a.attributes.requests.cpu)
      const cpuB = parseFloat(b.attributes.requests.cpu)
      if (cpuA !== cpuB) {
        return cpuA - cpuB
      }
      const memA = toBytes(a.attributes.requests.memory)
      const memB = toBytes(b.attributes.requests.memory)
      return memA - memB
    }),
)

const confirmDeleteDialog = ref<undefined | string>(undefined)

async function onDelete() {
  confirmDeleteDialog.value = undefined
}
</script>
