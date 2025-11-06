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
import { onMounted, ref } from 'vue'
import UIButton from '@/components/UIButton.vue'
import { useProjectsStore } from '@/stores/projects.ts'
import { useAuthStore } from '@/stores/auth.ts'
import { storeToRefs } from 'pinia'

const projectStore = useProjectsStore()
const authStore = useAuthStore()
const { chartersForDropdown, projectsForDropdown, selectedCharterId, selectedProjectId } = storeToRefs(projectStore)

onMounted(() => {
  projectStore.fetchCharters(authStore.vesselIds)
})
</script>
