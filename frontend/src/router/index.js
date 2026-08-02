import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('@/views/HomeView.vue'),
    meta: { title: '首页' },
  },
  {
    path: '/checkin',
    name: 'checkin',
    component: () => import('@/views/CheckinView.vue'),
    meta: { title: '每日打卡' },
  },
  {
    path: '/history',
    name: 'history',
    component: () => import('@/views/HistoryView.vue'),
    meta: { title: '历史记录' },
  },
  {
    path: '/trend',
    name: 'trend',
    component: () => import('@/views/TrendView.vue'),
    meta: { title: '趋势分析' },
  },
  {
    path: '/ai-chat',
    name: 'ai-chat',
    component: () => import('@/views/AiChatView.vue'),
    meta: { title: 'AI建议' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
