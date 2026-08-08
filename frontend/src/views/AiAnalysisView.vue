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
          <p class="mt-2 text-xs text-slate-400">{{ task.createTime || task.createdAt ? new Date(task.createTime || task.createdAt).toLocaleString() : '' }}</p>
        </div>

        <div class="glass-card p-5">
          <div class="flex items-center justify-between mb-2">
            <div class="flex items-center gap-2 text-blue-600 font-medium">
              <el-icon><Sunny /></el-icon> 今日点评
            </div>
            <el-button size="small" type="primary" plain :icon="MagicStick" :loading="generating" :disabled="generating" @click="generate">AI 分析</el-button>
          </div>
          <p class="text-sm text-slate-600 leading-relaxed">{{ task.dailyEvaluation || '暂无' }}</p>
        </div>

        <div class="glass-card p-5">
          <div class="flex items-center justify-between mb-2">
            <div class="flex items-center gap-2 text-indigo-600 font-medium">
              <el-icon><TrendCharts /></el-icon> 趋势总结
            </div>
            <el-button size="small" type="primary" plain :icon="MagicStick" :loading="generating" :disabled="generating" @click="generate">AI 分析</el-button>
          </div>
          <p class="text-sm text-slate-600 leading-relaxed">{{ task.trendSummary || '暂无' }}</p>
        </div>
      </div>

      <div class="glass-card p-5">
        <div class="flex items-center justify-between mb-2">
          <div class="flex items-center gap-2 text-amber-600 font-medium">
            <el-icon><Warning /></el-icon> 风险提示
          </div>
          <el-button size="small" type="primary" plain :icon="MagicStick" :loading="generating" :disabled="generating" @click="generate">AI 分析</el-button>
        </div>
        <p class="text-sm text-slate-600 leading-relaxed">{{ task.riskWarning || '暂无风险' }}</p>
      </div>

      <div class="glass-card p-5">
        <div class="flex items-center justify-between mb-2">
          <div class="flex items-center gap-2 text-emerald-600 font-medium">
            <el-icon><Star /></el-icon> 改进建议
          </div>
          <el-button size="small" type="primary" plain :icon="MagicStick" :loading="generating" :disabled="generating" @click="generate">AI 分析</el-button>
        </div>
        <ul class="list-disc list-inside space-y-1 text-sm text-slate-600">
          <li v-for="(s, i) in suggestionList" :key="i">{{ s }}</li>
          <li v-if="!suggestionList.length" class="list-none text-slate-400">暂无建议</li>
        </ul>
      </div>

      <!-- 综合概览统计卡 -->
      <div v-if="overview" class="grid grid-cols-2 md:grid-cols-5 gap-3">
        <div class="glass-card p-4 flex flex-col items-center justify-center text-center">
          <span class="text-xs text-slate-400">打卡天数</span>
          <span class="mt-1 text-2xl font-semibold text-slate-800">{{ overview.recordDays ?? '—' }}<small class="ml-0.5 text-xs text-slate-400">天</small></span>
        </div>
        <div class="glass-card p-4 flex flex-col items-center justify-center text-center">
          <span class="text-xs text-slate-400">平均睡眠</span>
          <span class="mt-1 text-2xl font-semibold text-indigo-600">{{ overview.avgSleep ?? '—' }}<small class="ml-0.5 text-xs text-slate-400">h</small></span>
        </div>
        <div class="glass-card p-4 flex flex-col items-center justify-center text-center">
          <span class="text-xs text-slate-400">平均运动</span>
          <span class="mt-1 text-2xl font-semibold text-cyan-500">{{ overview.avgExercise ?? '—' }}<small class="ml-0.5 text-xs text-slate-400">min</small></span>
        </div>
        <div class="glass-card p-4 flex flex-col items-center justify-center text-center">
          <span class="text-xs text-slate-400">平均饮水</span>
          <span class="mt-1 text-2xl font-semibold text-emerald-500">{{ overview.avgWater ?? '—' }}<small class="ml-0.5 text-xs text-slate-400">ml</small></span>
        </div>
        <div class="glass-card p-4 flex flex-col items-center justify-center text-center">
          <span class="text-xs text-slate-400">平均心情</span>
          <span class="mt-1 text-2xl font-semibold text-amber-500">{{ overview.avgMood ?? '—' }}<small class="ml-0.5 text-xs text-slate-400">/5</small></span>
        </div>
      </div>

      <!-- 各维度达标率明细 -->
      <div v-if="achievementDimensions.length" class="glass-card p-5">
        <div class="flex items-center justify-between mb-3">
          <div class="flex items-center gap-2 text-violet-600 font-medium">
            <el-icon><DataAnalysis /></el-icon> 各维度达标率明细
          </div>
          <el-button size="small" type="primary" plain :icon="MagicStick" :loading="generating" :disabled="generating" @click="generate">AI 分析</el-button>
        </div>
        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead>
              <tr class="text-slate-400 text-left">
                <th class="py-2 pr-4 font-medium">维度</th>
                <th class="py-2 pr-4 font-medium">实际</th>
                <th class="py-2 pr-4 font-medium">目标</th>
                <th class="py-2 pr-4 font-medium">达标率</th>
                <th class="py-2 font-medium">进度</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(d, i) in achievementDimensions" :key="i" class="border-t border-white/40 hover:bg-white/30 transition-colors">
                <td class="py-2 pr-4 text-slate-700">{{ d.label }}</td>
                <td class="py-2 pr-4 text-slate-600">{{ d.actual ?? '—' }}</td>
                <td class="py-2 pr-4 text-slate-600">{{ d.target ?? '—' }}</td>
                <td class="py-2 pr-4">
                  <span :class="Number(d.rate) >= 100 ? 'text-emerald-600' : 'text-amber-600'" class="font-medium">{{ d.rate }}%</span>
                </td>
                <td class="py-2">
                  <div class="h-2 w-28 rounded-full bg-slate-200/70 overflow-hidden">
                    <div class="h-full rounded-full bg-gradient-to-r from-indigo-400 to-cyan-400"
                         :style="{ width: Math.min(100, Number(d.rate) || 0) + '%' }"></div>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 图表区（固定高度包裹层，避免 VChart 自适应无限拉长） -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div class="glass-card p-4">
          <div class="flex items-center justify-between mb-2">
            <h3 class="text-sm font-medium text-slate-700">趋势分析</h3>
            <el-button size="small" type="primary" plain :icon="MagicStick" :loading="generating" :disabled="generating" @click="generate">AI 分析</el-button>
          </div>
          <div v-if="hasTrend" class="h-[320px]">
            <VChart autoresize :option="trendOption" style="height:100%;width:100%" />
          </div>
          <div v-else class="h-[320px] flex items-center justify-center text-slate-400 text-sm">图表生成中…</div>
        </div>
        <div class="glass-card p-4">
          <div class="flex items-center justify-between mb-2">
            <h3 class="text-sm font-medium text-slate-700">维度达成率</h3>
            <el-button size="small" type="primary" plain :icon="MagicStick" :loading="generating" :disabled="generating" @click="generate">AI 分析</el-button>
          </div>
          <div v-if="hasAchievement" class="h-[320px]">
            <VChart autoresize :option="achievementOption" style="height:100%;width:100%" />
          </div>
          <div v-else class="h-[320px] flex items-center justify-center text-slate-400 text-sm">图表生成中…</div>
        </div>
        <div class="glass-card p-4 lg:col-span-2">
          <div class="flex items-center justify-between mb-2">
            <h3 class="text-sm font-medium text-slate-700">能力雷达</h3>
            <el-button size="small" type="primary" plain :icon="MagicStick" :loading="generating" :disabled="generating" @click="generate">AI 分析</el-button>
          </div>
          <div v-if="hasRadar" class="h-[360px]">
            <VChart autoresize :option="radarOption" style="height:100%;width:100%" />
          </div>
          <div v-else class="h-[360px] flex items-center justify-center text-slate-400 text-sm">图表生成中…</div>
        </div>
      </div>

      <div class="glass-card p-5">
        <div class="flex items-center justify-between mb-2">
          <div class="text-sm font-medium text-slate-700">完整报告</div>
          <el-button size="small" type="primary" plain :icon="MagicStick" :loading="generating" :disabled="generating" @click="generate">AI 分析</el-button>
        </div>
        <pre class="whitespace-pre-wrap text-sm text-slate-600 leading-relaxed">{{ task.report || '暂无报告内容' }}</pre>
      </div>

      <!-- 历史 -->
      <div class="glass-card p-5" v-if="history.length">
        <div class="text-sm font-medium text-slate-700 mb-3">历史报告</div>
        <div class="flex flex-wrap gap-2">
          <button v-for="h in history" :key="h.id"
            class="px-3 py-1.5 rounded-full text-xs glass-card-soft text-slate-600 hover:text-blue-600 transition"
            @click="selectHistory(h.id)">
            {{ h.createTime || h.createdAt ? new Date(h.createTime || h.createdAt).toLocaleDateString() : '报告' }}
          </button>
        </div>
      </div>
    </template>

    <!-- 无报告引导 / 生成失败兜底 -->
    <div v-else class="glass-card p-10 text-center">
      <el-icon v-if="!generateError" class="text-4xl text-blue-400"><DataAnalysis /></el-icon>
      <el-icon v-else class="text-4xl text-rose-400"><Warning /></el-icon>
      <h3 class="mt-3 text-lg font-medium text-slate-700">
        {{ generateError ? '生成失败' : '还没有分析报告' }}
      </h3>
      <p class="mt-1 text-sm text-slate-500">
        {{ generateError || '基于你近 7 天的打卡、目标与习惯数据，一键生成专属分析报告' }}
      </p>
      <el-button type="primary" class="mt-4" :icon="MagicStick" :loading="generating" :disabled="generating" @click="generate">
        {{ generateError ? '重新生成' : '生成我的分析报告' }}
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

const suggestionList = computed(() => {
  const v = task.value?.suggestion
  if (Array.isArray(v)) {
    return v.map(s => String(s).trim()).filter(Boolean)
  }
  return String(v || '').split('\n').map(s => s.trim()).filter(Boolean)
})

const hasTrend = computed(() => !!task.value?.charts?.trend)
const hasAchievement = computed(() => !!task.value?.charts?.achievement)
const hasRadar = computed(() => !!task.value?.charts?.radar)

const overview = computed(() => task.value?.charts?.overview || null)
const achievementDimensions = computed(() =>
  toArr(task.value?.charts?.achievement?.dimensions)
)

const trendOption = computed(() => {
  const t = task.value?.charts?.trend || {}
  const series = [
    { name: '睡眠(h)', type: 'line', smooth: true, showSymbol: false, data: toArr(t.sleep), itemStyle: { color: '#667EEA' } },
    { name: '运动(min)', type: 'line', smooth: true, showSymbol: false, data: toArr(t.exercise), itemStyle: { color: '#22D3EE' } },
    { name: '饮水(ml)', type: 'line', smooth: true, showSymbol: false, data: toArr(t.water), itemStyle: { color: '#10B981' } },
    { name: '心情', type: 'line', smooth: true, showSymbol: false, data: toArr(t.mood), itemStyle: { color: '#F59E0B' } }
  ]
  for (const c of toArr(t.customSeries)) {
    series.push({
      name: c?.name || '自定义目标',
      type: 'line', smooth: true, showSymbol: false,
      data: toArr(c?.data),
      itemStyle: { color: c?.colorFrom || '#764BA2' },
      lineStyle: { type: 'dashed' }
    })
  }
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 44, right: 16, top: 36, bottom: 28 },
    legend: { data: series.map(s => s.name), top: 0, type: 'scroll' },
    xAxis: { type: 'category', boundaryGap: false, data: toArr(t.dates) },
    yAxis: { type: 'value' },
    series
  }
})

const achievementOption = computed(() => {
  const dims = task.value?.charts?.achievement?.dimensions || []
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (p) => {
        const d = dims[p?.[0]?.dataIndex]
        if (!d) return ''
        return `${d.label}<br/>实际: ${d.actual} / 目标: ${d.target}<br/>达标率: ${d.rate}%`
      }
    },
    grid: { left: 90, right: 48, top: 16, bottom: 16 },
    xAxis: { type: 'value', max: 100 },
    yAxis: { type: 'category', data: dims.map(d => d?.label || '') },
    series: [{
      type: 'bar',
      data: dims.map(d => Number(d?.rate ?? 0)),
      barWidth: '55%',
      itemStyle: { color: '#667EEA', borderRadius: [0, 6, 6, 0] },
      label: { show: true, position: 'right', formatter: '{c}%', color: '#5B6178' }
    }]
  }
})

const radarOption = computed(() => {
  const r = task.value?.charts?.radar || {}
  const indicators = toArr(r.indicators).map(it => ({ name: it?.name || '', max: it?.max ?? 100 }))
  const values = toArr(r.values).map(v => v ?? 0)
  const targets = toArr(r.targets).map(v => v ?? 0)
  return {
    tooltip: {},
    legend: { data: ['实际', '目标'], bottom: 0 },
    radar: {
      indicator: indicators.length ? indicators : [{ name: '默认', max: 100 }],
      radius: '65%'
    },
    series: [{
      type: 'radar',
      data: [
        { value: values.length ? values : [0], name: '实际', itemStyle: { color: '#667EEA' }, areaStyle: { opacity: 0.25 } },
        { value: targets.length ? targets : [0], name: '目标', itemStyle: { color: '#22D3EE' }, areaStyle: { opacity: 0.1 } }
      ]
    }]
  }
})

async function generate() {
  generating.value = true
  generateError.value = ''
  try {
    const res = await generateAnalysis(7)
    // 后端 generate 返回整个 Task 实体，经 request 拦截器解包后 res 即 Task 本体；兼容 {taskId} 形态
    const id = res?.id ?? res?.taskId
            ?? (res?.data && (res.data.id ?? res.data.taskId))
    if (id) {
      await poll(id)
    } else {
      await loadLatest()
    }
  } catch (e) {
    generateError.value = e?.message || '生成请求失败（请确认后端服务已启动）'
    console.error('[AiAnalysisView] 生成失败:', e)
  } finally {
    generating.value = false
  }
}

async function poll(id) {
  for (let i = 0; i < 40; i++) {
    const res = await getAnalysisTask(id)
    // request 拦截器已解包为 data 本体（即 Task 实体）
    const t = res
    if (t?.status === 'COMPLETED') { task.value = t; return }
    if (t?.status === 'FAILED') {
      generateError.value = t?.error || '分析生成失败，请稍后重试'
      return
    }
    await new Promise(r => setTimeout(r, 1500))
  }
  // 轮询 40 次（约 60s）仍非 COMPLETED，给出超时提示，避免无限等待
  generateError.value = '分析生成超时，请稍后重试'
  task.value = null
}

async function loadLatest() {
  const res = await getLatestAnalysis()
  const t = res // 已是解包后的 Task 本体
  if (t && t.id) task.value = t
  else task.value = null
}

// 生成失败时置位，供页面显示重试引导
const generateError = ref('')

async function reloadHistory() {
  const res = await listAnalysisHistory(10)
  history.value = res ?? []
}

async function selectHistory(id) {
  const res = await getAnalysisTask(id)
  task.value = res // 已是解包后的 Task 本体
}

function init() {
  reloadHistory()
  loadLatest().then(() => {
    // 无历史报告时自动触发一次生成，让页面一进来就有内容
    if (!task.value) generate()
  })
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
