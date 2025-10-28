import { defineStore } from 'pinia'
import vesselNameGenerator from '@/utils/vesselNameGenerator.ts'

const vesselName = vesselNameGenerator()

export const useOnboardingStore = defineStore('onboarding', {
  state: () => ({
    setupMode: 'setup',
    user: {
      firstName: '',
      lastName: '',
      email: '',
      experience: '',
    },
    questionnaire: {
      find: 'searchengine',
      role: 'cto',
      industry: '',
      users: '1',
    },
    billing: {
      companyName: '',
      name: '',
      address1: '',
      address2: '',
      country: 'nl',
      postalCode: '',
      city: '',
      state: '',
      vat: '',
      email: '',
    },
    vessel: {
      name: vesselName,
    },
    charter: {
      name: '',
      description: '',
    },
    project: {
      name: '',
    },
  }),
})
