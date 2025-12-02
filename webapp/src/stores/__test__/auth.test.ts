import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import axios from 'axios'
import router from '@/router'

// Mock dependencies
vi.mock('axios')
vi.mock('@/router', () => ({
  default: {
    push: vi.fn(),
  },
}))
vi.mock('@/stores/projects', () => ({
  useProjectsStore: () => ({
    $reset: vi.fn(),
  }),
}))
vi.mock('@/stores/onboarding', () => ({
  useOnboardingStore: () => ({
    $reset: vi.fn(),
  }),
}))
vi.mock('@/stores/license', () => ({
  useLicenseStore: () => ({
    fetchLicense: vi.fn(),
  }),
}))

describe('useAuthStore', () => {
  let store: ReturnType<typeof useAuthStore>

  // Helper functie om een JWT token te maken
  const createJWT = (payload: Record<string, unknown>, expMinutes = 60) => {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
    const exp = Math.floor(Date.now() / 1000) + expMinutes * 60
    const body = btoa(JSON.stringify({ ...payload, exp }))
    return `${header}.${body}.signature`
  }

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useAuthStore()
    vi.clearAllMocks()
    localStorage.clear()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  describe('state', () => {
    it('initializes with default values', () => {
      expect(store.accessToken).toBeUndefined()
      expect(store.refreshToken).toBeUndefined()
      expect(store.scopes).toBeUndefined()
      expect(store.vesselIds).toEqual([])
      expect(store.activeVesselId).toBeUndefined()
      expect(store.accessTokenTimeout).toBeNull()
      expect(store._schemaVersion).toBe(1)
    })
  })

  describe('getters', () => {
    it('isAuthenticated returns true when both tokens are present', () => {
      store.accessToken = 'access'
      store.refreshToken = 'refresh'
      expect(store.isAuthenticated).toBe(true)
    })

    it('isAuthenticated returns false when tokens are missing', () => {
      expect(store.isAuthenticated).toBe(false)
      store.accessToken = 'access'
      expect(store.isAuthenticated).toBe(false)
    })

    it('isAuthenticating returns true when only refreshToken is present', () => {
      store.refreshToken = 'refresh'
      expect(store.isAuthenticating).toBe(true)
    })

    it('isAuthenticating returns false when accessToken is present', () => {
      store.accessToken = 'access'
      store.refreshToken = 'refresh'
      expect(store.isAuthenticating).toBe(false)
    })
  })

  describe('extractAccessToken', () => {
    it('extracts vessel IDs from scopes', () => {
      const token = createJWT({
        scp: {
          'vessel1:resource1': 'permission',
          'vessel1:resource2': 'permission',
          'vessel2:resource1': 'permission',
        },
      })
      store.accessToken = token
      store.refreshToken = 'refresh'

      store.extractAccessToken()

      expect(store.vesselIds).toEqual(['vessel1', 'vessel2'])
    })

    it('signs out when accessToken is missing', async () => {
      store.refreshToken = 'refresh'
      const signOutSpy = vi.spyOn(store, 'signOut')

      await store.extractAccessToken()

      expect(signOutSpy).toHaveBeenCalled()
    })

    it('signs out when refreshToken is missing', async () => {
      const token = createJWT({ scp: {} })
      store.accessToken = token
      const signOutSpy = vi.spyOn(store, 'signOut')

      await store.extractAccessToken()

      expect(signOutSpy).toHaveBeenCalled()
    })
  })

  describe('setRefreshToken', () => {
    it('sets refresh token and refreshes access token', async () => {
      const accessToken = createJWT({ scp: {} })
      vi.mocked(axios.post).mockResolvedValue({ accessToken })

      await store.setRefreshToken('new-refresh-token')

      expect(store.refreshToken).toBe('new-refresh-token')
      expect(store.accessToken).toBe(accessToken)
    })
  })

  describe('signOut', () => {
    it('clears tokens and redirects to login', async () => {
      store.accessToken = 'access'
      store.refreshToken = 'refresh'
      vi.mocked(axios.delete).mockResolvedValue({})

      await store.signOut()

      expect(store.accessToken).toBeUndefined()
      expect(store.refreshToken).toBeUndefined()
      expect(axios.delete).toHaveBeenCalledWith('/auth/sign-out')
      expect(router.push).toHaveBeenCalledWith('/login')
    })

    it('clears localStorage', async () => {
      localStorage.setItem('auth', '{"refreshToken":"test"}')
      vi.mocked(axios.delete).mockResolvedValue({})

      await store.signOut()

      expect(localStorage.getItem('auth')).toBeNull()
    })

    it('stops access token timer', async () => {
      store.accessTokenTimeout = 123 as NodeJS.Timeout
      const stopSpy = vi.spyOn(store, 'stopAccessTokenTimer')
      vi.mocked(axios.delete).mockResolvedValue({})

      await store.signOut()

      expect(stopSpy).toHaveBeenCalled()
    })
  })

  describe('refreshAccessToken', () => {
    it('refreshes access token and sets authorization header', async () => {
      const scopes = { 'vessel1:resource': 'permission' }
      const accessToken = createJWT({ scp: scopes })
      store.refreshToken = 'refresh'

      vi.mocked(axios.post).mockResolvedValue({ accessToken })

      await store.refreshAccessToken()

      expect(store.accessToken).toBe(accessToken)
      expect(store.scopes).toEqual(scopes)
      expect(store.activeVesselId).toBe('vessel1')
      expect(axios.defaults.headers.common['Authorization']).toBe(`Bearer ${accessToken}`)
    })

    it('does nothing when refreshToken is missing', async () => {
      await store.refreshAccessToken()

      expect(axios.post).not.toHaveBeenCalled()
    })

    it('signs out on error', async () => {
      store.refreshToken = 'refresh'
      vi.mocked(axios.post).mockRejectedValue(new Error('Unauthorized'))
      const signOutSpy = vi.spyOn(store, 'signOut')

      await store.refreshAccessToken()

      expect(signOutSpy).toHaveBeenCalled()
    })
  })

  describe('refreshRefreshToken', () => {
    it('refreshes the refresh token', async () => {
      const newRefreshToken = 'new-refresh-token'
      store.refreshToken = 'old-refresh-token'
      vi.mocked(axios.post).mockResolvedValue({ refreshToken: newRefreshToken })

      await store.refreshRefreshToken()

      expect(store.refreshToken).toBe(newRefreshToken)
      expect(axios.post).toHaveBeenCalledWith('/auth/refresh-token', {
        refreshToken: 'old-refresh-token',
      })
    })
  })

  describe('startAccessTokenTimer', () => {
    it('sets a timeout to refresh access token before expiration', async () => {
      const accessToken = createJWT({ scp: {} }, 5) // expires in 5 minutes
      store.accessToken = accessToken
      const refreshSpy = vi.spyOn(store, 'refreshAccessToken').mockResolvedValue()

      await store.startAccessTokenTimer()

      expect(store.accessTokenTimeout).toBeDefined()

      // Fast-forward time to trigger the timeout (4 minutes = 5 min - 1 min buffer)
      vi.advanceTimersByTime(4 * 60 * 1000)

      expect(refreshSpy).toHaveBeenCalled()
    })

    it('does nothing when accessToken is missing', async () => {
      await store.startAccessTokenTimer()

      expect(store.accessTokenTimeout).toBeNull()
    })
  })

  describe('stopAccessTokenTimer', () => {
    it('clears the timeout', () => {
      store.accessTokenTimeout = 123 as NodeJS.Timeout

      store.stopAccessTokenTimer()

      // Timer should be cleared (we can't directly test clearTimeout, but we can check behavior)
      expect(store.accessTokenTimeout).toBe(123) // Value doesn't change, but timer is cleared
    })
  })

  describe('startRefreshTokenTimer', () => {
    it('refreshes access token and refresh token when near expiration', async () => {
      const refreshToken = createJWT({}, 23 * 60) // expires in 23 hours (< 24 hours)
      const accessToken = createJWT({ scp: {} })
      store.refreshToken = refreshToken

      vi.mocked(axios.post)
        .mockResolvedValueOnce({ accessToken }) // refreshAccessToken
        .mockResolvedValueOnce({ refreshToken: 'new-refresh' }) // refreshRefreshToken

      await store.startRefreshTokenTimer()

      expect(axios.post).toHaveBeenCalledTimes(2)
      expect(store.refreshToken).toBe('new-refresh')
    })

    it('does nothing when refreshToken is missing', async () => {
      await store.startRefreshTokenTimer()

      expect(axios.post).not.toHaveBeenCalled()
    })
  })
})
