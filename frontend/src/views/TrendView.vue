<script setup>
import { ref, onMounted, computed } from 'vue'
import { TrendingUp, Moon, Dumbbell, Droplets, Target, Radar } from 'lucide-vue-next'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, RadarChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import * as analysisApi from '@/api/analysis'
import * as goalApi from '@/api/goal'
import { ElMessage } from 'element-plus'

use([CanvasRenderer, LineChart, BarChart, RadarChart, TooltipComponent, LegendComponent, GridComponent])

const trend = ref(null)
const radar = ref(null)
const overview = ref(null)
const customGoals = ref([])
const days = ref(7)

async function load() {
  try {
    const [tr, rd, ov, goals] = await Promise.all([
      analysisApi.getTrends(days.value).catch(() => null),
      analysisApi.getRadar(days.value).catch(() => null),
      analysisApi.getOverview(days.value).catch(() => null),
      goalApi.getActiveWithCustom().catch(() => []),
    ])
    trend.value = tr
    radar.value = rd
    overview.value = ov
    customGoals.value = (goals || []).filter((g) => g.goalType === 'CUSTOM')
  } catch (e) {
    ElMessage.error('加载趋势数据失败')
  }
}
onMounted(load)

function baseGrid() {
  return { left: 40, right: 16, top: 30, bottom: 28 }
}
function axisStyle() {
  return {
    axisLine: { lineStyle: { color: '#dbe0f0' } },
    axisLabel: { color: '#94a3b8' },
  }
}
// 玻璃卡片化 tooltip
const chartTooltip = {
  trigger: 'axis',
  backgroundColor: 'rgba(255,255,255,0.92)',
  borderWidth: 1,
  borderColor: 'rgba(102,126,234,0.25)',
  textStyle: { color: '#1f2440' },
  extraCssText: 'box-shadow:0 10px 30px rgba(31,38,89,0.18);border-radius:12px;',
}
const chartTooltipItem = {
  backgroundColor: 'rgba(255,255,255,0.92)',
  borderWidth: 1,
  borderColor: 'rgba(102,126,234,0.25)',
  textStyle: { color: '#1f2440' },
  extraCssText: 'box-shadow:0 10px 30px rgba(31,38,89,0.18);border-radius:12px;',
}
// 过滤脏数据（值大于目标值 10 倍视为异常），动态计算 yAxis max
function dynamicYAxis(data, target) {
  const cleaned = (data || []).map((v) => {
    if (v == null) return null
    if (target && v > target * 10) return null
    return v
  })
  const valid = cleaned.filter((v) => v != null)
  const peak = valid.length ? Math.max(...valid) : 0
  const max = Math.max(target ? target * 1.5 : 10, peak * 1.2)
  return {
    type: 'value',
    min: 0,
    max: Math.ceil(max),
    splitLine: { lineStyle: { color: 'rgba(102,126,234,0.1)' } },
    axisLabel: { color: '#94a3b8' },
  }
}

const sleepOption = computed(() => ({
  tooltip: chartTooltip,
  grid: baseGrid(),
  xAxis: { type: 'category', data: trend.value?.dates || [], ...axisStyle() },
  yAxis: dynamicYAxis(trend.value?.sleep, overview.value?.sleepTarget),
  series: [{
    name: '睡眠(h)', type: 'line', smooth: true, data: trend.value?.sleep || [],
    itemStyle: { color: '#6366f1' }, areaStyle: { color: 'rgba(99,102,241,0.16)' },
    lineStyle: { width: 2.5, color: '#6366f1' }, symbol: 'circle', symbolSize: 6,
  }],
}))

const exerciseOption = computed(() => ({
  tooltip: chartTooltip,
  grid: baseGrid(),
  xAxis: { type: 'category', data: trend.value?.dates || [], ...axisStyle() },
  yAxis: dynamicYAxis(trend.value?.exercise, overview.value?.exerciseTarget),
  series: [{
    name: '运动(min)', type: 'bar', data: trend.value?.exercise || [],
    itemStyle: { color: '#f59e0b', borderRadius: [6, 6, 0, 0] },
  }],
}))

const waterOption = computed(() => ({
  tooltip: chartTooltip,
  grid: baseGrid(),
  xAxis: { type: 'category', data: trend.value?.dates || [], ...axisStyle() },
  yAxis: dynamicYAxis(trend.value?.water, overview.value?.waterTarget),
  series: [{
    name: '饮水(ml)', type: 'line', smooth: true, data: trend.value?.water || [],
    itemStyle: { color: '#22d3ee' }, areaStyle: { color: 'rgba(34,211,238,0.16)' },
    lineStyle: { width: 2.5, color: '#22d3ee' }, symbol: 'circle', symbolSize: 6,
  }],
}))

const radarOption = computed(() => {
  const rd = radar.value || { indicators: [], values: [], targets: [] }
  const rawIndicators = rd.indicators && rd.indicators.length ? rd.indicators : ['睡眠', '运动', '饮水', '饮食', '心情']
  const indicators = rawIndicators.map((it) =>
    typeof it === 'string' ? { name: it, max: 100 } : { name: it.name, max: it.max || 100 }
  )
  const dims = indicators.length
  const toArr = (arr) => {
    const a = Array.isArray(arr) ? arr.slice(0, dims) : []
    while (a.length < dims) a.push(0)
    return a
  }
  return {
    tooltip: chartTooltipItem,
    legend: { data: ['实际', '目标'], textStyle: { color: '#5b6178' }, bottom: 0 },
    radar: {
      indicator: indicators,
      radius: '62%',
      axisName: { color: '#5b6178', fontSize: 12 },
      splitLine: { lineStyle: { color: 'rgba(102,126,234,0.12)' } },
      splitArea: { areaStyle: { color: ['rgba(102,126,234,0.04)', 'rgba(118,75,162,0.05)'] } },
      axisLine: { lineStyle: { color: 'rgba(102,126,234,0.18)' } },
    },
    series: [{
      type: 'radar',
      data: [
        { value: toArr(rd.values), name: '实际', itemStyle: { color: '#667eea' }, areaStyle: { color: 'rgba(102,126,234,0.28)' }, lineStyle: { width: 2.5, color: '#667eea' } },
        { value: toArr(rd.targets), name: '目标', itemStyle: { color: '#ec4899' }, lineStyle: { type: 'dashed', color: '#ec4899' } },
      ],
    }],
  }
})

// 自定义目标动态趋势图（每个 CUSTOM 目标一张）
const customOptions = computed(() =>
  (trend.value?.customSeries || []).map((s) => ({
    key: s.goalId,
    name: s.name,
    unit: s.unit,
    targetValue: s.targetValue,
    colorFrom: s.colorFrom,
    colorTo: s.colorTo,
    option: {
      tooltip: chartTooltip,
      grid: baseGrid(),
      xAxis: { type: 'category', data: trend.value?.dates || [], ...axisStyle() },
      yAxis: dynamicYAxis(s.data, s.targetValue),
      series: [{
        name: s.name, type: 'line', smooth: true, data: s.data || [],
        itemStyle: { color: s.colorFrom }, lineStyle: { width: 2.5, color: s.colorFrom },
        areaStyle: { color: 'rgba(138,127,160,0.16)' }, symbol: 'circle', symbolSize: 6,
      }],
    },
  }))
)

const statCards = computed(() => {
  const o = overview.value || {}
  return [
    { title: '平均睡眠', value: o.avgSleep != null ? `${o.avgSleep}h` : '-', grad: 'bg-grad-sleep' },
    { title: '平均运动', value: o.avgExercise != null ? `${o.avgExercise}min` : '-', grad: 'bg-grad-exercise' },
    { title: '平均饮水', value: o.avgWater != null ? `${o.avgWater}ml` : '-', grad: 'bg-grad-water' },
    { title: '打卡天数', value: `${o.recordDays || 0}`, grad: 'bg-grad-primary' },
  ]
})
</script>

<template>
  <div class="max-w-6xl mx-auto px-5 py-6 relative z-10">
    <div class="flex items-center gap-2 mb-1">
      <TrendingUp class="text-brand" />
      <h1 class="text-2xl font-semibold text-slate-800">趋势分析</h1>
    </div>
    <p class="text-sm text-slate-500 mb-5">
      基于近 {{ days }} 天真实打卡数据，呈现你的睡眠、运动、饮水与综合健康画像。
    </p>

    <!-- 概览卡 -->
    <section class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
      <div v-for="(s, i) in statCards" :key="s.title" class="glass rounded-card-xl p-4 animate-rise" :style="{ animationDelay: i * 70 + 'ms' }">
        <div class="h-8 w-8 rounded-lg flex items-center justify-center text-white shadow mb-2" :class="s.grad"></div>
        <div class="text-xl font-bold text-slate-800">{{ s.value }}</div>
        <div class="text-xs text-slate-400 mt-1">{{ s.title }}</div>
      </div>
    </section>

    <!-- 内置四块图表网格 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div class="glass rounded-card-xl p-5">
        <div class="flex items-center gap-2 mb-3 text-slate-700 font-medium"><Moon class="text-brand" /> 睡眠趋势</div>
        <VChart :option="sleepOption" autoresize style="height: 240px" />
      </div>
      <div class="glass rounded-card-xl p-5">
        <div class="flex items-center gap-2 mb-3 text-slate-700 font-medium"><Dumbbell class="text-amber-500" /> 运动趋势</div>
        <VChart :option="exerciseOption" autoresize style="height: 240px" />
      </div>
      <div class="glass rounded-card-xl p-5">
        <div class="flex items-center gap-2 mb-3 text-slate-700 font-medium"><Droplets class="text-brand" /> 饮水趋势</div>
        <VChart :option="waterOption" autoresize style="height: 240px" />
      </div>
      <div class="glass rounded-card-xl p-5">
        <div class="flex items-center gap-2 mb-3 text-slate-700 font-medium"><Radar class="text-brand-soft" /> 综合能力雷达</div>
        <VChart :option="radarOption" autoresize style="height: 240px" />
      </div>
    </div>

    <!-- 自定义目标动态追加图表 -->
    <div v-if="customOptions.length" class="grid grid-cols-1 lg:grid-cols-2 gap-6 mt-6">
      <div v-for="c in customOptions" :key="c.key" class="glass rounded-card-xl p-5">
        <div class="flex items-center justify-between mb-3">
          <div class="flex items-center gap-2 text-slate-700 font-medium">
            <Target class="text-brand-soft" /> {{ c.name }}趋势
          </div>
          <span class="text-xs text-slate-400">目标 {{ c.targetValue }}{{ c.unit || '' }}</span>
        </div>
        <VChart :option="c.option" autoresize style="height: 240px" />
      </div>
    </div>

    <div v-else class="glass rounded-card-xl p-5 mt-6 text-center text-sm text-slate-400">
      暂无自定义目标，去「每日打卡」页添加后即可在此查看专属趋势。
    </div>
  </div>
</template>
