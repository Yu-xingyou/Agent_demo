<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Home, CheckCircle2, History, TrendingUp, Sparkles } from 'lucide-vue-next'
import ParticleCanvas from '../components/ParticleCanvas.vue'

const route = useRoute()
const router = useRouter()

const menus = [
  { path: '/', title: '首页', icon: Home },
  { path: '/checkin', title: '每日打卡', icon: CheckCircle2 },
  { path: '/history', title: '历史记录', icon: History },
  { path: '/trend', title: '趋势分析', icon: TrendingUp },
  { path: '/ai-chat', title: 'AI建议', icon: Sparkles },
]

// 趋势页使用重粒子效果，其余页面轻量
const density = computed(() =>
  route.path === '/trend' ? 'heavy' : 'low'
)

function go(path) {
  router.push(path)
}
</script>

<template>
  <div class="relative min-h-screen flex flex-col">
    <ParticleCanvas :density="density" />

    <!-- 顶部流动渐变导航栏 -->
    <header
      class="fixed top-0 left-0 right-0 z-50 h-16 flex items-center px-6 text-white glass-nav"
    >
      <div class="flex items-center gap-2 text-lg font-semibold tracking-wide cursor-pointer" @click="go('/')">
        <span class="text-xl">🌼</span>
        <span>习惯记</span>
        <span class="text-xs font-normal opacity-70 ml-1 hidden sm:inline">生活习惯助手</span>
      </div>
      <nav class="ml-8 flex items-center gap-1 overflow-x-auto flex-1">
        <button
          v-for="m in menus"
          :key="m.path"
          class="relative flex items-center gap-1.5 px-3.5 py-2 rounded-lg text-sm transition-all duration-300 whitespace-nowrap"
          :class="route.path === m.path ? 'font-semibold text-white bg-white/15 nav-active' : 'text-white/75 hover:text-white hover:bg-white/10'"
          @click="go(m.path)"
        >
          <component :is="m.icon" :size="16" />
          {{ m.title }}
        </button>
      </nav>
    </header>

    <main class="pt-16 flex-1 relative z-10">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<style scoped>
.glass-nav {
  background: linear-gradient(135deg, rgba(111, 138, 130, 0.82), rgba(126, 136, 163, 0.82), rgba(143, 129, 150, 0.82));
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.16);
  box-shadow: 0 6px 24px rgba(126, 136, 163, 0.2);
}
.nav-active::after {
  content: '';
  position: absolute;
  left: 14%;
  right: 14%;
  bottom: 4px;
  height: 2px;
  border-radius: 999px;
  background: #fff;
  box-shadow: 0 0 8px #fff;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.35s ease, transform 0.35s ease;
}
.fade-enter-from {
  opacity: 0;
  transform: translateY(12px);
}
.fade-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}
</style>
