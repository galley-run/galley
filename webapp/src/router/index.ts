import {createRouter, createWebHistory} from 'vue-router'
import MainLayout from '@/router/layouts/MainLayout.vue'
import DashboardView from '@/router/views/DashboardView.vue'
import ComputePlansView from '@/router/views/ComputePlansView.vue'
import EngineView from '@/router/views/Engine/EngineView.vue'
import OnboardingLayout from '@/router/layouts/OnboardingLayout.vue'
import RegistrationView from '@/router/views/Onboarding/RegistrationView.vue'
import SecurityScreeningView from '@/router/views/Onboarding/SecurityScreeningView.vue'
import NamingCeremonyView from '@/router/views/Onboarding/NamingCeremonyView.vue'
import FirstCharterView from '@/router/views/Onboarding/FirstCharterView.vue'
import BoardingView from '@/router/views/Onboarding/BoardingView.vue'
import AccountActivationView from '@/router/views/Onboarding/AccountActivationView.vue'
import {useAuthStore} from '@/stores/auth.ts'
import LoginLayout from '@/router/layouts/LoginLayout.vue'
import LoginView from '@/router/views/Login/LoginView.vue'
import LogoutView from '@/router/views/Login/LogoutView.vue'
import EngineNodeAddView from '@/router/views/Engine/Node/EngineNodeAddView.vue'
import EngineNodeFirstControllerView from '@/router/views/Engine/Node/EngineNodeFirstControllerView.vue'

const routes = [
  {
    path: '/auth',
    component: LoginLayout,
    children: [
      { path: 'login', component: LoginView, meta: { public: true }, alias: "/login" },
      { path: 'logout', component: LogoutView, meta: { public: true }, alias: "/logout" },
      {
        path: 'activate/:hash',
        component: AccountActivationView,
        meta: { public: true },
      },
    ],
  },

  {
    path: '/onboarding',
    component: OnboardingLayout,
    children: [
      { path: '', component: RegistrationView, meta: { public: true } },
      { path: 'security-screening', component: SecurityScreeningView, meta: { public: true } },
      { path: 'naming-ceremony', component: NamingCeremonyView, meta: { public: true } },
      { path: 'first-charter', component: FirstCharterView, meta: { public: true } },
      { path: 'boarding', component: BoardingView, meta: { public: true } },
    ],
  },
  {
    path: '/',
    component: MainLayout,
    children: [
      { path: '', component: DashboardView },
      { path: '/charter/compute-plans', component: ComputePlansView },
      { path: '/vessel/engine', component: EngineView },
      { path: '/vessel/engine/node/controller', component: EngineNodeFirstControllerView },
      { path: '/vessel/engine/node/add', component: EngineNodeAddView },
      { path: '/vessel/engine/node/:nodeId', component: EngineNodeAddView },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach(async (to) => {
  // redirect to login page if not logged in and trying to access a restricted page
  const authRequired = !to.meta.public
  const authStore = useAuthStore();

  if (authRequired && !authStore.refreshToken) {
    return {
      path: '/login',
      query: {returnUrl: to.fullPath}
    };
  }
});

export default router
