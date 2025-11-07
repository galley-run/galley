import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useLicenseStore } from '@/stores/license'
import axios from 'axios'

vi.mock('axios')

describe('useLicenseStore', () => {
  let store: ReturnType<typeof useLicenseStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useLicenseStore()
    vi.clearAllMocks()
  })

  describe('state', () => {
    it('initializes with default values', () => {
      expect(store.license).toBeUndefined()
      expect(store.licensedTo).toBeUndefined()
      expect(store.alert).toBeUndefined()
      expect(store.scopes).toEqual([])
      expect(store._schemaVersion).toBe(1)
    })
  })

  describe('fetchLicense', () => {
    it('fetches and sets license information', async () => {
      const mockLicense = {
        license: 'enterprise',
        licensedTo: 'Test Company',
        alert: undefined,
        scopes: ['vessel:read', 'vessel:write'],
      }

      vi.mocked(axios.get).mockResolvedValue(mockLicense)

      await store.fetchLicense()

      expect(axios.get).toHaveBeenCalledWith('/license')
      expect(store.license).toBe('enterprise')
      expect(store.licensedTo).toBe('Test Company')
      expect(store.alert).toBeUndefined()
      expect(store.scopes).toEqual(['vessel:read', 'vessel:write'])
    })

    it('handles license with alert', async () => {
      const mockLicense = {
        license: 'trial',
        licensedTo: 'Trial User',
        alert: 'Your trial expires in 7 days',
        scopes: ['vessel:read'],
      }

      vi.mocked(axios.get).mockResolvedValue(mockLicense)

      await store.fetchLicense()

      expect(store.license).toBe('trial')
      expect(store.licensedTo).toBe('Trial User')
      expect(store.alert).toBe('Your trial expires in 7 days')
      expect(store.scopes).toEqual(['vessel:read'])
    })

    it('handles license without optional fields', async () => {
      const mockLicense = {
        license: undefined,
        licensedTo: undefined,
        alert: undefined,
        scopes: [],
      }

      vi.mocked(axios.get).mockResolvedValue(mockLicense)

      await store.fetchLicense()

      expect(store.license).toBeUndefined()
      expect(store.licensedTo).toBeUndefined()
      expect(store.alert).toBeUndefined()
      expect(store.scopes).toEqual([])
    })

    it('handles API errors', async () => {
      vi.mocked(axios.get).mockRejectedValue(new Error('Network error'))

      await expect(store.fetchLicense()).rejects.toThrow('Network error')

      // Store should remain in its previous state
      expect(store.license).toBeUndefined()
      expect(store.licensedTo).toBeUndefined()
    })

    it('updates existing license information', async () => {
      // Set initial license
      const initialLicense = {
        license: 'trial',
        licensedTo: 'Initial User',
        alert: 'Trial ending soon',
        scopes: ['vessel:read'],
      }
      vi.mocked(axios.get).mockResolvedValue(initialLicense)
      await store.fetchLicense()

      // Update to new license
      const updatedLicense = {
        license: 'enterprise',
        licensedTo: 'Upgraded User',
        alert: undefined,
        scopes: ['vessel:read', 'vessel:write', 'admin:all'],
      }
      vi.mocked(axios.get).mockResolvedValue(updatedLicense)
      await store.fetchLicense()

      expect(store.license).toBe('enterprise')
      expect(store.licensedTo).toBe('Upgraded User')
      expect(store.alert).toBeUndefined()
      expect(store.scopes).toEqual(['vessel:read', 'vessel:write', 'admin:all'])
    })
  })

  describe('state mutations', () => {
    it('allows manual state updates', () => {
      store.license = 'custom'
      store.licensedTo = 'Manual User'
      store.alert = 'Custom alert'
      store.scopes = ['custom:scope']

      expect(store.license).toBe('custom')
      expect(store.licensedTo).toBe('Manual User')
      expect(store.alert).toBe('Custom alert')
      expect(store.scopes).toEqual(['custom:scope'])
    })
  })

  describe('store reset', () => {
    it('resets all state to initial values', async () => {
      const mockLicense = {
        license: 'enterprise',
        licensedTo: 'Test Company',
        alert: 'Some alert',
        scopes: ['vessel:read', 'vessel:write'],
      }
      vi.mocked(axios.get).mockResolvedValue(mockLicense)
      await store.fetchLicense()

      store.$reset()

      expect(store.license).toBeUndefined()
      expect(store.licensedTo).toBeUndefined()
      expect(store.alert).toBeUndefined()
      expect(store.scopes).toEqual([])
    })
  })
})
