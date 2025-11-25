<template>
  <div class="flex items-center gap-2">
    <UIDropDown v-model="selectedCharterId" variant="inline" :items="chartersForDropdown" />
    <SlashDivider />
    <UIDropDown v-model="selectedProjectId" variant="inline" :items="projectsForDropdown" />
    <SlashDivider />
    <UIButton :leading-addon="MenuDots" variant="neutral" ghost />
  </div>
</template>
<script setup lang="ts">
import UIDropDown from '@/components/FormField/UIDropDown.vue'
import SlashDivider from '@/assets/SlashDivider.vue'
import { MenuDots } from '@solar-icons/vue'
import { onMounted, watch } from 'vue'
import UIButton from '@/components/UIButton.vue'
import { useProjectsStore } from '@/stores/projects.ts'
import { useAuthStore } from '@/stores/auth.ts'
import { storeToRefs } from 'pinia'

const projectStore = useProjectsStore()

const authStore = useAuthStore()
const { chartersForDropdown, projectsForDropdown, charters, selectedCharterId, selectedProjectId } = storeToRefs(projectStore)

watch(selectedCharterId, (newCharterId) => {
  selectedProjectId.value = Object.values(charters.value).find(charter => charter.id === newCharterId)?.projects?.[0]?.id ?? ''
})

onMounted(() => {
  projectStore.fetchCharters(authStore.vesselIds)
})
</script>
