import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useOnboardingStore } from '@/stores/onboarding'

describe('useOnboardingStore', () => {
  let store: ReturnType<typeof useOnboardingStore>

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useOnboardingStore()
  })

  describe('state', () => {
    it('initializes with default intent', () => {
      expect(store.intent).toBe('business')
    })

    it('initializes user with empty values', () => {
      expect(store.user).toEqual({
        firstName: '',
        lastName: '',
        email: '',
      })
    })

    it('initializes inquiry with default values', () => {
      expect(store.inquiry).toEqual({
        technicalExperience: '',
        reference: 'search_engine',
        orgRole: '',
        orgIndustry: '',
        orgTeamSize: '1',
      })
    })

    it('initializes vesselBillingProfile with default country', () => {
      expect(store.vesselBillingProfile.country).toBe('nl')
      expect(store.vesselBillingProfile.companyName).toBe('')
      expect(store.vesselBillingProfile.billingTo).toBe('')
      expect(store.vesselBillingProfile.address1).toBe('')
      expect(store.vesselBillingProfile.address2).toBe('')
      expect(store.vesselBillingProfile.postalCode).toBe('')
      expect(store.vesselBillingProfile.city).toBe('')
      expect(store.vesselBillingProfile.state).toBe('')
      expect(store.vesselBillingProfile.vatNumber).toBe('')
      expect(store.vesselBillingProfile.email).toBe('')
    })

    it('initializes vessel with a generated name', () => {
      expect(store.vessel.name).toBeTruthy()
      expect(typeof store.vessel.name).toBe('string')
    })

    it('initializes charter with empty values', () => {
      expect(store.charter).toEqual({
        name: '',
        description: '',
      })
    })

    it('initializes project with default values', () => {
      expect(store.project).toEqual({
        name: '',
        purpose: 'webapp',
        environment: 'production',
      })
    })

    it('initializes completed with all false', () => {
      expect(store.completed).toEqual({
        checkIn: false,
        securityScreening: false,
        namingCeremony: false,
        firstCharter: false,
      })
    })
  })

  describe('state mutations', () => {
    it('allows updating intent', () => {
      store.intent = 'exploring'
      expect(store.intent).toBe('exploring')
    })

    it('allows updating user information', () => {
      store.user.firstName = 'John'
      store.user.lastName = 'Doe'
      store.user.email = 'john@example.com'

      expect(store.user).toEqual({
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
      })
    })

    it('allows updating inquiry information', () => {
      store.inquiry.technicalExperience = 'expert'
      store.inquiry.reference = 'friend'
      store.inquiry.orgRole = 'developer'
      store.inquiry.orgIndustry = 'tech'
      store.inquiry.orgTeamSize = '10-50'

      expect(store.inquiry).toEqual({
        technicalExperience: 'expert',
        reference: 'friend',
        orgRole: 'developer',
        orgIndustry: 'tech',
        orgTeamSize: '10-50',
      })
    })

    it('allows updating vessel billing profile', () => {
      store.vesselBillingProfile.companyName = 'Test Corp'
      store.vesselBillingProfile.country = 'us'
      store.vesselBillingProfile.email = 'billing@test.com'

      expect(store.vesselBillingProfile.companyName).toBe('Test Corp')
      expect(store.vesselBillingProfile.country).toBe('us')
      expect(store.vesselBillingProfile.email).toBe('billing@test.com')
    })

    it('allows updating vessel name', () => {
      store.vessel.name = 'My Custom Vessel'
      expect(store.vessel.name).toBe('My Custom Vessel')
    })

    it('allows updating charter information', () => {
      store.charter.name = 'First Charter'
      store.charter.description = 'Initial charter description'

      expect(store.charter).toEqual({
        name: 'First Charter',
        description: 'Initial charter description',
      })
    })

    it('allows updating project information', () => {
      store.project.name = 'My Project'
      store.project.purpose = 'api'
      store.project.environment = 'development'

      expect(store.project).toEqual({
        name: 'My Project',
        purpose: 'api',
        environment: 'development',
      })
    })

    it('allows updating completed steps', () => {
      store.completed.checkIn = true
      store.completed.securityScreening = true

      expect(store.completed.checkIn).toBe(true)
      expect(store.completed.securityScreening).toBe(true)
      expect(store.completed.namingCeremony).toBe(false)
      expect(store.completed.firstCharter).toBe(false)
    })
  })

  describe('store reset', () => {
    it('resets all state to initial values', () => {
      // Modify state
      store.intent = 'exploring'
      store.user.firstName = 'John'
      store.completed.checkIn = true

      // Reset
      store.$reset()

      // Verify reset
      expect(store.intent).toBe('business')
      expect(store.user.firstName).toBe('')
      expect(store.completed.checkIn).toBe(false)
    })
  })
})
