<script setup>
import { ref, onMounted, computed } from 'vue'
import { History, CalendarDays, Table, Moon, Dumbbell, Droplets, PenLine, Apple, Target } from 'lucide-vue-next'
import * as habitApi from '@/api/habit'
import * as goalApi from '@/api/goal'
import { MOOD_LABELS, MOOD_EMOJIS, goalColor } from '@/constants/theme'
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

// 补签弹窗状态
const makeupVisible = ref(false)
const makeupDate = ref('')
const makeupSubmitting = ref(false)
const makeupForm = ref(emptyMakeupForm())
const makeupCustomGoals = ref([])
const makeupCustomInputs = ref({})

function emptyMakeupForm() {
  return {
    recordDate: '',
    sleepTime: '',
    wakeTime: '',
    sleepQuality: null,
    dietDesc: '',
    dietScore: null,
    exerciseType: '',
    exerciseDuration: null,
    waterIntake: null,
    mood: null,
    remark: '',
  }
}

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
const todayStr = computed(() => localDate(new Date()))
const calendarCards = computed(() => {
  const { startDate, endDate } = lastNDays(30)
  const startD = new Date(startDate)
  const endD = new Date(endDate)
  const cards = []
  for (let d = new Date(startD); d <= endD; d.setDate(d.getDate() + 1)) {
    const ds = localDate(new Date(d))
    const row = rows.value.find((r) => r.recordDate === ds)
    cards.push({ date: ds, row, isPast: ds < todayStr.value })
  }
  return cards.reverse()
})

function customVal(date, goalId) {
  const m = customByDate.value[date]
  return m && m[goalId] ? m[goalId].value : null
}

// 补签相关
async function openMakeup(date) {
  makeupDate.value = date
  makeupForm.value = emptyMakeupForm()
  makeupForm.value.recordDate = date
  makeupVisible.value = true
  await loadMakeupCustomGoals(date)
}

async function loadMakeupCustomGoals(date) {
  try {
    const goals = (await goalApi.getActiveWithCustom()).filter((g) => g.goalType === 'CUSTOM')
    makeupCustomGoals.value = goals
    const recs = await goalApi.listRecordsByDate(date).catch(() => [])
    const byGoal = {}
    ;(recs || []).forEach((r) => { byGoal[r.goalId] = r })
    const inputs = {}
    goals.forEach((g) => {
      const rec = byGoal[g.id]
      inputs[g.id] = {
        value: rec && rec.value != null ? rec.value : null,
        remark: rec && rec.remark ? rec.remark : '',
      }
    })
    makeupCustomInputs.value = inputs
  } catch (e) {
    /* 忽略 */
  }
}

function setMakeupCustomInput(id, field, val) {
  if (!makeupCustomInputs.value[id]) makeupCustomInputs.value[id] = { value: null, remark: '' }
  makeupCustomInputs.value[id][field] = val
}

async function submitMakeup() {
  makeupSubmitting.value = true
  try {
    await habitApi.saveOrUpdate({ ...makeupForm.value })
    let savedCount = 0
    for (const g of makeupCustomGoals.value) {
      const inp = makeupCustomInputs.value[g.id]
      if (inp && inp.value != null && inp.value !== '') {
        await goalApi.save({
          goalId: g.id,
          goalType: 'CUSTOM',
          recordDate: makeupForm.value.recordDate,
          value: Number(inp.value),
          remark: inp.remark || '',
        })
        savedCount++
      }
    }
    ElMessage.success(savedCount ? `补签成功，${savedCount} 项自定义目标已保存` : '补签成功')
    makeupVisible.value = false
    await load()
  } catch (e) {
    /* 错误已由拦截器提示 */
  } finally {
    makeupSubmitting.value = false
  }
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
        <button v-if="!c.row && c.isPast" type="button" @click="openMakeup(c.date)"
          class="mt-3 w-full py-1.5 rounded-lg text-xs font-medium border border-brand/60 text-brand-soft bg-transparent
                 hover:bg-gradient-to-r hover:from-[#667EEA] hover:to-[#6F9A8A] hover:text-white hover:border-transparent
                 transition-all duration-200 flex items-center justify-center gap-1 cursor-pointer">
          <PenLine :size="13" /> 补签
        </button>
      </div>
    </div>

    <!-- 补签弹窗 -->
    <el-dialog v-model="makeupVisible" :title="null" width="92%" max-width="640px" align-center
      class="makeup-dialog" :close-on-click-modal="false">
      <template #header>
        <div class="flex items-center gap-2">
          <PenLine class="text-brand" :size="18" />
          <div>
            <div class="text-lg font-semibold text-slate-800">补签 · {{ makeupDate }}</div>
            <div class="text-xs text-slate-400 mt-0.5">为过去的某天补录打卡记录</div>
          </div>
        </div>
      </template>

      <div class="space-y-4 max-h-[68vh] overflow-y-auto pr-1">
        <!-- 睡眠 -->
        <div class="glass rounded-card-xl p-4 section-bar" style="border-color: #6f7a99">
          <div class="flex items-center gap-2 mb-3 text-slate-700 font-medium text-sm">
            <Moon class="text-indigo-500" :size="16" /> 睡眠
          </div>
          <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <label class="text-xs text-slate-500">入睡时间
              <input v-model="makeupForm.sleepTime" type="time" class="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 outline-none focus:border-brand bg-white" />
            </label>
            <label class="text-xs text-slate-500">起床时间
              <input v-model="makeupForm.wakeTime" type="time" class="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 outline-none focus:border-brand bg-white" />
            </label>
            <label class="text-xs text-slate-500">睡眠质量 (1-5)
              <input v-model.number="makeupForm.sleepQuality" type="number" min="1" max="5" class="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 outline-none focus:border-brand bg-white" />
            </label>
          </div>
        </div>

        <!-- 饮食 -->
        <div class="glass rounded-card-xl p-4 section-bar" style="border-color: #6f9a8a">
          <div class="flex items-center gap-2 mb-3 text-slate-700 font-medium text-sm">
            <Apple class="text-teal-600" :size="16" /> 饮食
          </div>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <label class="text-xs text-slate-500">饮食描述
              <input v-model="makeupForm.dietDesc" type="text" placeholder="那天吃了什么？" class="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 outline-none focus:border-brand bg-white" />
            </label>
            <label class="text-xs text-slate-500">饮食评分 (1-5)
              <input v-model.number="makeupForm.dietScore" type="number" min="1" max="5" class="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 outline-none focus:border-brand bg-white" />
            </label>
          </div>
        </div>

        <!-- 运动 -->
        <div class="glass rounded-card-xl p-4 section-bar" style="border-color: #c39b7e">
          <div class="flex items-center gap-2 mb-3 text-slate-700 font-medium text-sm">
            <Dumbbell class="text-orange-500" :size="16" /> 运动
          </div>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <label class="text-xs text-slate-500">运动类型
              <input v-model="makeupForm.exerciseType" type="text" placeholder="跑步 / 游泳 / 瑜伽" class="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 outline-none focus:border-brand bg-white" />
            </label>
            <label class="text-xs text-slate-500">时长 (分钟)
              <input v-model.number="makeupForm.exerciseDuration" type="number" min="0" class="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 outline-none focus:border-brand bg-white" />
            </label>
          </div>
        </div>

        <!-- 饮水 & 心情 -->
        <div class="glass rounded-card-xl p-4 section-bar" style="border-color: #6f97a0">
          <div class="flex items-center gap-2 mb-3 text-slate-700 font-medium text-sm">
            <Droplets class="text-cyan-500" :size="16" /> 饮水 & 心情
          </div>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <label class="text-xs text-slate-500">饮水量 (ml)
              <input v-model.number="makeupForm.waterIntake" type="number" min="0" class="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 outline-none focus:border-brand bg-white" />
            </label>
            <div class="text-xs text-slate-500">心情
              <div class="mt-2 flex gap-2">
                <button
                  v-for="m in [1,2,3,4,5]"
                  :key="m"
                  type="button"
                  class="mood-emoji text-2xl leading-none transition-transform duration-200 hover:scale-125 cursor-pointer"
                  :class="makeupForm.mood === m ? 'scale-125 drop-shadow-[0_0_8px_rgba(102,126,234,0.6)]' : 'opacity-60 hover:opacity-100'"
                  @click="makeupForm.mood = m"
                >{{ MOOD_EMOJIS[m] }}</button>
              </div>
              <div class="text-xs text-slate-400 mt-1">{{ makeupForm.mood ? MOOD_LABELS[makeupForm.mood] : '点击选择' }}</div>
            </div>
          </div>
          <label class="block text-xs text-slate-500 mt-3">备注
            <textarea v-model="makeupForm.remark" rows="2" placeholder="那天的小感想…" class="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 outline-none focus:border-brand resize-none bg-white"></textarea>
          </label>
        </div>

        <!-- 自定义目标 -->
        <div v-if="makeupCustomGoals.length" class="space-y-3">
          <div class="flex items-center gap-2 text-slate-700 font-medium text-sm px-1">
            <Target class="text-brand-soft" :size="16" /> 自定义目标
          </div>
          <div
            v-for="g in makeupCustomGoals"
            :key="g.id"
            class="glass rounded-card-xl p-4 section-bar"
            :style="{ borderColor: goalColor(g.goalType).from }"
          >
            <div class="flex items-center justify-between mb-2">
              <div class="flex items-center gap-2 text-slate-700 font-medium text-sm">
                <span class="h-2.5 w-2.5 rounded-full" :style="{ background: goalColor(g.goalType).from }"></span>
                {{ g.displayName }}
              </div>
              <span class="text-xs text-slate-400">目标 {{ g.targetValue }}{{ g.unit || '' }}</span>
            </div>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <label class="text-xs text-slate-500">数值{{ g.unit ? '（' + g.unit + '）' : '' }}
                <input
                  :value="makeupCustomInputs[g.id]?.value ?? null"
                  @input="setMakeupCustomInput(g.id, 'value', $event.target.value === '' ? null : Number($event.target.value))"
                  type="number"
                  :placeholder="'填写 ' + g.displayName"
                  class="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 outline-none focus:border-brand bg-white"
                />
              </label>
              <label class="text-xs text-slate-500">备注
                <input
                  :value="makeupCustomInputs[g.id]?.remark ?? ''"
                  @input="setMakeupCustomInput(g.id, 'remark', $event.target.value)"
                  type="text"
                  placeholder="可选"
                  class="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 outline-none focus:border-brand bg-white"
                />
              </label>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="flex gap-3">
          <button type="button" @click="makeupVisible = false"
            class="flex-1 py-2.5 rounded-xl text-sm font-medium border border-slate-200 text-slate-500 hover:bg-slate-50 transition-colors cursor-pointer">
            取消
          </button>
          <button type="button" :disabled="makeupSubmitting" @click="submitMakeup"
            class="flex-1 btn-grad py-2.5 rounded-xl font-medium flex items-center justify-center gap-2 text-sm transition-opacity disabled:opacity-60 cursor-pointer">
            <PenLine :size="16" /> {{ makeupSubmitting ? '保存中…' : '提交补签' }}
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
