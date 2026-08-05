<template>
  <div class="p-6 space-y-6">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-[22px] font-semibold text-slate-800">AI 分析报告</h2>
        <p class="text-sm text-slate-500 mt-1">基于你的打卡、目标与习惯数据，生成立体的可视化分析</p>
      </div>
      <div class="flex items-center gap-2">
        <el-button :icon="RefreshLeft" @click="reloadHistory">刷新列表</el-button>
        <el-button type="primary" :icon="MagicStick" :loading="generating" @click="generate">
          {{ generating ? '生成中…' : '生成报告' }}
        </el-button>
      </div>
    </div>

    <div v-if="generating" class="glass-card p-6 text-center text-slate-500">
      <el-icon class="is-loading text-2xl text-blue-500"><Loading /></el-icon>
      <p class="mt-2">正在为你分析，请稍候…</p>
    </div>

    <template v-else-if="task">
      <!-- 评分环 + 结构化结论 -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <div class="glass-card flex flex-col items-center justify-center p-6">
          <svg viewBox="0 0 120 120" class="w-32 h-32">
            <circle cx="60" cy="60" r="52" fill="none" stroke="rgba(255,255,255,.6)" stroke-width="12" />
            <circle cx="60" cy="60" r="52" fill="none" :stroke="scoreRing.color" stroke-width="12"
              stroke-linecap="round" :stroke-dasharray="scoreRing.dash" stroke-dashoffset="0"
              transform="rotate(-90 60 60)" />
            <text x="60" y="56" text-anchor="middle" class="fill-slate-800 font-semibold" style="font-size:26px">{{ task.score ?? '--' }}</text>
            <text x="60" y="76" text-anchor="middle" class="fill-slate-500" style="font-size:11px">综合评分</text>
          </svg>
          <p class="mt-2 text-xs text-slate-400">{{ task.createdAt ? new Date(task.createdAt).toLocaleString() : '' }}</p>
        </div>

        <div class="glass-card p-5">
          <div class="flex items-center gap-2 text-blue-600 font-medium mb-2">
            <el-icon><Sunny /></el-icon> 今日点评
          </div>
          <p class="text-sm text-slate-600 leading-relaxed">{{ task.dailyEvaluation || '暂无' }}</p>
        </div>

        <div class="glass-card p-5">
          <div class="flex items-center gap-2 text-indigo-600 font-medium mb-2">
            <el-icon><TrendCharts /></el-icon> 趋势总结
          </div>
          <p class="text-sm text-slate-600 leading-relaxed">{{ task.trendSummary || '暂无' }}</p>
        </div>
      </div>

      <div class="glass-card p-5">
        <div class="flex items-center gap-2 text-amber-600 font-medium mb-2">
          <el-icon><Warning /></el-icon> 风险提示
        </div>
        <p class="text-sm text-slate-600 leading-relaxed">{{ task.riskWarning || '暂无风险' }}</p>
      </div>

      <div class="glass-card p-5">
        <div class="flex items-center gap-2 text-emerald-600 font-medium mb-2">
          <el-icon><Star /></el-icon> 改进建议
        </div>
        <ul class="list-disc list-inside space-y-1 text-sm text-slate-600">
          <li v-for="(s, i) in suggestionList" :key="i">{{ s }}</li>
          <li v-if="!suggestionList.length" class="list-none text-slate-400">暂无建议</li>
        </ul>
      </div>

      <!-- 图表区（空数据占位防白屏） -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div class="glass-card p-4">
          <h3 class="text-sm font-medium text-slate-700 mb-2">趋势分析</h3>
          <VChart v-if="hasTrend" autoresize :option="trendOption" />
          <div v-else class="h-64 flex items-center justify-center text-slate-400 text-sm">图表生成中…</div>
        </div>
        <div class="glass-card p-4">
          <h3 class="text-sm font-medium text-slate-700 mb-2">维度达成率</h3>
          <VChart v-if="hasAchievement" autoresize :option="achievementOption" />
          <div v-else class="h-64 flex items-center justify-center text-slate-400 text-sm">图表生成中…</div>
        </div>
        <div class="glass-card p-4 lg:col-span-2">
          <h3 class="text-sm font-medium text-slate-700 mb-2">能力雷达</h3>
          <VChart v-if="hasRadar" autoresize :option="radarOption" />
          <div v-else class="h-64 flex items-center justify-center text-slate-400 text-sm">图表生成中…</div>
        </div>
      </div>

      <div class="glass-card p-5">
        <div class="text-sm font-medium text-slate-700 mb-2">完整报告</div>
        <pre class="whitespace-pre-wrap text-sm text-slate-600 leading-relaxed">{{ task.report || '暂无报告内容' }}</pre>
      </div>

      <!-- 历史 -->
      <div class="glass-card p-5" v-if="history.length">
        <div class="text-sm font-medium text-slate-700 mb-3">历史报告</div>
        <div class="flex flex-wrap gap-2">
          <button v-for="h in history" :key="h.id"
            class="px-3 py-1.5 rounded-full text-xs glass-card-soft text-slate-600 hover:text-blue-600 transition"
            @click="selectHistory(h.id)">
            {{ h.createdAt ? new Date(h.createdAt).toLocaleDateString() : '报告' }}
          </button>
        </div>
      </div>
    </template>

    <!-- 无报告引导 -->
    <div v-else class="glass-card p-10 text-center">
      <el-icon class="text-4xl text-blue-400"><DataAnalysis /></el-icon>
      <h3 class="mt-3 text-lg font-medium text-slate-700">还没有分析报告</h3>
      <p class="mt-1 text-sm text-slate-500">基于你近 7 天的打卡、目标与习惯数据，一键生成专属分析报告</p>
      <el-button type="primary" class="mt-4" :icon="MagicStick" :loading="generating" @click="generate">
        生成我的分析报告
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, RadarChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, RadarComponent, TitleComponent } from 'echarts/components'
import {
  RefreshLeft, MagicStick, Loading, Sunny, TrendCharts, Warning, Star, DataAnalysis
} from '@element-plus/icons-vue'
import { getLatestAnalysis, getAnalysisTask, listAnalysisHistory, generateAnalysis, regenerateAnalysis } from '@/api/aiAnalysis'

use([CanvasRenderer, LineChart, BarChart, RadarChart, PieChart, GridComponent, TooltipComponent, LegendComponent, RadarComponent, TitleComponent])

const props = defineProps({ tabId: { type: String, default: 'ai-analysis' } })

const task = ref(null)
const history = ref([])
const generating = ref(false)

const toArr = (v) => (Array.isArray(v) ? v : [])

const scoreRing = computed(() => {
  const s = task.value?.score ?? 0
  const color = s >= 80 ? '#10B981' : s >= 60 ? '#3B82F6' : '#F59E0B'
  return { color, dash: `${(s / 100) * 327} 327` }
})

const suggestionList = computed(() =>
  (task.value?.suggestion || '').split('\n').map(s => s.trim()).filter(Boolean)
)

const hasTrend = computed(() => !!task.value?.charts?.trend)
const hasAchievement = computed(() => !!task.value?.charts?.achievement)
const hasRadar = computed(() => !!task.value?.charts?.radar)

const trendOption = computed(() => {
  const t = task.value?.charts?.trend || {}
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 16, top: 30, bottom: 30 },
    legend: { data: ['打卡', '目标', '运动'], top: 0 },
    xAxis: { type: 'category', data: toArr(t.dates) },
    yAxis: { type: 'value' },
    series: [
      { name: '打卡', type: 'line', smooth: true, data: toArr(t.checkin) },
      { name: '目标', type: 'line', smooth: true, data: toArr(t.goal) },
      { name: '运动', type: 'line', smooth: true, data: toArr(t.exercise) }
    ]
  }
})

const achievementOption = computed(() => {
  const a = task.value?.charts?.achievement?.items || []
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['45%', '70%'],
      data: a.map(it => ({ name: it?.name || '项', value: it?.value ?? 0 }))
    }]
  }
})

const radarOption = computed(() => {
  const r = task.value?.charts?.radar || {}
  const indicators = toArr(r.indicators).map(it => ({ name: it?.name || '', max: it?.max ?? 100 }))
  const values = toArr(r.values).map(v => v ?? 0)
  return {
    tooltip: {},
    radar: { indicator: indicators.length ? indicators : [{ name: '默认', max: 100 }] },
    series: [{ type: 'radar', data: [{ value: values.length ? values : [0] }] }]
  }
})

async function generate() {
  generating.value = true
  try {
    const res = await generateAnalysis(7)
    const id = res?.data?.taskId ?? res?.taskId ?? res?.data?.id ?? res?.id
    if (id) {
      await poll(id)
    } else {
      await loadLatest()
    }
  } finally {
    generating.value = false
  }
}

async function poll(id) {
  for (let i = 0; i < 40; i++) {
    const res = await getAnalysisTask(id)
    const t = res?.data ?? res
    if (t?.status === 'COMPLETED') { task.value = t; return }
    if (t?.status === 'FAILED') { return }
    await new Promise(r => setTimeout(r, 1500))
  }
}

async function loadLatest() {
  const res = await getLatestAnalysis()
  const t = res?.data ?? res
  if (t && t.id) task.value = t
  else task.value = null
}

async function reloadHistory() {
  const res = await listAnalysisHistory(10)
  history.value = res?.data ?? res ?? []
}

async function selectHistory(id) {
  const res = await getAnalysisTask(id)
  task.value = res?.data ?? res
}

function init() {
  reloadHistory()
  loadLatest()
}

onMounted(init)
watch(() => props.tabId, init)
</script>

<style scoped>
.glass-card {
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 18px;
  box-shadow: 0 8px 30px rgba(30, 41, 59, 0.08);
}
.glass-card-soft {
  background: rgba(255, 255, 255, 0.45);
  border: 1px solid rgba(255, 255, 255, 0.5);
}
</style>
