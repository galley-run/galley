import { defineStore } from 'pinia'
import axios from 'axios'

const STORAGE_KEY = 'projects'

interface Projects {
  id: string
  name: string
  purpose: string
  environment: string
}

interface ProjectResponse {
  id: string
  attributes: { name: string; purpose: string; environment: string }
}

interface Charter {
  id: string
  vesselId: string
  name: string
  description: string
  projects: Projects[]
}

interface CharterResponse {
  id: string
  attributes: { name: string; description: string; vesselId: string }
}

export const useProjectsStore = defineStore('projects', {
  state: (): {
    charters: Charter[]
    selectedVesselId?: string
    selectedCharterId?: string
    selectedProjectId?: string
    _schemaVersion: 1,
  } => ({
    charters: [],
    selectedVesselId: undefined,
    selectedCharterId: undefined,
    selectedProjectId: undefined,
    _schemaVersion: 1,
  }),
  getters: {
    chartersForDropdown: (state) => {
      return state.charters.map(charter => ({
        label: charter.name,
        value: charter.id
      }))
    },
    projectsForDropdown: (state) => {
      return state.charters.find(charter => charter.id === state.selectedCharterId)?.projects.map(project => ({
        label: project.name,
        value: project.id
      })) ?? []
    }
  },
  actions: {
    async fetchCharters(vesselIds: string[]) {
      if (vesselIds.length === 0) return
      const data = []
      for (const vesselId of vesselIds) {
        const charters = (await axios.get(`/vessels/${vesselId}/charters`)) as CharterResponse[]
        for (const charter of charters) {
          const projects = (await axios.get(`/vessels/${vesselId}/charters/${charter.id}/projects`)) as ProjectResponse[]
          data.push({
            id: charter.id,
            vesselId: charter.attributes.vesselId,
            name: charter.attributes.name,
            description: charter.attributes.description,
            projects: projects.map((project) => ({
              id: project.id,
              name: project.attributes.name,
              purpose: project.attributes.purpose,
              environment: project.attributes.environment,
            })),
          })
        }
      }
      this.charters = data
      this.selectedVesselId = data.length > 0 ? data[0]?.vesselId : undefined
      this.selectedCharterId = data.length > 0 ? data[0]?.id : undefined
      this.selectedProjectId = data.length > 0 ? data[0]?.projects?.[0]?.id : undefined
    },
  },
})

export function initProjectsStore() {
  const store = useProjectsStore()

  // restore
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw)
      // simpele migratie
      if (parsed._schemaVersion === 1) {
        store.$patch(parsed)
      }
    }
  } catch {}

  // persist
  store.$subscribe(
    (_mutation, state) => {
      try {
        // alleen velden bewaren die je nodig hebt
        const toPersist = {
          charters: state.charters,
          selectedVesselId: state.selectedVesselId,
          selectedCharterId: state.selectedCharterId,
          selectedProjectId: state.selectedProjectId,
          _schemaVersion: 1,
        }
        localStorage.setItem(STORAGE_KEY, JSON.stringify(toPersist))
      } catch {}
    },
    { detached: true },
  )
}
