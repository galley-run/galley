import { defineStore } from 'pinia'
import axios from 'axios'

const STORAGE_KEY = 'license'

interface LicenseResponse {
  license?: string
  licensedTo?: string
  alert?: string
  scopes: string[]
}

export const useLicenseStore = defineStore('license', {
  state: (): {
    license?: string
    licensedTo?: string
    alert?: string
    scopes: string[]
    _schemaVersion: 1
  } => ({
    license: undefined,
    licensedTo: undefined,
    alert: undefined,
    scopes: [],
    _schemaVersion: 1,
  }),
  actions: {
    async fetchLicense() {
      const response = (await axios.get('/license')) as LicenseResponse

      this.license = response?.license
      this.licensedTo = response?.licensedTo
      this.alert = response?.alert
      this.scopes = response?.scopes
    },
  },
})

export function initLicenseStore() {
  const store = useLicenseStore()

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
          license: state.license,
          scopes: state.scopes,
          licensedTo: state.licensedTo,
          alert: state.alert,
          _schemaVersion: 1,
        }
        localStorage.setItem(STORAGE_KEY, JSON.stringify(toPersist))
      } catch {}
    },
    { detached: true },
  )
}
