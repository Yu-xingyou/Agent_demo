<script setup>
import { ref, onMounted, computed } from 'vue'
import { History, CalendarDays, Table, Moon, Dumbbell, Droplets } from 'lucide-vue-next'
import * as habitApi from '@/api/habit'
import * as goalApi from '@/api/goal'
import { MOOD_LABELS, MOOD_EMOJIS } from '@/constants/theme'
import { ElMessage } from 'element-plus'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { TooltipComponent, GridComponent } from 'echarts/components'

use([CanvasRenderer, LineChart, TooltipComponent, GridComponent])

const loading = ref(false)
const rows = ref([])
const viewMode = ref('table')
const customGoals = ref([])
const customByDate = ref({})

function localDate(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}
function lastNDays(n) {
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - (n - 1))
  return { startDate: localDate(start), endDate: localDate(end) }
}

async function load() {
  loading.value = true
  try {
    const { startDate, endDate } = lastNDays(30)
    const [recs, goals] = await Promise.all([
      habitApi.listByRange(startDate, endDate).catch(() => []),
      goalApi.getActiveWithCustom().catch(() => []),
    ])
    rows.value = recs || []
    const cust = (goals || []).filter((g) => g.goalType === 'CUSTOM')
    customGoals.value = cust

    if (cust.length) {
      const cr = await goalApi.listRecordsByRange(startDate, endDate).catch(() => [])
      const map = {}
      ;(cr || []).forEach((r) => {
        if (!map[r.recordDate]) map[r.recordDate] = {}
        map[r.recordDate][r.goalId] = r
      })
      customByDate.value = map
    }
  } catch (e) {
    ElMessage.error('加载历史记录失败')
  } finally {
    loading.value = false
  }
}
onMounted(load)

const sortedDesc = computed(() => [...rows.value].sort((a, b) => b.recordDate.localeCompare(a.recordDate)))

const statCards = computed(() => {
  const list = rows.value
  const n = list.length || 1
  const sum = (sel) => list.reduce((s, r) => s + (sel(r) || 0), 0)
  return [
    { title: '打卡天数', value: list.length, grad: 'bg-grad-primary' },
    { title: '平均睡眠', value: list.length ? (sum((r) => r.sleepDuration) / n).toFixed(1) + 'h' : '-', grad: 'bg-grad-sleep' },
    { title: '平均运动', value: list.length ? Math.round(sum((r) => r.exerciseDuration) / n) + 'min' : '-', grad: 'bg-grad-exercise' },
    { title: '平均饮水', value: list.length ? Math.round(sum((r) => r.waterIntake) / n) + 'ml' : '-', grad: 'bg-grad-water' },
  ]
})

function spark(dates, key) {
  return {
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(43,42,40,0.85)', borderWidth: 0, textStyle: { color: '#fff' } },
    grid: { left: 2, right: 2, top: 6, bottom: 2 },
    xAxis: { type: 'category', show: false, data: dates },
    yAxis: { type: 'value', show: false },
    series: [{
      type: 'line', smooth: true, symbol: 'none', data: dates.map((d) => {
        const r = rows.value.find((x) => x.recordDate === d)
        return r ? r[key] : null
      }),
    }],
  }
}
const sparkSleep = computed(() => spark(sortedDesc.value.map((r) => r.recordDate), 'sleepDuration'))
const sparkExercise = computed(() => spark(sortedDesc.value.map((r) => r.recordDate), 'exerciseDuration'))
const sparkWater = computed(() => spark(sortedDesc.value.map((r) => r.recordDate), 'waterIntake'))

// 日历卡片：最近30天
const calendarCards = computed(() => {
  const { startDate, endDate } = lastNDays(30)
  const startD = new Date(startDate)
  const endD = new Date(endDate)
  const cards = []
  for (let d = new Date(startD); d <= endD; d.setDate(d.getDate() + 1)) {
    const ds = localDate(new Date(d))
    const row = rows.value.find((r) => r.recordDate === ds)
    cards.push({ date: ds, row })
  }
  return cards.reverse()
})

function customVal(date, goalId) {
  const m = customByDate.value[date]
  return m && m[goalId] ? m[goalId].value : null
}
</script>

<template>
  <div class="max-w-5xl mx-auto px-5 py-6 relative z-10">
    <div class="flex items-center justify-between mb-1">
      <div class="flex items-center gap-2">
        <History class="text-brand" />
        <h1 class="text-2xl font-semibold text-slate-800">历史记录</h1>
      </div>
      <div class="flex items-center gap-1 glass rounded-xl p-1">
        <button class="px-3 py-1.5 rounded-lg text-sm flex items-center gap-1 transition-colors"
          :class="viewMode === 'table' ? 'bg-grad-primary text-white' : 'text-slate-500'"
          @click="viewMode = 'table'"><Table :size="14" /> 表格</button>
        <button class="px-3 py-1.5 rounded-lg text-sm flex items-center gap-1 transition-colors"
          :class="viewMode === 'calendar' ? 'bg-grad-primary text-white' : 'text-slate-500'"
          @click="viewMode = 'calendar'"><CalendarDays :size="14" /> 日历</button>
      </div>
    </div>
    <p class="text-sm text-slate-500 mb-6">最近 30 天的生活习惯打卡明细与趋势</p>

    <!-- 概览卡 -->
    <section class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
      <div v-for="s in statCards" :key="s.title" class="glass rounded-card-xl p-4">
        <div class="h-7 w-7 rounded-lg flex items-center justify-center text-white shadow mb-2" :class="s.grad"></div>
        <div class="text-xl font-bold text-slate-800">{{ s.value }}</div>
        <div class="text-xs text-slate-400 mt-1">{{ s.title }}</div>
      </div>
    </section>

    <!-- 维度迷你趋势 -->
    <section class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
      <div class="glass rounded-card-xl p-4">
        <div class="flex items-center gap-2 text-slate-600 text-sm mb-1"><Moon class="text-brand" /> 睡眠趋势</div>
        <VChart :option="sparkSleep" autoresize style="height: 56px" />
      </div>
      <div class="glass rounded-card-xl p-4">
        <div class="flex items-center gap-2 text-slate-600 text-sm mb-1"><Dumbbell class="text-amber-500" /> 运动趋势</div>
        <VChart :option="sparkExercise" autoresize style="height: 56px" />
      </div>
      <div class="glass rounded-card-xl p-4">
        <div class="flex items-center gap-2 text-slate-600 text-sm mb-1"><Droplets class="text-brand" /> 饮水趋势</div>
        <VChart :option="sparkWater" autoresize style="height: 56px" />
      </div>
    </section>

    <!-- 表格视图 -->
    <div v-if="viewMode === 'table'" class="glass rounded-card-xl p-5">
      <el-table :data="rows" v-loading="loading" stripe style="width: 100%"
        :header-cell-style="{ color: '#64748b', fontWeight: 600 }">
        <el-table-column prop="recordDate" label="日期" width="110" />
        <el-table-column label="睡眠" width="90">
          <template #default="{ row }">{{ row.sleepDuration ? row.sleepDuration + 'h' : '-' }}</template>
        </el-table-column>
        <el-table-column label="运动" width="90">
          <template #default="{ row }">{{ row.exerciseDuration ? row.exerciseDuration + 'min' : '-' }}</template>
        </el-table-column>
        <el-table-column label="饮水" width="90">
          <template #default="{ row }">{{ row.waterIntake ? row.waterIntake + 'ml' : '-' }}</template>
        </el-table-column>
        <el-table-column label="饮食" width="70">
          <template #default="{ row }">{{ row.dietScore ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="心情" width="80">
          <template #default="{ row }">
            <span v-if="row.mood" class="text-xl leading-none">{{ MOOD_EMOJIS[row.mood] }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <!-- 自定义目标动态列 -->
        <el-table-column v-for="g in customGoals" :key="g.id" :label="g.displayName" min-width="90">
          <template #default="{ row }">
            <span v-if="customVal(row.recordDate, g.id) != null">{{ customVal(row.recordDate, g.id) }}{{ g.unit || '' }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="$router.push('/checkin?date=' + row.recordDate)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && !rows.length" class="text-center text-slate-400 py-10">暂无记录，去打卡页开始吧</div>
    </div>

    <!-- 日历卡片视图 -->
    <div v-else class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3">
      <div v-for="c in calendarCards" :key="c.date"
        class="glass rounded-card-xl p-3 transition-all hover:shadow-card-hover"
        :class="c.row ? '' : 'opacity-60'">
        <div class="text-xs text-slate-400 mb-2">{{ c.date.slice(5) }}</div>
        <template v-if="c.row">
          <div class="flex items-center gap-1.5 mb-1.5">
            <span v-if="c.row.mood" class="text-base leading-none">{{ MOOD_EMOJIS[c.row.mood] }}</span>
            <span class="text-sm font-medium text-slate-700">{{ MOOD_LABELS[c.row.mood] || '' }}</span>
          </div>
          <div class="text-xs text-slate-500 space-y-0.5">
            <div>睡 {{ c.row.sleepDuration || '-' }}h</div>
            <div>动 {{ c.row.exerciseDuration || '-' }}min</div>
            <div>水 {{ c.row.waterIntake || '-' }}ml</div>
            <div v-for="g in customGoals" :key="g.id" class="text-brand-soft">
              {{ g.displayName }}: {{ customVal(c.date, g.id) != null ? customVal(c.date, g.id) + (g.unit || '') : '-' }}
            </div>
          </div>
        </template>
        <div v-else class="text-xs text-slate-400">未打卡</div>
      </div>
    </div>
  </div>
</template>
