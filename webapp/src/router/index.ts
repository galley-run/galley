import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/router/layouts/MainLayout.vue'
import DashboardView from '@/router/views/DashboardView.vue'
import ComputePlansView from '@/router/views/ComputePlansView.vue'
import EngineView from '@/router/views/EngineView.vue'
import OnboardingLayout from '@/router/layouts/OnboardingLayout.vue'
import RegistrationView from '@/router/views/Onboarding/RegistrationView.vue'
import SecurityScreeningView from '@/router/views/Onboarding/SecurityScreeningView.vue'
import NamingCeremonyView from '@/router/views/Onboarding/NamingCeremonyView.vue'
import FirstCharterView from '@/router/views/Onboarding/FirstCharterView.vue'

const routes = [
  {
    path: '/onboarding',
    component: OnboardingLayout,
    children: [
      { path: '', component: RegistrationView },
      { path: 'security-screening', component: SecurityScreeningView },
      { path: 'naming-ceremony', component: NamingCeremonyView },
      { path: 'first-charter', component: FirstCharterView },
    ],
  },
  {
    path: '/',
    component: MainLayout,
    children: [
      { path: '', component: DashboardView },
      { path: '/charter/compute-plans', component: ComputePlansView },
      { path: '/vessel/engine', component: EngineView },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

export default router
