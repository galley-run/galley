import { defineStore } from 'pinia'
import axios from 'axios'
import router from '@/router'

const STORAGE_KEY = 'auth'

export const useAuthStore = defineStore('auth', {
  state: (): {
    accessToken?: string
    refreshToken?: string
    scopes?: {
      [key: string]: string
    }
    vesselIds?: string[]
    activeVesselId?: string
    accessTokenTimeout: number | null
    _schemaVersion: 1
  } => ({
    accessToken: undefined,
    refreshToken: undefined,
    scopes: undefined,
    activeVesselId: undefined,
    accessTokenTimeout: null,
    _schemaVersion: 1,
  }),
  actions: {
    async signIn(email: string) {
      this.refreshToken = (await axios.post('/auth/sign-in', { email })).data.data.refreshToken

      await this.refreshAccessToken()
    },
    async setRefreshToken(refreshToken: string) {
      this.refreshToken = refreshToken

      await this.refreshAccessToken()
    },
    async signOut() {
      try {
        await axios.delete(`/auth/sign-out`)
      } catch {}

      this.$reset()
      this.stopAccessTokenTimer()

      try {
        localStorage.removeItem(STORAGE_KEY)
      } catch {}

      await router.push('/login')
    },
    async refreshAccessToken() {
      if (!this.refreshToken) return

      try {
        this.accessToken = (
          await axios.post('/auth/access-token', { refreshToken: this.refreshToken })
        ).data.data.accessToken
      } catch (_) {
        return this.signOut()
      }
      const jwtBase64 = this.accessToken?.split('.')[1] ?? ''
      if (!this.accessToken || !jwtBase64) {
        return this.signOut()
      }
      const payload = JSON.parse(atob(jwtBase64))
      this.scopes = payload?.scp
      this.activeVesselId = Object.keys(this.scopes ?? {}).find((key) => key.split(':')[0])

      axios.defaults.headers.common['Authorization'] = `Bearer ${this.accessToken}`

      await this.startAccessTokenTimer()
    },
    async refreshRefreshToken() {
      this.refreshToken = (
        await axios.post('/auth/refresh-token', { refreshToken: this.refreshToken })
      ).data.data.refreshToken
    },
    async startRefreshTokenTimer() {
      if (!this.refreshToken) return

      await this.refreshAccessToken()
      // parse json object from base64 encoded jwt token
      const jwtBase64 = this.refreshToken?.split('.')[1] ?? ''
      if (!this.refreshToken || !jwtBase64) {
        return this.signOut()
      }
      const jwtToken = JSON.parse(atob(jwtBase64))

      // set a timeout to refresh the token a minute before it expires
      const expires = new Date(jwtToken.exp * 1000)
      const timeout = expires.getTime() - Date.now() - 60 * 1000

      if (timeout < 86400000) {
        await this.refreshRefreshToken()
      }
    },
    async startAccessTokenTimer() {
      // parse json object from base64 encoded jwt token
      const jwtBase64 = this.accessToken?.split('.')[1] ?? ''
      if (!this.accessToken || !jwtBase64) {
        return
      }
      const jwtToken = JSON.parse(atob(jwtBase64))

      // set a timeout to refresh the token a minute before it expires
      const expires = new Date(jwtToken.exp * 1000)
      const timeout = expires.getTime() - Date.now() - 60 * 1000
      this.accessTokenTimeout = setTimeout(() => {
        this.refreshAccessToken()
      }, timeout)
    },
    stopAccessTokenTimer() {
      if (this.accessTokenTimeout) clearTimeout(this.accessTokenTimeout)
    },
  },
})

export function initAuthStore() {
  const store = useAuthStore()

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

  store.refreshAccessToken().then(async () => {
    await store.startRefreshTokenTimer()
  })

  // persist
  store.$subscribe(
    (_mutation, state) => {
      try {
        // alleen velden bewaren die je nodig hebt
        const toPersist = {
          refreshToken: state.refreshToken,
          scopes: state.scopes,
          vesselIds: state.vesselIds,
          activeVesselId: state.activeVesselId,
          _schemaVersion: 1,
        }
        localStorage.setItem(STORAGE_KEY, JSON.stringify(toPersist))
      } catch {}
    },
    { detached: true },
  )
}
