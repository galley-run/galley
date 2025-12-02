import { type MaybeRefOrGetter, ref, watch } from 'vue'
import countriesObj from '@/utils/countries'
import getCityHub from '@/utils/getCityHub'
import providersList from '@/utils/providers'
import type { EngineRegionSummary } from '@/types/api/engine'
import { useVesselResource } from './useVesselResource'

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

// Region-specific types
export type RegionCreateData = {
  name: string
  locationCity: string
  locationCountry: string
  geoRegion: string
  providerName: string
}

export type RegionUpdateData = Partial<RegionCreateData>

// Create resource-specific composables
const regionResource = useVesselResource<EngineRegionSummary, RegionCreateData, RegionUpdateData>(
  'engine/regions',
  'regionId',
)

// Export with region-specific names
export function useRegions(vesselId?: MaybeRefOrGetter<string | undefined>) {
  const { resources, ...rest } = regionResource.useResources(vesselId)
  return { regions: resources, ...rest }
}

export function useRegion(
  regionId?: MaybeRefOrGetter<string | undefined>,
  vesselId?: MaybeRefOrGetter<string | undefined>,
) {
  const { resource, ...rest } = regionResource.useResource(regionId, vesselId)
  return { region: resource, ...rest }
}

export function useCreateRegion(vesselId?: MaybeRefOrGetter<string | undefined>) {
  const { createResource, ...rest } = regionResource.useCreateResource(vesselId)
  return { createRegion: createResource, ...rest }
}

export function useUpdateRegion(
  regionId?: MaybeRefOrGetter<string | undefined>,
  vesselId?: MaybeRefOrGetter<string | undefined>,
) {
  const { updateResource, ...rest } = regionResource.useUpdateResource(regionId, vesselId)
  return { updateRegion: updateResource, ...rest }
}

export function useDeleteRegion(vesselId?: MaybeRefOrGetter<string | undefined>) {
  const { deleteResource, ...rest } = regionResource.useDeleteResource(vesselId)
  return { deleteRegion: deleteResource, ...rest }
}

export function useSaveRegion(
  regionId?: MaybeRefOrGetter<string | undefined>,
  vesselId?: MaybeRefOrGetter<string | undefined>,
) {
  const { saveResource, ...rest } = regionResource.useSaveResource(regionId, vesselId)
  return { saveRegion: saveResource, ...rest }
}

// Form helpers with watchers for create/edit forms
export function useRegionFormHelpers(vesselId?: MaybeRefOrGetter<string | undefined>) {
  const name = ref('')
  const providerName = ref('')
  const geoRegion = ref('')
  const locationCity = ref('')
  const locationCountry = ref('')

  const { regions } = useRegions(vesselId)
  const { region } = useRegion(undefined, vesselId)

  const { saveRegion } = useSaveRegion()

  // Load existing region data into form
  watch(
    region,
    (regionData) => {
      if (regionData) {
        name.value = regionData.attributes.name
        providerName.value = regionData.attributes.providerName
        geoRegion.value = regionData.attributes.geoRegion
        locationCity.value = regionData.attributes.locationCity
        locationCountry.value = regionData.attributes.locationCountry
      }
    },
    { immediate: true },
  )

  // Auto-set region name based on city
  watch(locationCity, (newCity) => {
    const hub = getCityHub(newCity)
    let sequence = 1

    if (regions?.value?.length && hub && regions.value.length > 0) {
      const existingRegions = regions.value.filter((r) => r?.attributes?.name?.startsWith(hub))

      if (existingRegions.length > 0) {
        sequence =
          existingRegions.reduce(
            (acc, r) => Math.max(acc, parseInt(r.attributes?.name?.slice(-1)) || 0),
            0,
          ) + 1
      }
    }

    if (hub && !name.value) {
      name.value = hub + sequence
    }
  })

  // Auto-set geo region based on country
  watch(locationCountry, (newCountry) => {
    if (newCountry && countriesObj[newCountry]) {
      geoRegion.value = countriesObj[newCountry].region
    }
  })

  const pristine = ref(true)
  watch([name, providerName, geoRegion, locationCity, locationCountry], (values) => {
    pristine.value = values.filter(Boolean).length === 0
  })

  return {
    saveRegion: () => saveRegion({
      name: name.value,
      providerName: providerName.value,
      geoRegion: geoRegion.value,
      locationCity: locationCity.value,
      locationCountry: locationCountry.value,
    }),
    name,
    providerName,
    geoRegion,
    locationCity,
    locationCountry,
    pristine,
  }
}
