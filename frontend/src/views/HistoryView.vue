<script setup>
import { ref, onMounted } from 'vue'
import { History } from 'lucide-vue-next'
import * as habitApi from '@/api/habit'
import { MOOD_COLORS, MOOD_LABELS } from '@/constants/theme'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const rows = ref([])

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
    rows.value = (await habitApi.listByRange(startDate, endDate)) || []
  } catch (e) {
    ElMessage.error('加载历史记录失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="max-w-5xl mx-auto px-5 py-6 relative z-10">
    <div class="flex items-center gap-2 mb-1">
      <History class="text-brand-indigo" />
      <h1 class="text-2xl font-semibold text-slate-800">历史记录</h1>
    </div>
    <p class="text-sm text-slate-500 mb-6">最近 30 天的生活习惯打卡明细</p>

    <div class="glass rounded-card-xl p-5">
      <el-table
        :data="rows"
        v-loading="loading"
        stripe
        style="width: 100%"
        :header-cell-style="{ color: '#64748b', fontWeight: 600 }"
      >
        <el-table-column prop="recordDate" label="日期" width="120" />
        <el-table-column label="睡眠" width="100">
          <template #default="{ row }">{{ row.sleepDuration ? row.sleepDuration + 'h' : '-' }}</template>
        </el-table-column>
        <el-table-column label="运动" width="100">
          <template #default="{ row }">{{ row.exerciseDuration ? row.exerciseDuration + 'min' : '-' }}</template>
        </el-table-column>
        <el-table-column label="饮水" width="100">
          <template #default="{ row }">{{ row.waterIntake ? row.waterIntake + 'ml' : '-' }}</template>
        </el-table-column>
        <el-table-column label="饮食评分" width="100">
          <template #default="{ row }">{{ row.dietScore ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="心情" width="90">
          <template #default="{ row }">
            <span v-if="row.mood" class="mood-badge" :style="{ background: MOOD_COLORS[row.mood] }">{{ row.mood }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="$router.push('/checkin')">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && !rows.length" class="text-center text-slate-400 py-10">暂无记录，去打卡页开始吧</div>
    </div>
  </div>
</template>
