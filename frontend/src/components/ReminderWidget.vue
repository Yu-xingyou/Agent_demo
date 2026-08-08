<template>
  <div class="reminder-widget glass-card p-5">
    <div class="flex items-center justify-between mb-3">
      <div class="flex items-center gap-2 text-slate-700 font-medium">
        <el-icon class="text-blue-500"><Bell /></el-icon>
        <span>今日打卡提醒</span>
      </div>
    </div>

    <div v-if="loading" class="text-sm text-slate-400 py-4 text-center">
      <el-icon class="is-loading"><Loading /></el-icon> 加载中…
    </div>

    <ul v-else-if="todayReminders.length" class="space-y-2">
      <li v-for="r in todayReminders" :key="r.id"
        class="flex items-center gap-3 glass-card-soft rounded-xl px-3 py-2">
        <span class="w-2 h-2 rounded-full bg-emerald-400 flex-shrink-0"></span>
        <div class="flex-1 min-w-0">
          <p class="text-sm text-slate-700 truncate">{{ r.title }}</p>
          <p class="text-xs text-slate-400">{{ r.time || '全天' }} · {{ typeLabel(r.type) }}</p>
        </div>
        <el-icon class="text-slate-300"><ArrowRight /></el-icon>
      </li>
    </ul>

    <div v-else class="text-center py-4">
      <p class="text-sm text-slate-400">今天还没有待办提醒</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Bell, ArrowRight, Loading } from '@element-plus/icons-vue'
import { listReminders } from '@/api/reminder'

const loading = ref(false)
const reminders = ref([])

const TYPE_MAP = {
  daily: { label: '每日' },
  weekly: { label: '每周' },
  once: { label: '一次性' }
}
const WEEK = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

function typeLabel(t) {
  return TYPE_MAP[t]?.label || '提醒'
}

function isDueToday(r) {
  if (!r.isActive) return false
  if (r.type === 'daily') return true
  if (r.type === 'weekly') {
    const todayIdx = new Date().getDay()
    const days = Array.isArray(r.weekday) ? r.weekday : (r.weekday != null ? [r.weekday] : [])
    return days.map(Number).includes(todayIdx)
  }
  if (r.type === 'once') {
    if (!r.date) return true
    return r.date === new Date().toISOString().slice(0, 10)
  }
  return false
}

const todayReminders = computed(() => reminders.value.filter(isDueToday))

onMounted(async () => {
  loading.value = true
  try {
    const res = await listReminders()
    reminders.value = res?.data ?? res ?? []
  } finally {
    loading.value = false
  }
})
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
