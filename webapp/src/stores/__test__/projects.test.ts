import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useProjectsStore } from '@/stores/projects'
import axios from 'axios'

vi.mock('axios')

describe('useProjectsStore', () => {
  let store: ReturnType<typeof useProjectsStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useProjectsStore()
    vi.clearAllMocks()
  })

  describe('state', () => {
    it('initializes with default values', () => {
      expect(store.charters).toEqual([])
      expect(store.selectedVesselId).toBeUndefined()
      expect(store.selectedCharterId).toBeUndefined()
      expect(store.selectedProjectId).toBeUndefined()
      expect(store._schemaVersion).toBe(1)
    })
  })

  describe('getters', () => {
    describe('chartersForDropdown', () => {
      it('returns empty array when no charters', () => {
        expect(store.chartersForDropdown).toEqual([])
      })

      it('transforms charters to dropdown format', () => {
        store.charters = [
          {
            id: 'charter1',
            vesselId: 'vessel1',
            name: 'First Charter',
            description: 'Description 1',
            projects: [],
          },
          {
            id: 'charter2',
            vesselId: 'vessel1',
            name: 'Second Charter',
            description: 'Description 2',
            projects: [],
          },
        ]

        expect(store.chartersForDropdown).toEqual([
          { label: 'First Charter', value: 'charter1' },
          { label: 'Second Charter', value: 'charter2' },
        ])
      })
    })

    describe('projectsForDropdown', () => {
      it('returns empty array when no charter is selected', () => {
        expect(store.projectsForDropdown).toEqual([])
      })

      it('returns empty array when selected charter has no projects', () => {
        store.charters = [
          {
            id: 'charter1',
            vesselId: 'vessel1',
            name: 'First Charter',
            description: 'Description',
            projects: [],
          },
        ]
        store.selectedCharterId = 'charter1'

        expect(store.projectsForDropdown).toEqual([])
      })

      it('transforms projects to dropdown format for selected charter', () => {
        store.charters = [
          {
            id: 'charter1',
            vesselId: 'vessel1',
            name: 'First Charter',
            description: 'Description',
            projects: [
              {
                id: 'project1',
                name: 'API Project',
                purpose: 'api',
                environment: 'production',
              },
              {
                id: 'project2',
                name: 'Web Project',
                purpose: 'webapp',
                environment: 'staging',
              },
            ],
          },
        ]
        store.selectedCharterId = 'charter1'

        expect(store.projectsForDropdown).toEqual([
          { label: 'API Project', value: 'project1' },
          { label: 'Web Project', value: 'project2' },
        ])
      })

      it('returns empty array when selected charter does not exist', () => {
        store.charters = [
          {
            id: 'charter1',
            vesselId: 'vessel1',
            name: 'First Charter',
            description: 'Description',
            projects: [],
          },
        ]
        store.selectedCharterId = 'nonexistent'

        expect(store.projectsForDropdown).toEqual([])
      })
    })
  })

  describe('fetchCharters', () => {
    it('does nothing when vesselIds array is empty', async () => {
      await store.fetchCharters([])

      expect(axios.get).not.toHaveBeenCalled()
      expect(store.charters).toEqual([])
    })

    it('fetches charters and projects for a single vessel', async () => {
      const mockCharters = [
        {
          id: 'charter1',
          attributes: {
            vesselId: 'vessel1',
            name: 'First Charter',
            description: 'Charter description',
          },
        },
      ]

      const mockProjects = [
        {
          id: 'project1',
          attributes: {
            name: 'Project A',
            purpose: 'api',
            environment: 'production',
          },
        },
      ]

      vi.mocked(axios.get)
        .mockResolvedValueOnce(mockCharters) // /vessels/vessel1/charters
        .mockResolvedValueOnce(mockProjects) // /vessels/vessel1/charters/charter1/projects

      await store.fetchCharters(['vessel1'])

      expect(axios.get).toHaveBeenCalledWith('/vessels/vessel1/charters')
      expect(axios.get).toHaveBeenCalledWith('/vessels/vessel1/charters/charter1/projects')

      expect(store.charters).toEqual([
        {
          id: 'charter1',
          vesselId: 'vessel1',
          name: 'First Charter',
          description: 'Charter description',
          projects: [
            {
              id: 'project1',
              name: 'Project A',
              purpose: 'api',
              environment: 'production',
            },
          ],
        },
      ])
    })

    it('fetches charters and projects for multiple vessels', async () => {
      const mockCharters1 = [
        {
          id: 'charter1',
          attributes: {
            vesselId: 'vessel1',
            name: 'Vessel 1 Charter',
            description: 'Description 1',
          },
        },
      ]

      const mockCharters2 = [
        {
          id: 'charter2',
          attributes: {
            vesselId: 'vessel2',
            name: 'Vessel 2 Charter',
            description: 'Description 2',
          },
        },
      ]

      const mockProjects1 = [
        {
          id: 'project1',
          attributes: {
            name: 'Project 1',
            purpose: 'api',
            environment: 'production',
          },
        },
      ]

      const mockProjects2 = [
        {
          id: 'project2',
          attributes: {
            name: 'Project 2',
            purpose: 'webapp',
            environment: 'staging',
          },
        },
      ]

      vi.mocked(axios.get)
        .mockResolvedValueOnce(mockCharters1) // vessel1 charters
        .mockResolvedValueOnce(mockProjects1) // charter1 projects
        .mockResolvedValueOnce(mockCharters2) // vessel2 charters
        .mockResolvedValueOnce(mockProjects2) // charter2 projects

      await store.fetchCharters(['vessel1', 'vessel2'])

      expect(axios.get).toHaveBeenCalledTimes(4)
      expect(store.charters).toHaveLength(2)
      expect(store.charters[0]?.vesselId).toBe('vessel1')
      expect(store.charters[1]?.vesselId).toBe('vessel2')
    })

    it('sets selected IDs after fetching', async () => {
      const mockCharters = [
        {
          id: 'charter1',
          attributes: {
            vesselId: 'vessel1',
            name: 'Charter',
            description: 'Description',
          },
        },
      ]

      const mockProjects = [
        {
          id: 'project1',
          attributes: {
            name: 'Project',
            purpose: 'api',
            environment: 'production',
          },
        },
      ]

      vi.mocked(axios.get)
        .mockResolvedValueOnce(mockCharters)
        .mockResolvedValueOnce(mockProjects)

      await store.fetchCharters(['vessel1'])

      expect(store.selectedVesselId).toBe('vessel1')
      expect(store.selectedCharterId).toBe('charter1')
      expect(store.selectedProjectId).toBe('project1')
    })

    it('handles vessels with multiple charters', async () => {
      const mockCharters = [
        {
          id: 'charter1',
          attributes: {
            vesselId: 'vessel1',
            name: 'Charter 1',
            description: 'Description 1',
          },
        },
        {
          id: 'charter2',
          attributes: {
            vesselId: 'vessel1',
            name: 'Charter 2',
            description: 'Description 2',
          },
        },
      ]

      const mockProjects1 = [
        {
          id: 'project1',
          attributes: {
            name: 'Project 1',
            purpose: 'api',
            environment: 'production',
          },
        },
      ]

      const mockProjects2 = [
        {
          id: 'project2',
          attributes: {
            name: 'Project 2',
            purpose: 'webapp',
            environment: 'staging',
          },
        },
      ]

      vi.mocked(axios.get)
        .mockResolvedValueOnce(mockCharters)
        .mockResolvedValueOnce(mockProjects1)
        .mockResolvedValueOnce(mockProjects2)

      await store.fetchCharters(['vessel1'])

      expect(store.charters).toHaveLength(2)
      expect(store.charters[0]?.projects).toHaveLength(1)
      expect(store.charters[1]?.projects).toHaveLength(1)
    })

    it('handles charters with no projects', async () => {
      const mockCharters = [
        {
          id: 'charter1',
          attributes: {
            vesselId: 'vessel1',
            name: 'Empty Charter',
            description: 'No projects',
          },
        },
      ]

      vi.mocked(axios.get)
        .mockResolvedValueOnce(mockCharters)
        .mockResolvedValueOnce([]) // empty projects array

      await store.fetchCharters(['vessel1'])

      expect(store.charters[0]?.projects).toEqual([])
      expect(store.selectedProjectId).toBeUndefined()
    })

    it('handles API errors gracefully', async () => {
      vi.mocked(axios.get).mockRejectedValue(new Error('Network error'))

      await expect(store.fetchCharters(['vessel1'])).rejects.toThrow('Network error')

      expect(store.charters).toEqual([])
    })
  })

  describe('state mutations', () => {
    it('allows manual selection changes', () => {
      store.selectedCharterId = 'charter1'
      store.selectedProjectId = 'project1'

      expect(store.selectedVesselId).toBe('vessel1')
      expect(store.selectedCharterId).toBe('charter1')
      expect(store.selectedProjectId).toBe('project1')
    })
  })

  describe('store reset', () => {
    it('resets all state to initial values', async () => {
      const mockCharters = [
        {
          id: 'charter1',
          attributes: {
            vesselId: 'vessel1',
            name: 'Charter',
            description: 'Description',
          },
        },
      ]

      vi.mocked(axios.get)
        .mockResolvedValueOnce(mockCharters)
        .mockResolvedValueOnce([])

      await store.fetchCharters(['vessel1'])

      store.$reset()

      expect(store.charters).toEqual([])
      expect(store.selectedVesselId).toBeUndefined()
      expect(store.selectedCharterId).toBeUndefined()
      expect(store.selectedProjectId).toBeUndefined()
    })
  })
})
