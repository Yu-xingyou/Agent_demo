<script setup>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  Flower2, Moon, Droplets, Dumbbell, Smile, Sparkles, ArrowRight,
  Target, ChevronRight, Plus, Trash2, X,
} from 'lucide-vue-next'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, RadarChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent, RadarComponent } from 'echarts/components'
import * as habitApi from '@/api/habit'
import * as goalApi from '@/api/goal'
import * as analysisApi from '@/api/analysis'
import ReminderWidget from '@/components/ReminderWidget.vue'
import { goalColor, MOOD_COLORS, MOOD_LABELS } from '@/constants/theme'
import { ElMessage, ElMessageBox } from 'element-plus'

use([CanvasRenderer, PieChart, RadarChart, TooltipComponent, LegendComponent, RadarComponent])

const router = useRouter()
const today = ref(null)
const recent = ref([])
const goals = ref([])
const overview = ref(null)
const radar = ref(null)
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
  ;(recent.value || []).forEach((r) => {
    if (r.mood) counts[r.mood] = (counts[r.mood] || 0) + 1
  })
  const data = Object.keys(MOOD_COLORS).map((k) => ({
    name: MOOD_LABELS[k],
    value: counts[k] || 0,
    itemStyle: { color: MOOD_COLORS[k] },
  }))
  return {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255,255,255,0.92)',
      borderWidth: 1,
      borderColor: 'rgba(102,126,234,0.25)',
      textStyle: { color: '#1f2440' },
      extraCssText: 'box-shadow:0 10px 30px rgba(31,38,89,0.18);border-radius:12px;',
    },
    legend: { bottom: 0, textStyle: { color: '#5b6178' } },
    series: [{
      type: 'pie',
      radius: ['48%', '72%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 8, borderColor: 'rgba(255,255,255,0.8)', borderWidth: 2 },
      label: { show: false },
      data,
    }],
  }
})

// 近 7 天多维能力画像雷达图（真实 analysis/radar，含自定义目标动态维度）
const radarOption = computed(() => {
  const ra = radar.value || { indicators: [], values: [] }
  const rawIndicators = Array.isArray(ra.indicators) && ra.indicators.length
    ? ra.indicators
    : ['睡眠', '运动', '饮水', '饮食', '心情'].map((name) => ({ name, max: 100 }))
  const indicators = rawIndicators.map((it) =>
    typeof it === 'string' ? { name: it, max: 100 } : { name: it.name, max: it.max || 100 }
  )
  const dims = indicators.length
  const vals = Array.isArray(ra.values) ? ra.values.slice(0, dims) : []
  while (vals.length < dims) vals.push(0)
  return {
    tooltip: {
      backgroundColor: 'rgba(255,255,255,0.92)',
      borderWidth: 1,
      borderColor: 'rgba(102,126,234,0.25)',
      textStyle: { color: '#1f2440' },
      extraCssText: 'box-shadow:0 10px 30px rgba(31,38,89,0.18);border-radius:12px;',
    },
    radar: {
      indicator: indicators,
      radius: '66%',
      center: ['50%', '54%'],
      axisName: { color: '#5b6178', fontSize: 12 },
      splitLine: { lineStyle: { color: 'rgba(102,126,234,0.12)' } },
      splitArea: { areaStyle: { color: ['rgba(102,126,234,0.04)', 'rgba(118,75,162,0.05)'] } },
      axisLine: { lineStyle: { color: 'rgba(102,126,234,0.18)' } },
    },
    series: [{
      type: 'radar',
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color: '#667eea', width: 2.5 },
      itemStyle: { color: '#764ba2' },
      areaStyle: { color: 'rgba(102,126,234,0.28)' },
      data: [{ value: vals, name: '近7天能力画像' }],
    }],
  }
})

// 任务目标进度（基于 HabitGoalVO，按类型配色）
const goalCards = computed(() => (goals.value || []).map((g) => {
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
    const [t, r, g, ov, ra] = await Promise.all([
      habitApi.getToday().catch(() => null),
      habitApi.getRecent(7),
      goalApi.getActiveWithCustom().catch(() => []),
      analysisApi.getOverview(7).catch(() => null),
      analysisApi.getRadar(7).catch(() => null),
    ])
    today.value = t
    recent.value = r || []
    goals.value = g || []
    overview.value = ov
    radar.value = ra
  } catch (e) {
    ElMessage.error('加载首页数据失败')
  } finally {
    loading.value = false
  }
}

// ===== 自定义目标：首页内联新增 & 删除 =====
const showAddGoal = ref(false)
const addGoalForm = ref({ customName: '', targetValue: null, unit: '' })
const adding = ref(false)

async function confirmAddGoal() {
  const name = addGoalForm.value.customName.trim()
  if (!name) {
    ElMessage.warning('请填写目标名称')
    return
  }
  if (!addGoalForm.value.targetValue || Number(addGoalForm.value.targetValue) <= 0) {
    ElMessage.warning('请填写有效的目标值')
    return
  }
  adding.value = true
  try {
    await goalApi.create({
      goalType: 'CUSTOM',
      customName: name,
      targetValue: Number(addGoalForm.value.targetValue),
      unit: addGoalForm.value.unit.trim(),
    })
    ElMessage.success('目标已添加')
    showAddGoal.value = false
    addGoalForm.value = { customName: '', targetValue: null, unit: '' }
    await load() // 重新加载，雷达图随自定义目标增减自动更新
  } catch (e) {
    // 透传后端具体错误（如 CODE_DUPLICATE_GOAL: 该自定义目标已存在）
    const msg = e?.response?.data?.message || e?.message || '添加目标失败'
    ElMessage.error(msg)
  } finally {
    adding.value = false
  }
}

async function removeGoal(g) {
  try {
    await ElMessageBox.confirm(`确定删除目标「${g.displayName}」吗？相关打卡记录也会一并清除。`, '删除目标', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await goalApi.deleteGoal(g.id)
    ElMessage.success('目标已删除')
    await load() // 重新加载，雷达图随自定义目标增减自动更新
  } catch (e) {
    ElMessage.error('删除目标失败')
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

    <!-- 打卡提醒小窗格（轻量、不抢视觉） -->
    <section class="mb-6">
      <ReminderWidget />
    </section>

    <!-- 饼图 + 折线趋势 -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
      <div class="glass rounded-card-xl p-5 lg:col-span-1">
        <h3 class="font-semibold mb-2 text-slate-700">本周心情分布</h3>
        <VChart :option="moodOption" autoresize style="height: 250px" />
      </div>
      <div class="glass rounded-card-xl p-5 lg:col-span-2">
        <div class="flex items-center justify-between mb-2">
          <h3 class="font-semibold text-slate-700">近 7 天能力画像</h3>
          <button class="text-brand text-sm hover:text-brand-soft flex items-center" @click="router.push('/trend')">
            详细分析 <ChevronRight :size="14" />
          </button>
        </div>
        <VChart :option="radarOption" autoresize style="height: 250px" />
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
        <button class="text-brand text-sm hover:text-brand-soft flex items-center" @click="router.push('/ai-chat')">
          让 AI 帮你设定 <ChevronRight :size="14" />
        </button>
      </div>
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <div v-for="g in goalCards" :key="g.id" class="goal-card p-4 rounded-xl group" :style="{ '--gc': g.color.from, '--gc2': g.color.to }">
          <div class="flex items-center justify-between">
            <span class="font-semibold text-slate-700">{{ g.displayName }}</span>
            <div class="flex items-center gap-2">
              <span class="text-xs px-2 py-0.5 rounded-full text-white" :style="{ background: g.color.from }">{{ g.progress }}%</span>
              <button
                v-if="g.goalType === 'CUSTOM'"
                class="opacity-0 group-hover:opacity-100 text-slate-400 hover:text-rose-500 transition-all"
                title="删除目标"
                @click="removeGoal(g)"
              >
                <Trash2 :size="15" />
              </button>
            </div>
          </div>
          <div class="text-xs text-slate-400 mt-1">目标 {{ g.targetValue }}{{ g.unit }}</div>
          <div class="mt-3 h-2 rounded-full bg-slate-100 overflow-hidden">
            <div class="h-full rounded-full transition-all duration-700" :style="{ width: g.progress + '%', background: `linear-gradient(90deg, ${g.color.from}, ${g.color.to})` }"></div>
          </div>
        </div>
        <div
          class="goal-card p-4 rounded-xl border-2 border-dashed border-indigo-200 flex flex-col items-center justify-center text-indigo-400 cursor-pointer hover:border-indigo-400 hover:text-indigo-600 transition-colors"
          @click="showAddGoal = true"
        >
          <Plus :size="22" />
          <span class="text-xs mt-1">设定新目标</span>
        </div>
      </div>

      <!-- 内联新增自定义目标表单 -->
      <div v-if="showAddGoal" class="glass rounded-card-xl p-5 mt-4 border border-indigo-200">
        <div class="flex items-center justify-between mb-3">
          <h4 class="font-semibold text-slate-700">新增自定义目标</h4>
          <button class="text-slate-400 hover:text-slate-600" @click="showAddGoal = false">
            <X :size="18" />
          </button>
        </div>
        <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <input
            v-model="addGoalForm.customName"
            class="px-3 py-2 rounded-lg border border-slate-200 text-sm outline-none focus:border-indigo-300"
            placeholder="目标名称，如「阅读」"
          />
          <input
            v-model="addGoalForm.targetValue"
            type="number"
            min="1"
            class="px-3 py-2 rounded-lg border border-slate-200 text-sm outline-none focus:border-indigo-300"
            placeholder="目标值，如 30"
          />
          <input
            v-model="addGoalForm.unit"
            class="px-3 py-2 rounded-lg border border-slate-200 text-sm outline-none focus:border-indigo-300"
            placeholder="单位，如「分钟」"
          />
        </div>
        <div class="flex justify-end gap-2 mt-3">
          <button class="px-4 py-1.5 rounded-lg text-sm text-slate-500 hover:bg-slate-100" @click="showAddGoal = false">取消</button>
          <button
            class="px-4 py-1.5 rounded-lg text-sm text-white bg-grad-primary disabled:opacity-50"
            :disabled="adding"
            @click="confirmAddGoal"
          >
            {{ adding ? '保存中…' : '保存目标' }}
          </button>
        </div>
      </div>
      <div v-if="!goals.length" class="text-slate-400 text-sm text-center py-4">还没有目标，点击上方「设定新目标」，让 AI 为你定制专属习惯计划</div>
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
