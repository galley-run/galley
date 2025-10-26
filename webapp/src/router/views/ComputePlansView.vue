<template>
  <div class="flex flex-col gap-8">
    <div class="card">
      <h1>Compute plans</h1>
      <p>
        This configuration can be used to bind applications and/or databases to. You can change
        billing settings later, however once the configuration is set. You can’t edit it for
        applications deployed with this virtual server.
      </p>
      <div class="flex opacity-30">
        <SlashesDivider />
        <SlashesDivider />
        <SlashesDivider />
        <SlashesDivider />
        <SlashesDivider />
        <SlashesDivider />
        <SlashesDivider />
        <SlashesDivider />
      </div>
    </div>
    <div class="card">
      <div class="card__header">
        <h2>Compute configurations</h2>
        <div>
          <UiButton ghost :leading-addon="Import" title="Import compute configurations" />
          <UiButton ghost :leading-addon="AddCircle" title="Add compute configuration" />
        </div>
      </div>
      <div class="stacked-list">
        <div
          class="stacked-list__item grid-cols-[1fr_max-content_max-content]"
          v-for="computeConfiguration in computeConfigurations"
          :key="computeConfiguration.label"
        >
          <div>
            <p>{{ computeConfiguration.label }}</p>
            <p>{{ computeConfiguration.cpu }} vCPU &bullet; {{ computeConfiguration.memory }} GB RAM</p>
          </div>
          <div>
            <p>€{{ shortCurrencyFormat(computeConfiguration.runningCost) }} p/m</p>
          </div>
          <div>
            <DropDown
              :items="[
                { label: 'Edit', value: 'edit' },
                { label: 'Delete', value: 'delete', variant: 'danger' },
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
</template>
<script setup lang="ts">
import SlashesDivider from '@/assets/SlashesDivider.vue'
import { AddCircle, Import, MenuDots } from '@solar-icons/vue'
import DropDown from '@/components/FormField/DropDown.vue'
import UiButton from '@/components/UIButton.vue'
import shortCurrencyFormat from '@/utils/shortCurrencyFormat.ts'

const computeConfigurations = [
  { label: 'Compute XS', cpu: 1, memory: 1, runningCost: 5 },
  { label: 'Compute S', cpu: 1, memory: 2, runningCost: 7.5 },
  { label: 'Compute M', cpu: 2, memory: 2, runningCost: 10 },
  { label: 'Compute L', cpu: 2, memory: 4, runningCost: 15 },
  { label: 'Compute XL', cpu: 4, memory: 4, runningCost: 25 },
]
</script>
