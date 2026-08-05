<script setup>
import { ref, onMounted, computed } from 'vue'
import { Bell, Plus, Trash2, Clock, Moon, Dumbbell, Droplets, Apple, Smile, Check, X } from 'lucide-vue-next'
import * as reminderApi from '@/api/reminder'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const loading = ref(false)
const showForm = ref(false)
const saving = ref(false)
const form = ref({ title: '', reminderTime: '08:00', reminderType: 'SLEEP', weekdays: '1,2,3,4,5,6,7', isActive: true })

const TYPE_MAP = {
  SLEEP: { label: '睡眠', icon: Moon, grad: 'bg-grad-sleep' },
  EXERCISE: { label: '运动', icon: Dumbbell, grad: 'bg-grad-exercise' },
  WATER: { label: '饮水', icon: Droplets, grad: 'bg-grad-water' },
  DIET: { label: '饮食', icon: Apple, grad: 'bg-grad-diet' },
  MOOD: { label: '心情', icon: Smile, grad: 'bg-grad-mood' },
}

const weekdaysText = computed(() => {
  const map = { 1: '一', 2: '二', 3: '三', 4: '四', 5: '五', 6: '六', 7: '日' }
  const ws = (form.value.weekdays || '').split(',').filter(Boolean)
  if (ws.length === 7) return '每天'
  return ws.length ? '周' + ws.map((w) => map[w]).join('、') : '未设置'
})

async function load() {
  loading.value = true
  try {
    const { data } = await reminderApi.listReminders()
    list.value = data || []
  } catch {
    ElMessage.error('加载提醒失败')
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!form.value.title.trim()) { ElMessage.warning('请填写提醒标题'); return }
  saving.value = true
  try {
    if (form.value.id) {
      await reminderApi.updateReminder(form.value.id, { ...form.value })
      ElMessage.success('已更新')
    } else {
      await reminderApi.createReminder({ ...form.value })
      ElMessage.success('已创建')
    }
    showForm.value = false
    form.value = { title: '', reminderTime: '08:00', reminderType: 'SLEEP', weekdays: '1,2,3,4,5,6,7', isActive: true }
    await load()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

function edit(r) {
  form.value = { ...r }
  showForm.value = true
}

async function remove(r) {
  try {
    await ElMessageBox.confirm(`确定删除提醒「${r.title}」吗？`, '删除提醒', { type: 'warning' })
  } catch { return }
  try {
    await reminderApi.deleteReminder(r.id)
    ElMessage.success('已删除')
    await load()
  } catch { ElMessage.error('删除失败') }
}

async function toggle(r) {
  try {
    await reminderApi.toggleReminder(r.id, !r.isActive)
    r.isActive = !r.isActive
  } catch { ElMessage.error('操作失败') }
}

onMounted(load)
</script>

<template>
  <div class="max-w-4xl mx-auto px-5 py-6 relative z-10">
    <div class="flex items-center justify-between mb-1">
      <div class="flex items-center gap-2">
        <Bell class="text-brand" />
        <h1 class="text-2xl font-semibold text-slate-800">打卡提醒</h1>
      </div>
      <button
        class="px-4 py-2 rounded-lg text-sm text-white bg-grad-primary flex items-center gap-1.5"
        @click="showForm = !showForm"
      >
        <Plus :size="15" /> 新建提醒
      </button>
    </div>
    <p class="text-sm text-slate-500 mb-5">按时提醒自己完成睡眠、运动、饮水等习惯打卡，养成稳定节奏。</p>

    <!-- 新建/编辑表单 -->
    <section v-if="showForm" class="glass rounded-card-xl p-5 mb-6 space-y-4">
      <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <label class="text-xs text-slate-500">提醒标题</label>
          <input v-model="form.title" class="mt-1 w-full px-3 py-2 rounded-lg border border-slate-200 text-sm outline-none focus:border-indigo-300" placeholder="如：睡前放松" />
        </div>
        <div>
          <label class="text-xs text-slate-500">提醒时间</label>
          <input v-model="form.reminderTime" type="time" class="mt-1 w-full px-3 py-2 rounded-lg border border-slate-200 text-sm outline-none focus:border-indigo-300" />
        </div>
        <div>
          <label class="text-xs text-slate-500">提醒类型</label>
          <select v-model="form.reminderType" class="mt-1 w-full px-3 py-2 rounded-lg border border-slate-200 text-sm outline-none focus:border-indigo-300">
            <option v-for="(v, k) in TYPE_MAP" :key="k" :value="k">{{ v.label }}</option>
          </select>
        </div>
        <div>
          <label class="text-xs text-slate-500">重复（{{ weekdaysText }}）</label>
          <input v-model="form.weekdays" class="mt-1 w-full px-3 py-2 rounded-lg border border-slate-200 text-sm outline-none focus:border-indigo-300" placeholder="1,2,3,4,5,6,7" />
        </div>
      </div>
      <div class="flex justify-end gap-2">
        <button class="px-4 py-1.5 rounded-lg text-sm text-slate-500 hover:bg-slate-100" @click="showForm = false">取消</button>
        <button class="px-4 py-1.5 rounded-lg text-sm text-white bg-grad-primary disabled:opacity-50" :disabled="saving" @click="save">
          {{ saving ? '保存中…' : '保存' }}
        </button>
      </div>
    </section>

    <!-- 提醒列表 -->
    <section class="space-y-3">
      <div v-if="!list.length && !loading" class="glass rounded-card-xl p-8 text-center text-slate-400 text-sm">
        还没有提醒，点击右上角「新建提醒」开始设定吧。
      </div>
      <div
        v-for="r in list"
        :key="r.id"
        class="glass rounded-card-xl p-4 flex items-center gap-4"
        :class="r.isActive ? '' : 'opacity-60'"
      >
        <div class="h-11 w-11 rounded-xl flex items-center justify-center text-white shadow" :class="(TYPE_MAP[r.reminderType] || TYPE_MAP.SLEEP).grad">
          <component :is="(TYPE_MAP[r.reminderType] || TYPE_MAP.SLEEP).icon" :size="20" />
        </div>
        <div class="flex-1">
          <div class="font-medium text-slate-700">{{ r.title }}</div>
          <div class="text-xs text-slate-400 flex items-center gap-1 mt-0.5">
            <Clock :size="12" /> {{ r.reminderTime }} · {{ weekdaysText }}
          </div>
        </div>
        <button
          class="h-8 w-8 rounded-lg flex items-center justify-center transition-colors"
          :class="r.isActive ? 'bg-emerald-100 text-emerald-600' : 'bg-slate-100 text-slate-400'"
          :title="r.isActive ? '点击停用' : '点击启用'"
          @click="toggle(r)"
        >
          <Check v-if="r.isActive" :size="16" />
          <X v-else :size="16" />
        </button>
        <button class="h-8 w-8 rounded-lg flex items-center justify-center text-slate-400 hover:text-indigo-500" @click="edit(r)">
          <component :is="Bell" :size="15" />
        </button>
        <button class="h-8 w-8 rounded-lg flex items-center justify-center text-slate-400 hover:text-rose-500" @click="remove(r)">
          <Trash2 :size="15" />
        </button>
      </div>
    </section>
  </div>
</template>
