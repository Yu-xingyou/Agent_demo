<script setup>
import { TrendingUp, Moon, Dumbbell, Droplets, Target } from 'lucide-vue-next'

const panels = [
  { key: 'sleep', title: '睡眠趋势', icon: Moon, desc: '入睡时长与睡眠质量随时间的波动，帮助发现作息规律。' },
  { key: 'exercise', title: '运动趋势', icon: Dumbbell, desc: '每周运动时长累计，追踪你的活力曲线。' },
  { key: 'water', title: '饮水趋势', icon: Droplets, desc: '每日饮水量对比推荐值，养成补水习惯。' },
  { key: 'radar', title: '综合能力', icon: Target, desc: '睡眠 / 运动 / 饮水 / 心情 / 饮食五维雷达，呈现整体健康画像。' },
]
</script>

<template>
  <div class="max-w-6xl mx-auto px-5 py-6">
    <div class="flex items-center gap-2 mb-1">
      <TrendingUp class="text-teal-700" />
      <h1 class="text-2xl font-semibold text-slate-800">趋势分析</h1>
    </div>
    <p class="text-sm text-slate-500 mb-6">
      数据可视化由阶段九统计接口驱动，当前为图表容器占位，后端分析 API 接入后将自动渲染。
    </p>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div v-for="p in panels" :key="p.key" class="panel-card p-5">
        <div class="flex items-center gap-2 mb-3 text-slate-700 font-medium">
          <component :is="p.icon" class="text-teal-700" /> {{ p.title }}
        </div>
        <div class="h-56 rounded-xl bg-slate-50 border border-dashed border-slate-200 flex items-center justify-center text-slate-400 text-sm">
          图表容器（id: chart-{{ p.key }}）· 待接口接入
        </div>
        <p class="text-xs text-slate-500 mt-3 leading-relaxed">{{ p.desc }}</p>
      </div>
    </div>
  </div>
</template>
