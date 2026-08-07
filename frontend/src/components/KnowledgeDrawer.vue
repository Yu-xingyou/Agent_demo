<script setup>
import { ref, computed, watch } from 'vue'
import { BookOpen, Search, Trash2, RefreshCw, FileText, ChevronDown, Inbox } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import { importDocuments, searchKnowledge, listDocuments, deleteDocuments } from '@/api/rag'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

// 知识类型标签样式映射
const TYPE_META = {
  sleep: { label: '睡眠', cls: 'bg-indigo-50 text-indigo-600 border-indigo-100' },
  exercise: { label: '运动', cls: 'bg-emerald-50 text-emerald-600 border-emerald-100' },
  diet: { label: '饮食', cls: 'bg-amber-50 text-amber-600 border-amber-100' },
  custom: { label: '自定义', cls: 'bg-slate-100 text-slate-500 border-slate-200' },
}
const typeMeta = (t) => TYPE_META[t] || TYPE_META.custom

// ---- 导入预设 ----
const importing = ref(false)
async function handleImport() {
  // 预设知识（sql/knowledge/*.md）由后端初始化脚本 init-knowledge.mjs
  // 调用 /api/embedding 写入向量库，前端按钮不直接导入预设文档。
  ElMessage.info('预设知识需由后端初始化脚本导入；如需自定义知识，请调用导入接口并传入文档文本')
}

// ---- 检索测试 ----
const query = ref('')
const topK = ref(3)
const searching = ref(false)
const results = ref([])
const searched = ref(false)

async function handleSearch() {
  if (!query.value.trim()) {
    ElMessage.warning('请输入要检索的问题')
    return
  }
  searching.value = true
  try {
    const data = await searchKnowledge(query.value.trim(), topK.value)
    results.value = Array.isArray(data) ? data : []
    searched.value = true
  } catch (e) {
    console.error('知识库检索失败', e)
    results.value = []
  } finally {
    searching.value = false
  }
}

// ---- 文档列表 ----
const docs = ref([])
const docsLoading = ref(false)
const collapsed = ref({})

async function fetchDocuments() {
  docsLoading.value = true
  try {
    const data = await listDocuments()
    docs.value = Array.isArray(data) ? data : []
  } catch (e) {
    console.error('查询知识库文档失败', e)
    docs.value = []
  } finally {
    docsLoading.value = false
  }
}

// 按来源文件分组，组内按 chunkIndex 升序
const groups = computed(() => {
  const map = new Map()
  for (const d of docs.value) {
    const key = d.source || '未知来源'
    if (!map.has(key)) map.set(key, [])
    map.get(key).push(d)
  }
  return [...map.entries()].map(([source, items]) => ({
    source,
    docType: items[0]?.docType,
    items: items.slice().sort((a, b) => (a.chunkIndex ?? 0) - (b.chunkIndex ?? 0)),
  }))
})

function toggleGroup(source) {
  collapsed.value[source] = !collapsed.value[source]
}

async function handleDelete(doc) {
  try {
    await ElMessageBox.confirm('确定删除该知识片段？删除后 AI 将不再引用此内容。', '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await deleteDocuments([doc.id])
    docs.value = docs.value.filter((d) => d.id !== doc.id)
    results.value = results.value.filter((r) => r.id !== doc.id)
    ElMessage.success('片段已删除')
  } catch (e) {
    console.error('删除知识片段失败', e)
  }
}

// 抽屉打开时懒加载文档列表
watch(visible, (v) => {
  if (v && docs.value.length === 0) fetchDocuments()
})
</script>

<template>
  <el-drawer
    v-model="visible"
    direction="rtl"
    size="480px"
    :with-header="false"
    class="knowledge-drawer"
  >
    <div class="flex flex-col h-full bg-slate-50/60">
      <!-- 头部 -->
      <div class="bg-grad-primary text-white px-5 py-4 flex items-center gap-3 shrink-0">
        <div class="h-10 w-10 rounded-xl bg-white/20 flex items-center justify-center shrink-0">
          <BookOpen :size="20" />
        </div>
        <div class="flex-1 min-w-0">
          <h2 class="text-base font-semibold leading-tight">健康知识库</h2>
          <p class="text-xs text-white/80 mt-0.5">AI 回答的专业依据来源</p>
        </div>
        <button
          class="text-white/80 hover:text-white transition-colors text-xl leading-none cursor-pointer px-1"
          title="关闭"
          @click="visible = false"
        >×</button>
      </div>

      <div class="flex-1 overflow-y-auto p-4 space-y-4">
        <!-- 操作区 -->
        <section class="glass-strong rounded-card-xl p-4">
          <h3 class="text-sm font-semibold text-slate-700 mb-3 flex items-center gap-1.5">
            <RefreshCw :size="14" class="text-brand-soft" /> 知识入库
          </h3>
          <button
            class="btn-grad w-full rounded-xl py-2.5 text-sm text-white flex items-center justify-center gap-2 transition-opacity disabled:opacity-60 cursor-pointer disabled:cursor-not-allowed"
            :disabled="importing"
            @click="handleImport"
          >
            <RefreshCw :size="15" :class="importing ? 'animate-spin' : ''" />
            {{ importing ? '导入中…' : '导入知识库说明' }}
          </button>
        </section>

        <!-- 检索测试区 -->
        <section class="glass-strong rounded-card-xl p-4">
          <h3 class="text-sm font-semibold text-slate-700 mb-3 flex items-center gap-1.5">
            <Search :size="14" class="text-brand-soft" /> 检索测试
          </h3>
          <div class="flex items-center gap-2">
            <input
              v-model="query"
              type="text"
              placeholder="例如：睡眠不好怎么办"
              class="flex-1 min-w-0 rounded-full border border-slate-200 bg-white/70 px-3.5 py-2 text-sm outline-none focus:border-brand transition-colors"
              @keyup.enter="handleSearch"
            />
            <select
              v-model.number="topK"
              class="rounded-full border border-slate-200 bg-white/70 px-2 py-2 text-xs text-slate-600 outline-none focus:border-brand cursor-pointer"
              title="返回条数"
            >
              <option v-for="n in 10" :key="n" :value="n">{{ n }}</option>
            </select>
            <button
              class="btn-grad w-9 h-9 rounded-full flex items-center justify-center text-white shrink-0 transition-opacity disabled:opacity-60 cursor-pointer disabled:cursor-not-allowed"
              :disabled="searching"
              title="检索"
              @click="handleSearch"
            >
              <Search :size="15" :class="searching ? 'animate-pulse' : ''" />
            </button>
          </div>

          <div v-if="results.length" class="mt-3 space-y-2.5">
            <div
              v-for="(r, i) in results"
              :key="r.id"
              class="bg-white/80 border border-indigo-50 rounded-xl p-3 animate-rise"
              :style="{ animationDelay: i * 60 + 'ms' }"
            >
              <div class="flex items-center gap-2 mb-1.5">
                <span
                  class="text-[11px] px-2 py-0.5 rounded-full border"
                  :class="typeMeta(r.docType).cls"
                >{{ typeMeta(r.docType).label }}</span>
                <span class="text-[11px] text-slate-400 truncate flex-1">{{ r.source }}</span>
                <span class="text-[11px] font-medium text-brand-soft shrink-0">
                  {{ (r.score ?? 0).toFixed(3) }}
                </span>
              </div>
              <div class="h-1 rounded-full bg-slate-100 overflow-hidden mb-2">
                <div
                  class="h-full bg-grad-primary rounded-full transition-all duration-500"
                  :style="{ width: Math.min(100, (r.score ?? 0) * 100) + '%' }"
                />
              </div>
              <p class="text-xs text-slate-600 leading-relaxed line-clamp-3">{{ r.content }}</p>
            </div>
          </div>
          <p v-else-if="searched && !searching" class="mt-3 text-xs text-slate-400 text-center py-4">
            没有检索到相关片段，试试换个说法或先导入预设知识库。
          </p>
        </section>

        <!-- 文档列表区 -->
        <section class="glass-strong rounded-card-xl p-4">
          <div class="flex items-center justify-between mb-3">
            <h3 class="text-sm font-semibold text-slate-700 flex items-center gap-1.5">
              <FileText :size="14" class="text-brand-soft" /> 已入库片段
              <span v-if="docs.length" class="text-xs font-normal text-slate-400">（{{ docs.length }}）</span>
            </h3>
            <button
              class="text-slate-400 hover:text-brand-soft transition-colors cursor-pointer"
              title="刷新列表"
              @click="fetchDocuments"
            ><RefreshCw :size="14" :class="docsLoading ? 'animate-spin' : ''" /></button>
          </div>

          <!-- 加载骨架 -->
          <div v-if="docsLoading" class="space-y-2">
            <div v-for="n in 3" :key="n" class="h-9 rounded-lg bg-slate-100 animate-pulse" />
          </div>

          <!-- 空态 -->
          <div v-else-if="!docs.length" class="text-center py-6">
            <Inbox :size="28" class="mx-auto text-slate-300 mb-2" />
            <p class="text-xs text-slate-400">知识库还是空的，先导入预设文档试试</p>
          </div>

          <!-- 分组列表 -->
          <div v-else class="space-y-2">
            <div v-for="g in groups" :key="g.source" class="rounded-xl bg-white/60 border border-indigo-50 overflow-hidden">
              <button
                class="w-full flex items-center gap-2 px-3 py-2 text-left hover:bg-white/80 transition-colors cursor-pointer"
                @click="toggleGroup(g.source)"
              >
                <ChevronDown
                  :size="14"
                  class="text-slate-400 transition-transform shrink-0"
                  :class="collapsed[g.source] ? '-rotate-90' : ''"
                />
                <span
                  class="text-[11px] px-2 py-0.5 rounded-full border shrink-0"
                  :class="typeMeta(g.docType).cls"
                >{{ typeMeta(g.docType).label }}</span>
                <span class="text-xs text-slate-700 truncate flex-1">{{ g.source }}</span>
                <span class="text-[11px] text-slate-400 shrink-0">{{ g.items.length }} 片段</span>
              </button>

              <transition-group v-if="!collapsed[g.source]" name="fade-row" tag="div">
                <div
                  v-for="item in g.items"
                  :key="item.id"
                  class="group flex items-center gap-2 px-3 py-1.5 border-t border-slate-100/80 hover:bg-white/70 transition-colors"
                >
                  <span class="text-[11px] text-slate-400 shrink-0 w-7">#{{ item.chunkIndex ?? 0 }}</span>
                  <span class="text-xs text-slate-500 truncate flex-1">{{ item.content }}</span>
                  <button
                    class="text-slate-300 group-hover:text-red-500 transition-colors shrink-0 cursor-pointer"
                    title="删除该片段"
                    @click="handleDelete(item)"
                  ><Trash2 :size="13" /></button>
                </div>
              </transition-group>
            </div>
          </div>
        </section>
      </div>
    </div>
  </el-drawer>
</template>

<style scoped>
.line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.fade-row-leave-active {
  transition: all 0.25s ease;
}
.fade-row-leave-to {
  opacity: 0;
  transform: translateX(12px);
}
</style>

<style>
.knowledge-drawer .el-drawer__body {
  padding: 0;
  overflow: hidden;
}
@media (max-width: 640px) {
  .knowledge-drawer {
    width: 90vw !important;
  }
}
</style>
