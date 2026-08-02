<script setup>
import { useRoute, useRouter } from 'vue-router'
import { Home, CheckCircle2, History, TrendingUp, Sparkles } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()

const menus = [
  { path: '/', title: '首页', icon: Home },
  { path: '/checkin', title: '每日打卡', icon: CheckCircle2 },
  { path: '/history', title: '历史记录', icon: History },
  { path: '/trend', title: '趋势分析', icon: TrendingUp },
  { path: '/ai-chat', title: 'AI建议', icon: Sparkles },
]

function go(path) {
  router.push(path)
}
</script>

<template>
  <div class="min-h-screen flex flex-col">
    <!-- 顶部渐变导航栏（还原原习惯记风格） -->
    <header
      class="fixed top-0 left-0 right-0 z-50 h-16 flex items-center px-6 text-white"
      style="background: linear-gradient(135deg, #0f766e 0%, #134e4a 100%)"
    >
      <div class="flex items-center gap-2 text-lg font-semibold tracking-wide cursor-pointer" @click="go('/')">
        <span class="text-xl">🌼</span>
        <span>习惯记</span>
        <span class="text-xs font-normal opacity-70 ml-1">生活习惯助手</span>
      </div>
      <nav class="ml-10 flex items-center gap-1">
        <button
          v-for="m in menus"
          :key="m.path"
          class="flex items-center gap-1.5 px-3.5 py-2 rounded-lg text-sm transition-colors duration-200"
          :class="route.path === m.path ? 'font-semibold text-amber-500 bg-white/10' : 'text-white/85 hover:text-amber-500'"
          @click="go(m.path)"
        >
          <component :is="m.icon" :size="16" />
          {{ m.title }}
        </button>
      </nav>
    </header>

    <!-- 内容区 -->
    <main class="pt-16 flex-1">
      <router-view />
    </main>
  </div>
</template>
