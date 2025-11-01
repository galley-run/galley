import { defineStore } from 'pinia'
import vesselNameGenerator from '@/utils/vesselNameGenerator.ts'

const vesselName = vesselNameGenerator()

export type Intent = 'business' | 'exploring'

export type Inquiry = {
  technicalExperience: string
  reference: string
  orgRole: string
  orgIndustry: string
  orgTeamSize: string
}

export type VesselBillingProfile = {
  companyName: string
  name: string
  address1: string
  address2: string
  country: string
  postalCode: string
  city: string
  state: string
  email: string
  vat: string
}

export type Vessel = {
  name: string
}

export type Charter = {
  name: string
  description: string
}

export type Project = {
  name: string
  purpose: string
  environment: string
}

export type User = {
  firstName: string
  lastName: string
  email: string
}

export type Completed = {
  checkIn: boolean
  securityScreening: boolean
  namingCeremony: boolean
  firstCharter: boolean
}

export const useOnboardingStore = defineStore('onboarding', {
  state: (): {
    intent: Intent
    user: User
    inquiry: Inquiry
    vesselBillingProfile: VesselBillingProfile
    vessel: Vessel
    charter: Charter
    project: Project
    completed: Completed
  } => ({
    intent: 'business',
    user: {
      firstName: '',
      lastName: '',
      email: '',
    },
    inquiry: {
      technicalExperience: '',
      reference: 'search_engine',
      orgRole: '',
      orgIndustry: '',
      orgTeamSize: '1',
    },
    vesselBillingProfile: {
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
      purpose: 'webapp',
      environment: 'production'
    },
    completed: {
      checkIn: false,
      securityScreening: false,
      namingCeremony: false,
      firstCharter: false
    }
  }),
})
