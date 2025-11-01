<template>
  <div class="flex items-center gap-2">
    <UIDropDown v-model="charter" variant="inline" :items="charters" />
    <SlashDivider />
    <UIDropDown v-model="project" variant="inline" :items="projects" />
    <SlashDivider />
    <UIButton :leading-addon="MenuDots" variant="neutral" ghost />
  </div>
</template>
<script setup lang="ts">
import UIDropDown from '@/components/FormField/UIDropDown.vue'
import SlashDivider from '@/assets/SlashDivider.vue'
import { MenuDots } from '@solar-icons/vue'
import { ref } from 'vue'
import UIButton from '@/components/UIButton.vue'

const charters = [{ label: 'Clicqo', value: '2DAC9D56-1B2E-4364-9F01-1E457ACB26B4' }]

const projects = [
  { label: 'clicqo.nl', value: 'B1292432-4E30-4141-A834-15D5D9182241' },
  { label: 'staging.clicqo.nl', value: '3B27546C-8F25-4C45-BE9B-6E6340386162' },
]

const charter = ref<string | null>('2DAC9D56-1B2E-4364-9F01-1E457ACB26B4')
const project = ref<string | null>('B1292432-4E30-4141-A834-15D5D9182241')
</script>
