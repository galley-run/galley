import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { useRegionForm, geoRegions, countries, providers } from '../useRegionForm'
import { createPinia } from 'pinia'
import { useProjectsStore } from '@/stores/projects'
import axios from 'axios'
import { defineComponent } from 'vue'
import { mount, flushPromises } from '@vue/test-utils'
import { VueQueryPlugin, QueryClient } from '@tanstack/vue-query'

vi.mock('axios')

const mockRoute = {
  params: {
    vesselId: 'vessel1',
    regionId: undefined,
  },
}

vi.mock('vue-router', () => ({
  useRoute: () => mockRoute,
}))

// Helper function to create a wrapper component for testing composables
function withSetup(composable: () => any) {
  let result: any
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: 0,
      },
    },
  })

  const TestComponent = defineComponent({
    setup() {
      result = composable()
      return () => {}
    },
  })

  const pinia = createPinia()

  const wrapper = mount(TestComponent, {
    global: {
      plugins: [pinia, [VueQueryPlugin, { queryClient }]],
    },
  })

  return { result, wrapper, queryClient }
}

describe('useRegionForm', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockRoute.params = {
      vesselId: 'vessel1',
      regionId: undefined,
    }
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  describe('constants', () => {
    it('exports geoRegions array', () => {
      expect(geoRegions).toHaveLength(6)
      expect(geoRegions[0]).toEqual({ value: 'eu', label: 'Europe' })
    })

    it('exports countries array', () => {
      expect(Array.isArray(countries)).toBe(true)
      expect(countries.length).toBeGreaterThan(0)
      expect(countries[0]).toHaveProperty('value')
      expect(countries[0]).toHaveProperty('label')
    })

    it('exports providers array', () => {
      expect(Array.isArray(providers)).toBe(true)
      expect(providers.length).toBeGreaterThan(0)
      expect(providers[0]).toHaveProperty('value')
      expect(providers[0]).toHaveProperty('label')
    })
  })

  describe('initialization', () => {
    it('initializes with empty form values', () => {
      const { result } = withSetup(() => useRegionForm())
      const { name, provider, geoRegion, city, country } = result

      expect(name.value).toBe('')
      expect(provider.value).toBe('')
      expect(geoRegion.value).toBe('')
      expect(city.value).toBe('')
      expect(country.value).toBe('')
    })

    it('initializes pristine as true', () => {
      const { result } = withSetup(() => useRegionForm())

      expect(result.pristine.value).toBe(true)
    })
  })

  describe('pristine state', () => {
    it('sets pristine to false when any field has a value', async () => {
      const { result } = withSetup(() => useRegionForm())
      const { name, pristine } = result

      name.value = 'test'
      await flushPromises()

      expect(pristine.value).toBe(false)
    })

    it('remains true when all fields are empty', async () => {
      const { result } = withSetup(() => useRegionForm())

      await flushPromises()

      expect(result.pristine.value).toBe(true)
    })

    it('becomes false when multiple fields are set', async () => {
      const { result } = withSetup(() => useRegionForm())

      result.name.value = 'AMS1'
      result.city.value = 'amsterdam'
      await flushPromises()

      expect(result.pristine.value).toBe(false)
    })
  })

  describe('city watch - auto-set region name', () => {
    it('auto-generates region name from city hub', async () => {
      vi.mocked(axios.get).mockResolvedValue([])

      const { result } = withSetup(() => useRegionForm())

      result.city.value = 'amsterdam'
      await flushPromises()

      expect(result.name.value).toBe('AMS1')
    })

    it('increments sequence for duplicate hubs', async () => {
      vi.mocked(axios.get).mockResolvedValue([
        { attributes: { name: 'AMS1' } },
        { attributes: { name: 'AMS2' } },
      ])

      const { result } = withSetup(() => useRegionForm())

      await flushPromises()

      result.city.value = 'amsterdam'
      await flushPromises()

      expect(result.name.value).toBe('AMS3')
    })

    it('handles non-sequential region names', async () => {
      vi.mocked(axios.get).mockResolvedValue([
        { attributes: { name: 'AMS1' } },
        { attributes: { name: 'AMS5' } },
      ])

      const { result } = withSetup(() => useRegionForm())

      await flushPromises()

      result.city.value = 'amsterdam'
      await flushPromises()

      expect(result.name.value).toBe('AMS6')
    })

    it('does not overwrite existing name', async () => {
      vi.mocked(axios.get).mockResolvedValue([])

      const { result } = withSetup(() => useRegionForm())

      result.name.value = 'CustomName'
      result.city.value = 'amsterdam'
      await flushPromises()

      expect(result.name.value).toBe('CustomName')
    })

    it('handles unknown cities gracefully', async () => {
      vi.mocked(axios.get).mockResolvedValue([])

      const { result } = withSetup(() => useRegionForm())

      result.city.value = 'unknown-city'
      await flushPromises()

      expect(result.name.value).toBe('')
    })

    it('normalizes city names correctly', async () => {
      vi.mocked(axios.get).mockResolvedValue([])

      const { result } = withSetup(() => useRegionForm())

      result.city.value = 'New York'
      await flushPromises()

      expect(result.name.value).toBe('NYC1')
    })
  })

  describe('country watch - auto-set geo region', () => {
    it('auto-sets geo region based on country', async () => {
      const { result } = withSetup(() => useRegionForm())

      result.country.value = 'nl'
      await flushPromises()

      expect(result.geoRegion.value).toBe('eu')
    })

    it('updates geo region for different countries', async () => {
      const { result } = withSetup(() => useRegionForm())

      result.country.value = 'us'
      await flushPromises()
      expect(result.geoRegion.value).toBe('usa')

      result.country.value = 'br'
      await flushPromises()
      expect(result.geoRegion.value).toBe('latam')
    })

    it('handles unknown countries gracefully', async () => {
      const { result } = withSetup(() => useRegionForm())

      result.country.value = 'XX'
      await flushPromises()

      expect(result.geoRegion.value).toBe('')
    })
  })

  describe('saveRegion', () => {
    it('creates new region when no regionId provided', async () => {
      vi.mocked(axios.post).mockResolvedValue({ data: { id: 'new-region' } })

      const { result } = withSetup(() => useRegionForm())

      result.name.value = 'AMS1'
      result.city.value = 'amsterdam'
      result.country.value = 'nl'
      result.geoRegion.value = 'eu'
      result.provider.value = 'AWS'

      await result.saveRegion()

      expect(axios.post).toHaveBeenCalledWith('/vessels/vessel1/engine/regions', {
        name: 'AMS1',
        locationCity: 'amsterdam',
        locationCountry: 'nl',
        geoRegion: 'eu',
        providerName: 'AWS',
      })
    })

    it('updates existing region when regionId provided', async () => {
      vi.mocked(axios.patch).mockResolvedValue({ data: { id: 'region1' } })

      const { result } = withSetup(() => useRegionForm())

      result.name.value = 'AMS1'
      result.city.value = 'amsterdam'
      result.country.value = 'nl'
      result.geoRegion.value = 'eu'
      result.provider.value = 'AWS'

      await result.saveRegion('region1')

      expect(axios.patch).toHaveBeenCalledWith('/vessels/vessel1/engine/regions/region1', {
        name: 'AMS1',
        locationCity: 'amsterdam',
        locationCountry: 'nl',
        geoRegion: 'eu',
        providerName: 'AWS',
      })
    })

    it('returns the response from API', async () => {
      const mockResponse = { data: { id: 'new-region', attributes: { name: 'AMS1' } } }
      vi.mocked(axios.post).mockResolvedValue(mockResponse)

      const { result } = withSetup(() => useRegionForm())

      const response = await result.saveRegion()

      expect(response).toEqual(mockResponse)
    })

    it('uses selectedVesselId from store when no route vesselId', async () => {
      vi.mocked(axios.post).mockResolvedValue({ data: { id: 'new-region' } })

      mockRoute.params = {}

      const { result } = withSetup(() => {
        const store = useProjectsStore()
        store.charters = [
          {
            id: 'charter1',
            vesselId: 'vessel2',
            name: 'Test Charter',
            description: 'Test',
            projects: [],
          },
        ]
        store.selectedCharterId = 'charter1'
        return useRegionForm()
      })

      await result.saveRegion()

      expect(axios.post).toHaveBeenCalledWith('/vessels/vessel2/engine/regions', expect.any(Object))
    })
  })

  describe('deleteRegion', () => {
    it('deletes region by id', async () => {
      vi.mocked(axios.delete).mockResolvedValue({})

      const { result } = withSetup(() => useRegionForm())

      await result.deleteRegion('region1')

      expect(axios.delete).toHaveBeenCalledWith('/vessels/vessel1/engine/regions/region1')
    })

    it('returns the response from API', async () => {
      const mockResponse = { status: 204 }
      vi.mocked(axios.delete).mockResolvedValue(mockResponse)

      const { result } = withSetup(() => useRegionForm())

      const response = await result.deleteRegion('region1')

      expect(response).toEqual(mockResponse)
    })
  })

  describe('loading existing region', () => {
    it('populates form when engineRegion data loads', async () => {
      const mockRegionData = {
        attributes: {
          name: 'AMS1',
          providerName: 'AWS',
          geoRegion: 'eu',
          locationCity: 'amsterdam',
          locationCountry: 'nl',
        },
      }

      mockRoute.params = {
        vesselId: 'vessel1',
        regionId: 'region1',
      }

      vi.mocked(axios.get).mockResolvedValue(mockRegionData)

      const { result } = withSetup(() => useRegionForm())

      await flushPromises()

      expect(result.name.value).toBe('AMS1')
      expect(result.provider.value).toBe('AWS')
      expect(result.geoRegion.value).toBe('eu')
      expect(result.city.value).toBe('amsterdam')
      expect(result.country.value).toBe('nl')
    })
  })

  describe('mutation states', () => {
    it('exposes saveRegionMutation for tracking state', () => {
      const { result } = withSetup(() => useRegionForm())

      expect(result.saveRegionMutation).toBeDefined()
      expect(result.saveRegionMutation).toHaveProperty('mutateAsync')
      expect(result.saveRegionMutation).toHaveProperty('isPending')
      expect(result.saveRegionMutation).toHaveProperty('isError')
      expect(result.saveRegionMutation).toHaveProperty('isSuccess')
    })

    it('exposes deleteRegionMutation for tracking state', () => {
      const { result } = withSetup(() => useRegionForm())

      expect(result.deleteRegionMutation).toBeDefined()
      expect(result.deleteRegionMutation).toHaveProperty('mutateAsync')
      expect(result.deleteRegionMutation).toHaveProperty('isPending')
      expect(result.deleteRegionMutation).toHaveProperty('isError')
      expect(result.deleteRegionMutation).toHaveProperty('isSuccess')
    })
  })

  describe('engineRegions query', () => {
    it('fetches engine regions list when vesselId present', async () => {
      const mockRegions = [
        { attributes: { name: 'AMS1' } },
        { attributes: { name: 'LON1' } },
      ]

      vi.mocked(axios.get).mockResolvedValue(mockRegions)

      const { result } = withSetup(() => useRegionForm())

      await flushPromises()

      expect(axios.get).toHaveBeenCalledWith('/vessels/vessel1/engine/regions')
      expect(result.engineRegions.value).toEqual(mockRegions)
    })

    it('is disabled when no vesselId is available', async () => {
      mockRoute.params = {}
      vi.mocked(axios.get).mockResolvedValue([])

      const { result } = withSetup(() => useRegionForm())

      await flushPromises()

      // Query should not be enabled without vesselId
      expect(result.engineRegions.value).toBeUndefined()
    })
  })

  describe('integration tests', () => {
    it('auto-fills name and geoRegion when city and country are set', async () => {
      vi.mocked(axios.get).mockResolvedValue([])

      const { result } = withSetup(() => useRegionForm())

      result.city.value = 'amsterdam'
      result.country.value = 'nl'
      await flushPromises()

      expect(result.name.value).toBe('AMS1')
      expect(result.geoRegion.value).toBe('eu')
      expect(result.pristine.value).toBe(false)
    })

    it('handles complete form workflow', async () => {
      vi.mocked(axios.get).mockResolvedValue([])
      vi.mocked(axios.post).mockResolvedValue({ data: { id: 'new-region' } })

      const { result } = withSetup(() => useRegionForm())

      // Fill form
      result.city.value = 'london'
      result.country.value = 'gb'
      result.provider.value = 'AWS'
      await flushPromises()

      // Verify auto-filled values
      expect(result.name.value).toBe('LON1')
      expect(result.geoRegion.value).toBe('eu')

      // Save
      const response = await result.saveRegion()

      expect(axios.post).toHaveBeenCalledWith('/vessels/vessel1/engine/regions', {
        name: 'LON1',
        locationCity: 'london',
        locationCountry: 'gb',
        geoRegion: 'eu',
        providerName: 'AWS',
      })
      expect(response).toBeDefined()
    })
  })
})
