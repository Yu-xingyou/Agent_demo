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
    path: '/ai-analysis',
    name: 'ai-analysis',
    component: () => import('@/views/AiAnalysisView.vue'),
    meta: { title: 'AI分析' },
  },
  {
    path: '/reminder',
    name: 'reminder',
    component: () => import('@/views/ReminderView.vue'),
    meta: { title: '打卡提醒' },
  },
  {
    path: '/ai-chat',
    name: 'ai-chat',
    component: () => import('@/views/AiChatView.vue'),
    meta: { title: 'AI建议' },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: '页面不存在' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 同步文档标题，提升标签页体验与可访问性
router.afterEach((to) => {
  const base = '生活习惯助手'
  document.title = to.meta?.title ? `${to.meta.title} · ${base}` : base
})

export default router
