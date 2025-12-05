import { computed, type MaybeRefOrGetter, toValue } from 'vue'
import { useRoute } from 'vue-router'
import { useProjectsStore } from '@/stores/projects.ts'
import { storeToRefs } from 'pinia'

// Helper to get vessel ID from route or store
export function useVesselId(vesselId?: MaybeRefOrGetter<string | undefined>) {
  const route = useRoute()
  const projectsStore = useProjectsStore()
  const { selectedVesselId } = storeToRefs(projectsStore)

  return computed(() => {
    if (vesselId !== undefined) {
      const value = toValue(vesselId)
      if (value) return value
    }
    return (route.params.vesselId as string) || selectedVesselId.value
  })
}

// Helper to get charter ID from route or store
export function useCharterId(charterId?: MaybeRefOrGetter<string | undefined>) {
  const route = useRoute()
  const projectsStore = useProjectsStore()
  const { selectedCharterId } = storeToRefs(projectsStore)

  return computed(() => {
    if (charterId !== undefined) {
      const value = toValue(charterId)
      if (value) return value
    }
    return (route.params.charterId as string) || selectedCharterId.value
  })
}

// Helper to get resource ID from route
export function useResourceId(
  resourceIdParam: string,
  resourceId?: MaybeRefOrGetter<string | undefined>,
) {
  const route = useRoute()

  return computed(() => {
    if (resourceId !== undefined) {
      const value = toValue(resourceId)
      if (value) return value
    }
    return route.params[resourceIdParam] as string
  })
}
