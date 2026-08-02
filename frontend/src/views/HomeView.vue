<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  Flower2, Moon, Droplets, Dumbbell, Smile, Sparkles, ArrowRight, TrendingUp,
  Target, ChevronRight, Plus,
} from 'lucide-vue-next'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, LineChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import * as habitApi from '@/api/habit'
import * as goalApi from '@/api/goal'
import * as analysisApi from '@/api/analysis'
import { goalColor, MOOD_COLORS, MOOD_LABELS, CHART_PALETTE } from '@/constants/theme'
import { ElMessage } from 'element-plus'

use([CanvasRenderer, PieChart, LineChart, TooltipComponent, LegendComponent, GridComponent])

const router = useRouter()
const today = ref(null)
const recent = ref([])
const goals = ref([])
const overview = ref(null)
const trend = ref(null)
const loading = ref(false)

const cards = computed(() => {
  const t = today.value || {}
  const o = overview.value || {}
  return [
    {
      key: 'sleep', title: '睡眠', icon: Moon, grad: 'bg-grad-sleep',
      value: t.sleepDuration != null ? `${t.sleepDuration}h` : (o.avgSleep ? `${o.avgSleep}h` : '未打卡'),
      sub: '平均 ' + (o.avgSleep ?? '-') + 'h / 目标 8h',
    },
    {
      key: 'water', title: '饮水', icon: Droplets, grad: 'bg-grad-water',
      value: t.waterIntake != null ? `${t.waterIntake}ml` : (o.avgWater ? `${o.avgWater}ml` : '未打卡'),
      sub: '平均 ' + (o.avgWater ?? '-') + 'ml / 目标 2000ml',
    },
    {
      key: 'exercise', title: '运动', icon: Dumbbell, grad: 'bg-grad-exercise',
      value: t.exerciseDuration != null ? `${t.exerciseDuration}min` : (o.avgExercise ? `${o.avgExercise}min` : '未打卡'),
      sub: '平均 ' + (o.avgExercise ?? '-') + 'min / 目标 60min',
    },
    {
      key: 'mood', title: '心情', icon: Smile, grad: 'bg-grad-mood',
      value: t.mood ? MOOD_LABELS[t.mood] : (o.avgMood ? `${o.avgMood}/5` : '未打卡'),
      sub: '平均 ' + (o.avgMood ?? '-') + '/5',
    },
  ]
})

// 本周心情饼图（基于最近 7 天记录）
const moodOption = computed(() => {
  const counts = {}
  recent.value.forEach((r) => {
    if (r.mood) counts[r.mood] = (counts[r.mood] || 0) + 1
  })
  const data = Object.keys(MOOD_COLORS).map((k) => ({
    name: MOOD_LABELS[k],
    value: counts[k] || 0,
    itemStyle: { color: MOOD_COLORS[k] },
  }))
  return {
    tooltip: { trigger: 'item', backgroundColor: 'rgba(15,23,42,0.85)', borderWidth: 0, textStyle: { color: '#fff' } },
    legend: { bottom: 0, textStyle: { color: '#64748b' } },
    series: [{
      type: 'pie',
      radius: ['48%', '72%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 8, borderColor: 'rgba(255,255,255,0.7)', borderWidth: 2 },
      label: { show: false },
      data,
    }],
  }
})

// 近 7 天趋势折线（真实 analysis/trends）
const trendOption = computed(() => {
  const tr = trend.value || { dates: [], sleep: [], exercise: [], water: [] }
  return {
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(15,23,42,0.85)', borderWidth: 0, textStyle: { color: '#fff' } },
    legend: { data: ['睡眠(h)', '运动(min)', '饮水(ml)'], textStyle: { color: '#64748b' }, top: 0 },
    grid: { left: 36, right: 12, top: 36, bottom: 24 },
    xAxis: { type: 'category', data: tr.dates, axisLine: { lineStyle: { color: '#cbd5e1' } }, axisLabel: { color: '#94a3b8' } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: 'rgba(148,163,184,0.2)' } }, axisLabel: { color: '#94a3b8' } },
    series: [
      { name: '睡眠(h)', type: 'line', smooth: true, data: tr.sleep, itemStyle: { color: '#6366f1' }, areaStyle: { color: 'rgba(99,102,241,0.12)' } },
      { name: '运动(min)', type: 'line', smooth: true, data: tr.exercise, itemStyle: { color: '#f59e0b' }, areaStyle: { color: 'rgba(245,158,11,0.1)' } },
      { name: '饮水(ml)', type: 'line', smooth: true, data: tr.water, itemStyle: { color: '#06b6d4' }, areaStyle: { color: 'rgba(6,182,212,0.1)' } },
    ],
  }
})

// 任务目标进度（基于 HabitGoalVO，按类型配色）
const goalCards = computed(() => goals.value.map((g) => {
  const c = goalColor(g.goalType)
  const progress = g.targetValue && g.currentValue
    ? Math.min(100, Math.round((Number(g.currentValue) / Number(g.targetValue)) * 100)) : 0
  return { ...g, color: c, progress }
}))

const heroSlides = [
  { title: '今日打卡', desc: '坚持记录，遇见更好的自己', icon: Flower2, to: '/checkin', grad: 'bg-grad-primary' },
  { title: '每日一语', desc: '种一棵树最好的时间是十年前，其次是现在', icon: Sparkles, to: '/ai-chat', grad: 'bg-grad-mood' },
  { title: '目标达成', desc: '看看你的任务目标养成进度', icon: Target, to: '/checkin', grad: 'bg-grad-custom' },
]
const heroIndex = ref(0)
const heroIcon = computed(() => heroSlides[heroIndex.value].icon)
const heroGrad = computed(() => heroSlides[heroIndex.value].grad)
let timer = null
onMounted(() => {
  timer = setInterval(() => { heroIndex.value = (heroIndex.value + 1) % heroSlides.length }, 4500)
  load()
})
import { onBeforeUnmount } from 'vue'
onBeforeUnmount(() => timer && clearInterval(timer))

// 横向滑动卡片（习惯贴士）
const tips = [
  { title: '早睡早起', desc: '规律作息让第二天的精力更充沛', icon: Moon, grad: 'bg-grad-sleep' },
  { title: '足量饮水', desc: '每天 2000ml，唤醒身体代谢力', icon: Droplets, grad: 'bg-grad-water' },
  { title: '适度运动', desc: '每周 150 分钟中等强度刚刚好', icon: Dumbbell, grad: 'bg-grad-exercise' },
  { title: '均衡饮食', desc: '五彩蔬果，给身体温柔的养分', icon: Flower2, grad: 'bg-grad-diet' },
  { title: '记录心情', desc: '关注情绪，是爱自己的开始', icon: Smile, grad: 'bg-grad-mood' },
]

async function load() {
  loading.value = true
  try {
    const [t, r, g, ov, tr] = await Promise.all([
      habitApi.getToday().catch(() => null),
      habitApi.getRecent(7),
      goalApi.getActiveWithCustom().catch(() => []),
      analysisApi.getOverview(7).catch(() => null),
      analysisApi.getTrends(7).catch(() => null),
    ])
    today.value = t
    recent.value = r || []
    goals.value = g || []
    overview.value = ov
    trend.value = tr
  } catch (e) {
    ElMessage.error('加载首页数据失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="max-w-6xl mx-auto px-5 py-6 relative z-10">
    <!-- Hero 流动渐变轮播（背景随轮换切换，无摇摆，仅定时轮换） -->
    <section
      class="rounded-card-xl gradient-border glass-strong p-7 mb-6 text-white flex items-center gap-5 cursor-pointer transition-all duration-700"
      :class="heroGrad"
      @click="router.push(heroSlides[heroIndex].to)"
    >
      <div class="h-14 w-14 rounded-2xl bg-white/20 flex items-center justify-center backdrop-blur">
        <component :is="heroIcon" :size="28" />
      </div>
      <div class="flex-1">
        <div class="text-xs opacity-70 mb-1 tracking-widest">HABIT AGENT</div>
        <h2 class="text-2xl font-bold">{{ heroSlides[heroIndex].title }}</h2>
        <p class="opacity-90 text-sm mt-1">{{ heroSlides[heroIndex].desc }}</p>
      </div>
      <ArrowRight class="opacity-70" />
    </section>

    <!-- 4 张渐变统计卡 -->
    <section class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
      <div
        v-for="(c, i) in cards"
        :key="c.key"
        class="stat-card glass rounded-card-xl p-4 flex flex-col justify-between cursor-pointer animate-rise"
        :style="{ animationDelay: i * 80 + 'ms' }"
        @click="router.push('/checkin')"
      >
        <div class="flex items-center justify-between">
          <span class="text-sm text-slate-500">{{ c.title }}</span>
          <div class="h-9 w-9 rounded-xl flex items-center justify-center text-white shadow" :class="c.grad">
            <component :is="c.icon" :size="18" />
          </div>
        </div>
        <div class="mt-3">
          <div class="text-2xl font-bold text-slate-800">{{ c.value }}</div>
          <div class="text-xs text-slate-400 mt-1">{{ c.sub }}</div>
        </div>
      </div>
    </section>

    <!-- 饼图 + 折线趋势 -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
      <div class="glass rounded-card-xl p-5 lg:col-span-1">
        <h3 class="font-semibold mb-2 text-slate-700">本周心情分布</h3>
        <VChart :option="moodOption" autoresize style="height: 250px" />
      </div>
      <div class="glass rounded-card-xl p-5 lg:col-span-2">
        <div class="flex items-center justify-between mb-2">
          <h3 class="font-semibold text-slate-700">近 7 天趋势</h3>
          <button class="text-brand-indigo text-sm hover:text-brand-purple flex items-center" @click="router.push('/trend')">
            详细分析 <ChevronRight :size="14" />
          </button>
        </div>
        <VChart :option="trendOption" autoresize style="height: 250px" />
      </div>
    </div>

    <!-- 任务目标进度区（预留空间 + 按类型配色） -->
    <section class="glass rounded-card-xl p-5 mb-6">
      <div class="flex items-center justify-between mb-4">
        <div class="flex items-center gap-2">
          <div class="h-8 w-8 rounded-lg bg-grad-primary flex items-center justify-center text-white">
            <Target :size="16" />
          </div>
          <h3 class="font-semibold text-slate-700">任务目标</h3>
        </div>
        <button class="text-brand-indigo text-sm hover:text-brand-purple flex items-center" @click="router.push('/checkin')">
          去打卡设定 <ChevronRight :size="14" />
        </button>
      </div>
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <div v-for="g in goalCards" :key="g.id" class="goal-card p-4 rounded-xl" :style="{ '--gc': g.color.from, '--gc2': g.color.to }">
          <div class="flex items-center justify-between">
            <span class="font-semibold text-slate-700">{{ g.displayName }}</span>
            <span class="text-xs px-2 py-0.5 rounded-full text-white" :style="{ background: g.color.from }">{{ g.progress }}%</span>
          </div>
          <div class="text-xs text-slate-400 mt-1">目标 {{ g.targetValue }}{{ g.unit }}</div>
          <div class="mt-3 h-2 rounded-full bg-slate-100 overflow-hidden">
            <div class="h-full rounded-full transition-all duration-700" :style="{ width: g.progress + '%', background: `linear-gradient(90deg, ${g.color.from}, ${g.color.to})` }"></div>
          </div>
        </div>
        <div
          class="goal-card p-4 rounded-xl border-2 border-dashed border-indigo-200 flex flex-col items-center justify-center text-indigo-400 cursor-pointer hover:border-indigo-400 hover:text-indigo-600 transition-colors"
          @click="router.push('/checkin')"
        >
          <Plus :size="22" />
          <span class="text-xs mt-1">设定新目标</span>
        </div>
      </div>
      <div v-if="!goals.length" class="text-slate-400 text-sm text-center py-4">暂无激活目标，去「每日打卡」页设定吧</div>
    </section>

    <!-- 横向滑动卡片（习惯贴士） -->
    <section class="mb-6">
      <h3 class="font-semibold mb-3 text-slate-700">习惯小贴士</h3>
      <div class="flex gap-4 overflow-x-auto pb-3 scroll-smooth">
        <div
          v-for="t in tips"
          :key="t.title"
          class="min-w-[200px] glass rounded-card-xl p-4 flex-shrink-0 hover:-translate-y-1 transition-transform"
        >
          <div class="h-10 w-10 rounded-xl flex items-center justify-center text-white mb-3" :class="t.grad">
            <component :is="t.icon" :size="20" />
          </div>
          <div class="font-semibold text-slate-700">{{ t.title }}</div>
          <div class="text-xs text-slate-500 mt-1 leading-relaxed">{{ t.desc }}</div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 14px 36px rgba(99, 102, 241, 0.18);
}
.goal-card {
  border: 1px solid rgba(255, 255, 255, 0.6);
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(12px);
}
</style>
