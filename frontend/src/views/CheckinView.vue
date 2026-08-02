<script setup>
import { ref, onMounted, computed } from 'vue'
import { Moon, Apple, Dumbbell, Droplets, Smile, Save } from 'lucide-vue-next'
import * as habitApi from '@/api/habit'
import { MOOD_COLORS, MOOD_LABELS } from '@/constants/theme'
import { ElMessage } from 'element-plus'

const today = ref(null)
const submitting = ref(false)

// 本地时区的今日年月日，避免使用 toISOString() 导致的 UTC 偏移（UTC+8 下午会差一天）
function localDateStr(d = new Date()) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const form = ref({
  recordDate: localDateStr(),
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
})

const isChecked = computed(() => !!today.value)

async function load() {
  try {
    const t = await habitApi.getToday().catch(() => null)
    today.value = t
    if (t) {
      Object.assign(form.value, {
        recordDate: t.recordDate,
        sleepTime: t.sleepTime || '',
        wakeTime: t.wakeTime || '',
        sleepQuality: t.sleepQuality,
        dietDesc: t.dietDesc || '',
        dietScore: t.dietScore,
        exerciseType: t.exerciseType || '',
        exerciseDuration: t.exerciseDuration,
        waterIntake: t.waterIntake,
        mood: t.mood,
        remark: t.remark || '',
      })
    }
  } catch (e) {
    /* 忽略，视为未打卡 */
  }
}

async function submit() {
  submitting.value = true
  try {
    await habitApi.saveOrUpdate({ ...form.value })
    ElMessage.success(isChecked.value ? '记录已更新' : '打卡成功')
    await load()
  } catch (e) {
    /* 错误已由拦截器提示 */
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="max-w-3xl mx-auto px-5 py-6 relative z-10">
    <h1 class="text-2xl font-semibold text-slate-800 mb-1">每日打卡</h1>
    <p class="text-sm text-slate-500 mb-6">
      {{ isChecked ? '今天已打卡，可更新你的记录' : '记录今天的睡眠、饮食、运动与心情' }}
    </p>

    <div class="space-y-4">
      <!-- 睡眠 -->
      <div class="glass rounded-card-xl p-5 section-bar" style="border-color: #6366f1">
        <div class="flex items-center gap-2 mb-4 text-slate-700 font-medium">
          <Moon class="text-indigo-500" /> 睡眠
        </div>
        <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <label class="text-sm text-slate-500">入睡时间
            <input v-model="form.sleepTime" type="time" class="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 outline-none focus:border-brand-indigo" />
          </label>
          <label class="text-sm text-slate-500">起床时间
            <input v-model="form.wakeTime" type="time" class="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 outline-none focus:border-brand-indigo" />
          </label>
          <label class="text-sm text-slate-500">睡眠质量 (1-5)
            <input v-model.number="form.sleepQuality" type="number" min="1" max="5" class="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 outline-none focus:border-brand-indigo" />
          </label>
        </div>
      </div>

      <!-- 饮食 -->
      <div class="glass rounded-card-xl p-5 section-bar" style="border-color: #0d9488">
        <div class="flex items-center gap-2 mb-4 text-slate-700 font-medium">
          <Apple class="text-teal-600" /> 饮食
        </div>
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <label class="text-sm text-slate-500">饮食描述
            <input v-model="form.dietDesc" type="text" placeholder="今天吃了什么？" class="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 outline-none focus:border-brand-indigo" />
          </label>
          <label class="text-sm text-slate-500">饮食评分 (1-5)
            <input v-model.number="form.dietScore" type="number" min="1" max="5" class="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 outline-none focus:border-brand-indigo" />
          </label>
        </div>
      </div>

      <!-- 运动 -->
      <div class="glass rounded-card-xl p-5 section-bar" style="border-color: #f59e0b">
        <div class="flex items-center gap-2 mb-4 text-slate-700 font-medium">
          <Dumbbell class="text-orange-500" /> 运动
        </div>
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <label class="text-sm text-slate-500">运动类型
            <input v-model="form.exerciseType" type="text" placeholder="跑步 / 游泳 / 瑜伽" class="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 outline-none focus:border-brand-indigo" />
          </label>
          <label class="text-sm text-slate-500">时长 (分钟)
            <input v-model.number="form.exerciseDuration" type="number" min="0" class="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 outline-none focus:border-brand-indigo" />
          </label>
        </div>
      </div>

      <!-- 饮水 & 心情 -->
      <div class="glass rounded-card-xl p-5 section-bar" style="border-color: #06b6d4">
        <div class="flex items-center gap-2 mb-4 text-slate-700 font-medium">
          <Droplets class="text-cyan-500" /> 饮水 & 心情
        </div>
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <label class="text-sm text-slate-500">饮水量 (ml)
            <input v-model.number="form.waterIntake" type="number" min="0" class="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 outline-none focus:border-brand-indigo" />
          </label>
          <div class="text-sm text-slate-500">心情
            <div class="mt-2 flex gap-2">
              <button
                v-for="m in [1,2,3,4,5]"
                :key="m"
                class="mood-badge transition-transform"
                :class="form.mood === m ? 'ring-2 ring-offset-2 ring-slate-400 scale-110' : ''"
                :style="{ background: MOOD_COLORS[m] }"
                @click="form.mood = m"
              >{{ m }}</button>
            </div>
            <div class="text-xs text-slate-400 mt-1">{{ form.mood ? MOOD_LABELS[form.mood] : '点击选择' }}</div>
          </div>
        </div>
        <label class="block text-sm text-slate-500 mt-4">备注
          <textarea v-model="form.remark" rows="2" placeholder="今天的小感想…" class="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 outline-none focus:border-brand-indigo resize-none"></textarea>
        </label>
      </div>

      <button
        class="btn-grad w-full py-3 rounded-xl font-medium flex items-center justify-center gap-2 transition-opacity disabled:opacity-60"
        :disabled="submitting"
        @click="submit"
      >
        <Save :size="18" /> {{ isChecked ? '更新记录' : '提交打卡' }}
      </button>
    </div>
  </div>
</template>
