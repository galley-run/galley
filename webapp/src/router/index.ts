import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/router/layouts/MainLayout.vue'
import DashboardView from '@/router/views/DashboardView.vue'
import ComputePlansView from '@/router/views/ComputePlansView.vue'
import EngineView from '@/router/views/EngineView.vue'

const routes = [
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
