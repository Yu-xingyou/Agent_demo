<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Target, Plus, Check, Flame } from 'lucide-vue-next'
import * as goalApi from '@/api/goal'
import { goalColor } from '@/constants/theme'
import { ElMessage } from 'element-plus'

const router = useRouter()
const goals = ref([])
const achievement = ref(null)
const loading = ref(false)

const goalCards = computed(() => goals.value.map((g) => {
  const c = goalColor(g.goalType)
  const target = Number(g.targetValue) || 0
  const current = Number(g.currentValue) || 0
  const progress = target > 0 ? Math.min(100, Math.round((current / target) * 100)) : 0
  return { ...g, color: c, progress, target, current }
}))

// 预留空间：新增目标入口的类型插槽
const presetTypes = [
  { type: 'SLEEP', label: '睡眠', unit: 'h' },
  { type: 'EXERCISE', label: '运动', unit: 'min' },
  { type: 'WATER', label: '饮水', unit: 'ml' },
  { type: 'DIET', label: '饮食', unit: '/5' },
  { type: 'CUSTOM', label: '自定义', unit: '' },
]

async function load() {
  loading.value = true
  try {
    const [g] = await Promise.all([
      goalApi.getActiveWithCustom().catch(() => []),
    ])
    goals.value = g || []
  } catch (e) {
    ElMessage.error('加载目标失败')
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>

<template>
  <div class="max-w-6xl mx-auto px-5 py-6 relative z-10">
    <!-- 头部 -->
    <div class="flex items-center justify-between mb-6">
      <div class="flex items-center gap-3">
        <div class="h-11 w-11 rounded-2xl bg-grad-primary flex items-center justify-center text-white shadow-glow">
          <Target :size="22" />
        </div>
        <div>
          <h1 class="text-2xl font-bold text-slate-800">任务目标</h1>
          <p class="text-sm text-slate-500">设定并追踪你的习惯养成计划</p>
        </div>
      </div>
      <button class="btn-grad px-4 py-2 rounded-xl text-sm font-medium flex items-center gap-1.5" @click="router.push('/checkin')">
        <Plus :size="16" /> 去打卡
      </button>
    </div>

    <!-- 目标卡片网格 -->
    <section class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
      <div
        v-for="(g, i) in goalCards"
        :key="g.id"
        class="goal-card rounded-card-xl p-5 animate-rise"
        :style="{ animationDelay: i * 70 + 'ms', '--gc': g.color.from, '--gc2': g.color.to }"
      >
        <div class="flex items-start justify-between">
          <div>
            <div class="flex items-center gap-2">
              <span class="h-2.5 w-2.5 rounded-full" :style="{ background: g.color.from }"></span>
              <span class="font-semibold text-slate-700">{{ g.displayName }}</span>
            </div>
            <div class="text-xs text-slate-400 mt-1">
              目标 {{ g.target }}{{ g.unit }} · 当前 {{ g.current || '—' }}
            </div>
          </div>
          <span v-if="g.isActive" class="text-xs px-2 py-0.5 rounded-full text-white" :style="{ background: g.color.from }">
            <Check :size="12" class="inline" /> 激活
          </span>
        </div>

        <!-- 进度环 -->
        <div class="flex items-center justify-center my-4">
          <div class="ring" :style="{ '--p': g.progress }">
            <div class="ring-inner">
              <span class="text-xl font-bold text-slate-800">{{ g.progress }}%</span>
            </div>
          </div>
        </div>

        <div class="h-2 rounded-full bg-slate-100 overflow-hidden">
          <div class="h-full rounded-full transition-all duration-700" :style="{ width: g.progress + '%', background: `linear-gradient(90deg, ${g.color.from}, ${g.color.to})` }"></div>
        </div>
      </div>

      <!-- 预留新增目标入口空间 -->
      <div
        class="goal-card rounded-card-xl p-5 border-2 border-dashed border-indigo-200 flex flex-col items-center justify-center text-indigo-400 cursor-pointer hover:border-indigo-400 hover:text-indigo-600 transition-colors"
        @click="router.push('/ai-chat')"
      >
        <div class="h-12 w-12 rounded-full bg-indigo-50 flex items-center justify-center"><Plus :size="24" /></div>
        <span class="text-sm mt-3 font-medium">新增任务目标</span>
        <span class="text-xs mt-1 text-center text-indigo-300">让 AI 帮你规划专属习惯</span>
      </div>
    </section>

    <!-- 预留类型色板说明（色彩映射展示） -->
    <section class="glass rounded-card-xl p-5 mt-6">
      <div class="flex items-center gap-2 mb-3 text-slate-700 font-medium"><Flame class="text-brand-purple" /> 目标色彩体系</div>
      <div class="flex flex-wrap gap-3">
        <div v-for="t in presetTypes" :key="t.type" class="flex items-center gap-2 px-3 py-2 rounded-lg" :style="{ background: goalColor(t.type).from + '22' }">
          <span class="h-3 w-3 rounded-full" :style="{ background: `linear-gradient(135deg, ${goalColor(t.type).from}, ${goalColor(t.type).to})` }"></span>
          <span class="text-sm text-slate-600">{{ goalColor(t.type).label }}</span>
        </div>
      </div>
    </section>

    <div v-if="!goals.length && !loading" class="text-center text-slate-400 py-10">
      暂无激活目标，点击右上方「去打卡」或「新增任务目标」开始规划
    </div>
  </div>
</template>

<style scoped>
.goal-card {
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(14px);
  box-shadow: 0 8px 30px rgba(15, 23, 42, 0.08);
}
.goal-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 16px 40px rgba(99, 102, 241, 0.2);
}
.ring {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background: conic-gradient(var(--gc) calc(var(--p) * 1%), rgba(148, 163, 184, 0.18) 0);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.7s ease;
}
.ring-inner {
  width: 76px;
  height: 76px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
