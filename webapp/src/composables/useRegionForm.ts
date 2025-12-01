import { ref, watch } from 'vue'
import countriesObj from '@/utils/countries'
import getCityHub from '@/utils/getCityHub'
import providersList from '@/utils/providers'
import { useMutation, useQuery } from '@tanstack/vue-query'
import axios, { type AxiosResponse } from 'axios'
import type { ApiResponse } from '@/types/api'
import type { EngineRegionSummary } from '@/types/api/engine'
import { useRoute } from 'vue-router'
import { useProjectsStore } from '@/stores/projects.ts'
import { storeToRefs } from 'pinia'

export const geoRegions = [
  { value: 'eu', label: 'Europe' },
  { value: 'usa', label: 'United States' },
  { value: 'na', label: 'North America' },
  { value: 'latam', label: 'Latin America' },
  { value: 'apac', label: 'Asia-Pacific' },
  { value: 'africa', label: 'Africa' },
]

export const countries = Object.entries(countriesObj).map(([countryCode, country]) => ({
  value: countryCode,
  label: country.name,
}))

export const providers = providersList.map((provider) => ({
  value: provider,
  label: provider,
}))

export function useRegionForm() {
  const route = useRoute()
  const { vesselId, regionId } = route.params
  const projectsStore = useProjectsStore()
  const { selectedVesselId } = storeToRefs(projectsStore)


  const name = ref('')
  const provider = ref('')
  const geoRegion = ref('')
  const city = ref('')
  const country = ref('')

  const { data: engineRegions } = useQuery({
    enabled: !!vesselId || !!selectedVesselId.value,
    queryKey: ['vessel', vesselId || selectedVesselId.value, 'engine', 'regions'],
    queryFn: () =>
      axios.get<ApiResponse<EngineRegionSummary>[], ApiResponse<EngineRegionSummary>[]>(
        `/vessels/${vesselId || selectedVesselId.value}/engine/regions`,
      ),
  })

  const { data: engineRegion } = useQuery({
    enabled: !!regionId && (!!vesselId || !!selectedVesselId.value),
    queryKey: ['vessel', vesselId || selectedVesselId.value, 'engine', 'regions', regionId],
    queryFn: () =>
      axios.get<ApiResponse<EngineRegionSummary>, ApiResponse<EngineRegionSummary>>(
        `/vessels/${vesselId || selectedVesselId.value}/engine/regions/${regionId}`,
      ),
  })

  watch(engineRegion, (region) => {
    if (region) {
      name.value = region.attributes.name
      provider.value = region.attributes.providerName
      geoRegion.value = region.attributes.geoRegion
      city.value = region.attributes.locationCity
      country.value = region.attributes.locationCountry
    }
  }, { immediate: true })

  // Auto-set region name based on city
  watch(city, (newCity) => {
    const hub = getCityHub(newCity)
    let sequence = 1

    if (engineRegions?.value?.length && hub && engineRegions?.value?.length > 0) {
      const regions = engineRegions.value.filter((region) =>
        region?.attributes?.name?.startsWith(hub),
      )

      if (regions.length > 0) {
        sequence =
          regions.reduce(
            (acc, region) => Math.max(acc, parseInt(region.attributes?.name?.slice(-1))),
            0,
          ) + 1
      }
    }

    if (hub && !name.value) {
      name.value = hub + sequence
    }
  })

  // Auto-set geo region based on country
  watch(country, (newCountry) => {
    if (newCountry && countriesObj[newCountry]) {
      geoRegion.value = countriesObj[newCountry].region
    }
  })

  const pristine = ref(true)
  watch([name, provider, geoRegion, city, country], (values) => {
    pristine.value = values.filter(Boolean).length === 0
  })

  // Mutation for creating/updating regions
  const saveRegionMutation = useMutation({
    mutationFn: ({
      regionId,
      data,
    }: {
      regionId?: string
      data: {
        name: string
        locationCity: string
        locationCountry: string
        geoRegion: string
        providerName: string
      }
    }) => {
      if (regionId) {
        return axios.patch<AxiosResponse<EngineRegionSummary>, AxiosResponse<EngineRegionSummary>>(
          `/vessels/${vesselId || selectedVesselId.value}/engine/regions/${regionId}`,
          data,
        )
      }
      return axios.post<AxiosResponse<EngineRegionSummary>, AxiosResponse<EngineRegionSummary>>(
        `/vessels/${vesselId || selectedVesselId.value}/engine/regions`,
        data,
      )
    },
  })

  const saveRegion = async (targetRegionId?: string) => {
    return await saveRegionMutation.mutateAsync({
      regionId: targetRegionId,
      data: {
        name: name.value,
        locationCity: city.value,
        locationCountry: country.value,
        geoRegion: geoRegion.value,
        providerName: provider.value,
      },
    })
  }

  // Mutation for deleting regions
  const deleteRegionMutation = useMutation({
    mutationFn: (regionId: string) =>
      axios.delete(`/vessels/${vesselId || selectedVesselId.value}/engine/regions/${regionId}`),
  })

  const deleteRegion = async (targetRegionId: string) => {
    // TODO: Show error message if delete fails
    return await deleteRegionMutation.mutateAsync(targetRegionId)
  }

  return {
    name,
    provider,
    geoRegion,
    city,
    country,
    pristine,
    engineRegions,
    saveRegion,
    saveRegionMutation,
    deleteRegion,
    deleteRegionMutation,
  }
}
