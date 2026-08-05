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
    path: '/ai-advice',
    name: 'ai-advice',
    component: () => import('@/views/AiAdviceView.vue'),
    meta: { title: 'AI建议' },
  },
  {
    path: '/ai-chat',
    name: 'ai-chat',
    component: () => import('@/views/AiAdviceView.vue'),
    meta: { title: 'AI建议' },
  },
  // 分析报告已整合进「AI 建议」页，旧路由重定向至整合页
  {
    path: '/ai-analysis',
    redirect: '/ai-advice',
  },
  {
    path: '/reminder',
    name: 'reminder',
    component: () => import('@/views/ReminderView.vue'),
    meta: { title: '打卡提醒' },
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
