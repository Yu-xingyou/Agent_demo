<script setup>
import { useRouter } from 'vue-router'
import { Home } from 'lucide-vue-next'

const router = useRouter()
</script>

<template>
  <div class="min-h-screen flex items-center justify-center px-6 relative">
    <div class="glass rounded-card-xl p-10 max-w-md w-full text-center">
      <div class="text-6xl mb-4">🧭</div>
      <h1 class="text-2xl font-semibold text-slate-800 mb-2">页面走丢了</h1>
      <p class="text-sm text-slate-500 mb-6">你访问的页面不存在或已被移动。</p>
      <button
        class="px-5 py-2.5 rounded-lg text-sm text-white bg-grad-primary flex items-center gap-1.5 mx-auto hover:opacity-90 transition-opacity"
        @click="router.push('/')"
      >
        <Home :size="16" /> 返回首页
      </button>
    </div>
  </div>
</template>
