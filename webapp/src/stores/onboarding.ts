import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useOnboardingStore = defineStore('onboarding', () => {
  const setupMode = ref<'setup'|'explore'>('setup')
  const user = ref({
    firstName: '',
    lastName: '',
    email: '',
    experience: '',
  })
  const questionnaire = ref({
    find: '',
    role: '',
    industry: '',
    users: '',
  })

  const billing = ref({
    companyName: '',
    companyAddress: '',
    companyCountry: '',
    companyZip: '',
    companyCity: '',
    companyAddress1: '',
    companyAddress2: '',
    companyVat: '',
    email: '',
  })

  const vessel = ref({
    name: '',
  })

  const charter = ref({
    name: '',
    description: '',
  })

  const project = ref({
    name: '',
    environment: '',
    purpose: '',
  })

  return {
    setupMode,
    user,
    questionnaire,
    billing,
    vessel,
    charter,
    project
  }
})
