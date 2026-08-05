<script setup>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { BrainCircuit, RefreshCw, Clock, FileText, Loader2 } from 'lucide-vue-next'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, RadarChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent } from 'echarts/components'
import * as aiApi from '@/api/aiAnalysis'
import { ElMessage } from 'element-plus'

use([CanvasRenderer, LineChart, RadarChart, TooltipComponent, LegendComponent])

const days = ref(7)
const task = ref(null)
const history = ref([])
const loading = ref(false)
const generating = ref(false)
let pollTimer = null

async function generate() {
  generating.value = true
  try {
    const { data } = await aiApi.generateAnalysis(days.value)
    task.value = data
    ElMessage.success('已提交分析任务，正在生成…')
    startPolling()
  } catch (e) {
    ElMessage.error('提交分析失败')
  } finally {
    generating.value = false
  }
}

function startPolling() {
  stopPolling()
  pollTimer = setInterval(async () => {
    if (!task.value) return
    try {
      const { data } = await aiApi.getAnalysisTask(task.value.id)
      task.value = data
      if (data.status === 'COMPLETED' || data.status === 'FAILED') {
        stopPolling()
        if (data.status === 'COMPLETED') ElMessage.success('分析报告已生成')
        else ElMessage.error('生成失败：' + (data.error || '未知错误'))
      }
    } catch {
      stopPolling()
    }
  }, 1500)
}

function stopPolling() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}

async function reloadHistory() {
  try {
    const { data } = await aiApi.listAnalysisHistory(10)
    history.value = data || []
  } catch { /* ignore */ }
}

async function openHistory(id) {
  try {
    const { data } = await aiApi.getAnalysisTask(id)
    task.value = data
    if (data.status === 'PENDING' || data.status === 'RUNNING') startPolling()
  } catch { ElMessage.error('加载报告失败') }
}

const radarOption = computed(() => {
  const ra = task.value?.charts?.radar || { indicators: [], values: [], targets: [] }
  const raw = ra.indicators && ra.indicators.length
    ? ra.indicators : ['睡眠', '运动', '饮水', '饮食', '心情']
  const indicators = raw.map((it) => typeof it === 'string'
    ? { name: it, max: 100 } : { name: it.name, max: it.max || 100 })
  const dims = indicators.length
  const toArr = (arr) => { const a = Array.isArray(arr) ? arr.slice(0, dims) : []; while (a.length < dims) a.push(0); return a }
  return {
    tooltip: { trigger: 'item', backgroundColor: 'rgba(255,255,255,0.92)', borderWidth: 1, borderColor: 'rgba(102,126,234,0.25)', textStyle: { color: '#1f2440' } },
    legend: { data: ['实际', '目标'], textStyle: { color: '#5b6178' }, bottom: 0 },
    radar: {
      indicator: indicators, radius: '64%',
      axisName: { color: '#5b6178', fontSize: 12 },
      splitLine: { lineStyle: { color: 'rgba(102,126,234,0.12)' } },
      splitArea: { areaStyle: { color: ['rgba(102,126,234,0.04)', 'rgba(118,75,162,0.05)'] } },
      axisLine: { lineStyle: { color: 'rgba(102,126,234,0.18)' } },
    },
    series: [{
      type: 'radar',
      data: [
        { value: toArr(ra.values), name: '实际', itemStyle: { color: '#667eea' }, areaStyle: { color: 'rgba(102,126,234,0.28)' }, lineStyle: { width: 2.5, color: '#667eea' } },
        { value: toArr(ra.targets), name: '目标', itemStyle: { color: '#ec4899' }, lineStyle: { type: 'dashed', color: '#ec4899' } },
      ],
    }],
  }
})

const trendOption = computed(() => {
  const tr = task.value?.charts?.trend || {}
  const dates = tr.dates || []
  const mk = (key, name, color) => ({
    name, type: 'line', smooth: true, data: tr[key] || [],
    itemStyle: { color }, lineStyle: { width: 2.5, color }, areaStyle: { color: color + '28' }, symbol: 'circle', symbolSize: 5,
  })
  return {
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(255,255,255,0.92)', borderWidth: 1, borderColor: 'rgba(102,126,234,0.25)', textStyle: { color: '#1f2440' } },
    legend: { data: ['睡眠', '运动', '饮水'], textStyle: { color: '#5b6178' }, bottom: 0 },
    grid: { left: 40, right: 16, top: 20, bottom: 36 },
    xAxis: { type: 'category', data: dates, axisLabel: { color: '#94a3b8' }, axisLine: { lineStyle: { color: '#dbe0f0' } } },
    yAxis: { type: 'value', axisLabel: { color: '#94a3b8' }, splitLine: { lineStyle: { color: 'rgba(102,126,234,0.1)' } } },
    series: [mk('sleep', '睡眠', '#6366f1'), mk('exercise', '运动', '#f59e0b'), mk('water', '饮水', '#22d3ee')],
  }
})

const statusText = computed(() => {
  const s = task.value?.status
  return { PENDING: '排队中', RUNNING: '生成中', COMPLETED: '已完成', FAILED: '失败' }[s] || s || '-'
})

onMounted(reloadHistory)
onBeforeUnmount(stopPolling)
</script>

<template>
  <div class="max-w-6xl mx-auto px-5 py-6 relative z-10">
    <div class="flex items-center gap-2 mb-1">
      <BrainCircuit class="text-brand" />
      <h1 class="text-2xl font-semibold text-slate-800">AI 智能分析</h1>
    </div>
    <p class="text-sm text-slate-500 mb-5">基于真实打卡数据，由多智能体协同生成可视化分析报告与可执行建议。</p>

    <!-- 控制区 -->
    <section class="glass rounded-card-xl p-5 mb-6 flex flex-wrap items-center gap-4">
      <div class="flex items-center gap-2">
        <span class="text-sm text-slate-500">分析周期</span>
        <select v-model.number="days" class="px-3 py-2 rounded-lg border border-slate-200 text-sm outline-none focus:border-indigo-300">
          <option :value="7">近 7 天</option>
          <option :value="14">近 14 天</option>
          <option :value="30">近 30 天</option>
        </select>
      </div>
      <button
        class="px-4 py-2 rounded-lg text-sm text-white bg-grad-primary disabled:opacity-50 flex items-center gap-1.5"
        :disabled="generating"
        @click="generate"
      >
        <Loader2 v-if="generating" :size="15" class="animate-spin" />
        <BrainCircuit v-else :size="15" />
        {{ generating ? '提交中…' : '生成分析报告' }}
      </button>
      <button
        v-if="task"
        class="px-3 py-2 rounded-lg text-sm text-slate-500 hover:bg-slate-100 flex items-center gap-1.5"
        @click="generate"
      >
        <RefreshCw :size="14" /> 重新生成
      </button>
      <span v-if="task" class="text-xs px-2 py-0.5 rounded-full"
        :class="task.status === 'COMPLETED' ? 'bg-emerald-100 text-emerald-600'
          : task.status === 'FAILED' ? 'bg-rose-100 text-rose-600'
          : 'bg-indigo-100 text-indigo-600'">
        {{ statusText }}
      </span>
    </section>

    <!-- 报告区 -->
    <div v-if="task" class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
      <div class="glass rounded-card-xl p-5">
        <div class="flex items-center gap-2 mb-3 text-slate-700 font-medium"><Radar class="text-brand-soft" /> 能力画像</div>
        <VChart v-if="task.charts?.radar" :option="radarOption" autoresize style="height: 260px" />
        <div v-else class="text-slate-400 text-sm h-[200px] flex items-center justify-center">图表生成中…</div>
      </div>
      <div class="glass rounded-card-xl p-5">
        <div class="flex items-center gap-2 mb-3 text-slate-700 font-medium"><TrendingUp class="text-brand" /> 多维趋势</div>
        <VChart v-if="task.charts?.trend" :option="trendOption" autoresize style="height: 260px" />
        <div v-else class="text-slate-400 text-sm h-[200px] flex items-center justify-center">图表生成中…</div>
      </div>
    </div>

    <div v-if="task && task.tags?.length" class="flex flex-wrap gap-2 mb-6">
      <span v-for="t in task.tags" :key="t" class="text-xs px-2.5 py-1 rounded-full bg-indigo-50 text-indigo-600">{{ t }}</span>
    </div>

    <div v-if="task && task.report" class="glass rounded-card-xl p-6 mb-6 prose prose-slate max-w-none">
      <div class="flex items-center gap-2 mb-3 text-slate-700 font-medium"><FileText class="text-brand" /> AI 分析报告</div>
      <pre class="whitespace-pre-wrap text-sm leading-relaxed text-slate-700 font-sans">{{ task.report }}</pre>
    </div>

    <!-- 历史列表 -->
    <section class="glass rounded-card-xl p-5">
      <div class="flex items-center gap-2 mb-3 text-slate-700 font-medium"><Clock class="text-brand" /> 历史报告</div>
      <div v-if="history.length" class="space-y-2">
        <button
          v-for="h in history" :key="h.id"
          class="w-full text-left flex items-center justify-between px-4 py-3 rounded-xl border border-slate-100 hover:border-indigo-200 hover:bg-indigo-50/40 transition-colors"
          @click="openHistory(h.id)"
        >
          <div>
            <div class="font-medium text-slate-700">{{ h.title }}</div>
            <div class="text-xs text-slate-400">{{ h.createTime }}</div>
          </div>
          <span class="text-xs px-2 py-0.5 rounded-full"
            :class="h.status === 'COMPLETED' ? 'bg-emerald-100 text-emerald-600'
              : h.status === 'FAILED' ? 'bg-rose-100 text-rose-600'
              : 'bg-indigo-100 text-indigo-600'">{{ statusText }}</span>
        </button>
      </div>
      <div v-else class="text-slate-400 text-sm text-center py-4">还没有历史报告</div>
    </section>
  </div>
</template>

<script>
import { TrendingUp, Radar } from 'lucide-vue-next'
export default { components: { TrendingUp, Radar } }
</script>
