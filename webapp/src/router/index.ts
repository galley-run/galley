import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/router/layouts/MainLayout.vue'
import DashboardView from '@/router/views/DashboardView.vue'

const routes = [{ path: '/', component: MainLayout, children: [
    {path: '', component: DashboardView}
  ] }]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

export default router
