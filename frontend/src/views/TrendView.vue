<script setup>
import { ref, onMounted, computed } from 'vue'
import { TrendingUp, Moon, Dumbbell, Droplets, Target, Radar } from 'lucide-vue-next'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, RadarChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import * as analysisApi from '@/api/analysis'
import { CHART_PALETTE } from '@/constants/theme'
import { ElMessage } from 'element-plus'

use([CanvasRenderer, LineChart, BarChart, RadarChart, TooltipComponent, LegendComponent, GridComponent])

const trend = ref(null)
const radar = ref(null)
const overview = ref(null)
const days = ref(7)

async function load() {
  try {
    const [tr, rd, ov] = await Promise.all([
      analysisApi.getTrends(days.value).catch(() => null),
      analysisApi.getRadar(days.value).catch(() => null),
      analysisApi.getOverview(days.value).catch(() => null),
    ])
    trend.value = tr
    radar.value = rd
    overview.value = ov
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
    axisLine: { lineStyle: { color: '#cbd5e1' } },
    axisLabel: { color: '#94a3b8' },
  }
}

const sleepOption = computed(() => ({
  tooltip: { trigger: 'axis', backgroundColor: 'rgba(15,23,42,0.85)', borderWidth: 0, textStyle: { color: '#fff' } },
  grid: baseGrid(),
  xAxis: { type: 'category', data: trend.value?.dates || [], ...axisStyle() },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: 'rgba(148,163,184,0.2)' } }, axisLabel: { color: '#94a3b8' } },
  series: [{
    name: '睡眠(h)', type: 'line', smooth: true, data: trend.value?.sleep || [],
    itemStyle: { color: '#6366f1' }, areaStyle: { color: 'rgba(99,102,241,0.14)' },
  }],
}))

const exerciseOption = computed(() => ({
  tooltip: { trigger: 'axis', backgroundColor: 'rgba(15,23,42,0.85)', borderWidth: 0, textStyle: { color: '#fff' } },
  grid: baseGrid(),
  xAxis: { type: 'category', data: trend.value?.dates || [], ...axisStyle() },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: 'rgba(148,163,184,0.2)' } }, axisLabel: { color: '#94a3b8' } },
  series: [{
    name: '运动(min)', type: 'bar', data: trend.value?.exercise || [],
    itemStyle: { color: '#f59e0b', borderRadius: [6, 6, 0, 0] },
  }],
}))

const waterOption = computed(() => ({
  tooltip: { trigger: 'axis', backgroundColor: 'rgba(15,23,42,0.85)', borderWidth: 0, textStyle: { color: '#fff' } },
  grid: baseGrid(),
  xAxis: { type: 'category', data: trend.value?.dates || [], ...axisStyle() },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: 'rgba(148,163,184,0.2)' } }, axisLabel: { color: '#94a3b8' } },
  series: [{
    name: '饮水(ml)', type: 'line', smooth: true, data: trend.value?.water || [],
    itemStyle: { color: '#06b6d4' }, areaStyle: { color: 'rgba(6,182,212,0.14)' },
  }],
}))

const radarOption = computed(() => {
  const rd = radar.value || { indicators: [], values: [], targets: [] }
  return {
    tooltip: { backgroundColor: 'rgba(15,23,42,0.85)', borderWidth: 0, textStyle: { color: '#fff' } },
    legend: { data: ['实际', '目标'], textStyle: { color: '#64748b' }, bottom: 0 },
    radar: {
      indicator: (rd.indicators || []).map((name) => ({ name, max: 100 })),
      radius: '62%',
      axisName: { color: '#475569', fontSize: 12 },
      splitLine: { lineStyle: { color: 'rgba(148,163,184,0.3)' } },
      splitArea: { areaStyle: { color: ['rgba(99,102,241,0.04)', 'rgba(168,85,247,0.04)'] } },
    },
    series: [{
      type: 'radar',
      data: [
        { value: rd.values || [], name: '实际', itemStyle: { color: '#6366f1' }, areaStyle: { color: 'rgba(99,102,241,0.25)' } },
        { value: rd.targets || [], name: '目标', itemStyle: { color: '#a855f7' }, lineStyle: { type: 'dashed' } },
      ],
    }],
  }
})

const statCards = computed(() => {
  const o = overview.value || {}
  return [
    { title: '平均睡眠', value: o.avgSleep != null ? `${o.avgSleep}h` : '-', grad: 'bg-grad-sleep' },
    { title: '平均运动', value: o.avgExercise != null ? `${o.avgExercise}min` : '-', grad: 'bg-grad-exercise' },
    { title: '平均饮水', value: o.avgWater != null ? `${o.avgWater}ml` : '-', grad: 'bg-grad-water' },
    { title: '打卡天数', value: `${o.checkedDays || 0}/${o.days || days.value}`, grad: 'bg-grad-primary' },
  ]
})
</script>

<template>
  <div class="max-w-6xl mx-auto px-5 py-6 relative z-10">
    <div class="flex items-center gap-2 mb-1">
      <TrendingUp class="text-brand-indigo" />
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

    <!-- 四块图表网格 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div class="glass rounded-card-xl p-5">
        <div class="flex items-center gap-2 mb-3 text-slate-700 font-medium"><Moon class="text-brand-indigo" /> 睡眠趋势</div>
        <VChart :option="sleepOption" autoresize style="height: 240px" />
      </div>
      <div class="glass rounded-card-xl p-5">
        <div class="flex items-center gap-2 mb-3 text-slate-700 font-medium"><Dumbbell class="text-amber-500" /> 运动趋势</div>
        <VChart :option="exerciseOption" autoresize style="height: 240px" />
      </div>
      <div class="glass rounded-card-xl p-5">
        <div class="flex items-center gap-2 mb-3 text-slate-700 font-medium"><Droplets class="text-cyan-500" /> 饮水趋势</div>
        <VChart :option="waterOption" autoresize style="height: 240px" />
      </div>
      <div class="glass rounded-card-xl p-5">
        <div class="flex items-center gap-2 mb-3 text-slate-700 font-medium"><Radar class="text-brand-purple" /> 综合能力雷达</div>
        <VChart :option="radarOption" autoresize style="height: 240px" />
      </div>
    </div>
  </div>
</template>
