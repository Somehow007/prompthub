import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/Home.vue'),
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/Login.vue'),
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/Register.vue'),
    },
    {
      path: '/template/create',
      name: 'template-create',
      component: () => import('@/views/TemplateCreate.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/template/:id',
      name: 'template-detail',
      component: () => import('@/views/TemplateDetail.vue'),
    },
    {
      path: '/template/:id/edit',
      name: 'template-edit',
      component: () => import('@/views/TemplateCreate.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/template/:id/history',
      name: 'template-history',
      component: () => import('@/views/TemplateHistory.vue'),
    },
    {
      path: '/profile/:id',
      name: 'profile',
      component: () => import('@/views/Profile.vue'),
    },
    {
      path: '/favorites',
      name: 'favorites',
      component: () => import('@/views/Favorites.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('@/views/Dashboard.vue'),
    },
    {
      path: '/activity',
      name: 'activity',
      component: () => import('@/views/Activity.vue'),
    },
    {
      path: '/admin/activities',
      name: 'admin-activities',
      component: () => import('@/views/ActivityManage.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

export default router
