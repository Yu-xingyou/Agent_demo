<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Flower2, Moon, Droplets, Dumbbell, Smile, Sparkles, ArrowRight } from 'lucide-vue-next'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent } from 'echarts/components'
import * as habitApi from '@/api/habit'
import * as goalApi from '@/api/goal'
import { ElMessage } from 'element-plus'

use([CanvasRenderer, PieChart, TooltipComponent, LegendComponent])

const router = useRouter()
const today = ref(null)
const recent = ref([])
const goals = ref([])
const loading = ref(false)

const moodColors = { 1: '#ef4444', 2: '#f97316', 3: '#eab308', 4: '#22c55e', 5: '#0d9488' }
const moodLabels = { 1: '很差', 2: '较差', 3: '一般', 4: '不错', 5: '很棒' }

const cards = computed(() => {
  const t = today.value || {}
  return [
    { key: 'sleep', title: '睡眠', icon: Moon, cls: 'stat-sleep', value: t.sleepDuration ? `${t.sleepDuration}h` : '未打卡', sub: t.sleepQuality ? `质量 ${t.sleepQuality}/5` : '记录你的睡眠' },
    { key: 'water', title: '饮水', icon: Droplets, cls: 'stat-water', value: t.waterIntake ? `${t.waterIntake}ml` : '未打卡', sub: '今日饮水目标 2000ml' },
    { key: 'exercise', title: '运动', icon: Dumbbell, cls: 'stat-exercise', value: t.exerciseDuration ? `${t.exerciseDuration}min` : '未打卡', sub: t.exerciseType || '动一动更精神' },
    { key: 'mood', title: '心情', icon: Smile, cls: 'stat-mood', value: t.mood ? moodLabels[t.mood] : '未打卡', sub: t.mood ? `评分 ${t.mood}/5` : '记录此刻心情' },
  ]
})

const moodOption = computed(() => {
  const counts = {}
  recent.value.forEach((r) => {
    if (r.mood) counts[r.mood] = (counts[r.mood] || 0) + 1
  })
  const data = Object.keys(moodColors).map((k) => ({
    name: moodLabels[k],
    value: counts[k] || 0,
    itemStyle: { color: moodColors[k] },
  }))
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['45%', '70%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        data,
      },
    ],
  }
})

const heroSlides = [
  { title: '今日打卡', desc: '坚持记录，遇见更好的自己', icon: Flower2 },
  { title: '每日一语', desc: '种一棵树最好的时间是十年前，其次是现在', icon: Sparkles },
  { title: '本周概况', desc: '看看你的习惯养成进度', icon: TrendingUp },
]
const heroIndex = ref(0)
const heroIcon = computed(() => heroSlides[heroIndex.value].icon)
setInterval(() => { heroIndex.value = (heroIndex.value + 1) % heroSlides.length }, 4000)

async function load() {
  loading.value = true
  try {
    const [t, r, g] = await Promise.all([
      habitApi.getToday().catch(() => null),
      habitApi.getRecent(7),
      goalApi.getActiveWithCustom().catch(() => []),
    ])
    today.value = t
    recent.value = r || []
    goals.value = g || []
  } catch (e) {
    ElMessage.error('加载首页数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="max-w-6xl mx-auto px-5 py-6">
    <!-- Hero 轮播 -->
    <section
      class="rounded-card shadow-card p-6 mb-6 text-white flex items-center gap-4 cursor-pointer transition-all"
      style="background: linear-gradient(135deg, #0f766e 0%, #134e4a 100%)"
      @click="router.push('/checkin')"
    >
      <component :is="heroIcon" :size="40" class="text-amber-500" />
      <div class="flex-1">
        <h2 class="text-xl font-semibold">{{ heroSlides[heroIndex].title }}</h2>
        <p class="opacity-90 text-sm mt-1">{{ heroSlides[heroIndex].desc }}</p>
      </div>
      <ArrowRight class="text-amber-500" />
    </section>

    <!-- 4 张渐变统计卡 -->
    <section class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
      <div
        v-for="c in cards"
        :key="c.key"
        class="stat-card flex flex-col justify-between cursor-pointer"
        :class="c.cls"
        @click="router.push('/checkin')"
      >
        <div class="flex items-center justify-between">
          <span class="text-sm opacity-90">{{ c.title }}</span>
          <component :is="c.icon" :size="20" />
        </div>
        <div>
          <div class="text-2xl font-semibold">{{ c.value }}</div>
          <div class="text-xs opacity-80 mt-1">{{ c.sub }}</div>
        </div>
      </div>
    </section>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- 本周心情分布 -->
      <div class="panel-card p-5 lg:col-span-1">
        <h3 class="font-semibold mb-3 text-slate-700">本周心情分布</h3>
        <VChart :option="moodOption" autoresize style="height: 240px" />
      </div>

      <!-- 最近 7 天记录 -->
      <div class="panel-card p-5 lg:col-span-2">
        <div class="flex items-center justify-between mb-3">
          <h3 class="font-semibold text-slate-700">最近 7 天记录</h3>
          <button class="text-teal-700 text-sm hover:text-amber-500" @click="router.push('/history')">查看全部 →</button>
        </div>
        <table class="w-full text-sm">
          <thead>
            <tr class="text-slate-500 text-left">
              <th class="py-2">日期</th><th>睡眠</th><th>运动</th><th>饮水</th><th>心情</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in recent" :key="r.id" class="border-t border-slate-100">
              <td class="py-2">{{ r.recordDate }}</td>
              <td>{{ r.sleepDuration ? r.sleepDuration + 'h' : '-' }}</td>
              <td>{{ r.exerciseDuration ? r.exerciseDuration + 'min' : '-' }}</td>
              <td>{{ r.waterIntake ? r.waterIntake + 'ml' : '-' }}</td>
              <td>
                <span v-if="r.mood" class="mood-badge" :style="{ background: moodColors[r.mood] }">{{ r.mood }}</span>
                <span v-else>-</span>
              </td>
            </tr>
            <tr v-if="!recent.length"><td colspan="5" class="py-4 text-center text-slate-400">暂无记录</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 我的目标 -->
    <section class="mt-6">
      <div class="flex items-center justify-between mb-3">
        <h3 class="font-semibold text-slate-700">我的目标</h3>
        <button class="text-teal-700 text-sm hover:text-amber-500" @click="router.push('/ai-chat')">让 AI 帮你规划 →</button>
      </div>
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <div
          v-for="g in goals"
          :key="g.id"
          class="goal-card p-4"
          :style="{ '--goal-color': g.color || '#0f766e' }"
        >
          <div class="font-semibold text-slate-700">{{ g.title }}</div>
          <div class="text-xs text-slate-500 mt-1">{{ g.description || '坚持养成好习惯' }}</div>
          <div class="mt-3 h-1.5 rounded-full bg-slate-100 overflow-hidden">
            <div class="h-full rounded-full" :style="{ width: (g.progress || 0) + '%', background: g.color || '#0f766e' }"></div>
          </div>
        </div>
        <div v-if="!goals.length" class="text-slate-400 text-sm col-span-full">暂无目标，去 AI 建议页聊聊吧</div>
      </div>
    </section>
  </div>
</template>
