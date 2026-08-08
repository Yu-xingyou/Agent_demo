<script setup>
import { ref, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { Sparkles, Send, Square, Bot, User, RotateCcw, Plus, Trash2, PanelLeftClose, PanelLeftOpen, MessageSquare, BookOpen } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import { streamMessage, stopChat } from '@/api/chat'
import { createSession, listSessions, getSession, deleteSession } from '@/api/session'
import { isRouteAgentName } from '@/constants/agent'
import KnowledgeDrawer from '@/components/KnowledgeDrawer.vue'

// 知识库管理抽屉（内嵌于本页，不新增路由）
const knowledgeOpen = ref(false)

// ---- 会话列表 ----
const sessions = ref([])
const sessionLoading = ref(false)
const sidebarOpen = ref(true)

async function fetchSessions() {
  sessionLoading.value = true
  try {
    const data = await listSessions()
    sessions.value = Array.isArray(data) ? data : []
  } catch {
    sessions.value = []
  } finally {
    sessionLoading.value = false
  }
}

// ---- 消息 ----
const messages = ref([
  { role: 'ai', text: '你好，我是你的习惯助手 🌼 想聊聊今天的打卡，还是需要一些习惯养成建议？\n\n试试问我：「我最近一周睡眠怎么样」「帮我看看本周运动达成率」或「帮我建个每日阅读目标」。' },
])

const suggestions = [
  '我最近一周睡眠怎么样',
  '帮我看看本周运动达成率',
  '我目前有哪些习惯目标',
  '帮我建个每日阅读 30 分钟的目标',
]

const input = ref('')
const sending = ref(false)
const sessionId = ref('')
const toolStatus = ref('')
let abortController = null

const scrollBox = ref(null)
let scrollRaf = 0
function scrollToBottom() {
  // 节流：合并同一帧内的多次滚动，避免高频触发导致主线程阻塞
  if (scrollRaf) return
  scrollRaf = requestAnimationFrame(() => {
    scrollRaf = 0
    if (scrollBox.value) scrollBox.value.scrollTop = scrollBox.value.scrollHeight
  })
}

// ---- 流式打字机节流渲染 ----
// 后端 SSE 是逐 token 流式，但 token 到达频率极高；直接把每个 token 写进响应式
// 属性会被 Vue 批量合并成"成段刷新"，视觉上不像流式。这里用一个缓冲区累积 token，
// 每 STREAM_FLUSH_MS 毫秒才刷一次到 aiMsg.text，还原"逐字流式"的打字机效果。
const STREAM_FLUSH_MS = 24          // 刷新间隔（毫秒），调大更"慢"、更明显逐字，调小更流畅
let streamBuffer = ''               // 待刷新的 token 累积区
let streamTimer = null              // 节流定时器句柄
let streamFlushPending = false      // 当前是否已有待执行的刷新

// 当前流式渲染目标消息在 messages 数组中的索引。
// 注意：必须通过 messages.value[index].text 写入（走 Vue 响应式 set），
// 不能持有原始对象引用直接改属性，否则不触发渲染更新。
let streamTargetIndex = -1

/** 接收一个 token：先入缓冲，再节流刷新 */
function streamAppend(token) {
  streamBuffer += token
  if (streamFlushPending) return
  streamFlushPending = true
  // 用定时器控制刷新节奏（打字机效果），每帧最多刷一次
  streamTimer = setTimeout(flushStreamBuffer, STREAM_FLUSH_MS)
}

/** 把缓冲区的 token 一次性写入 AI 消息的 text 并触发滚动 */
function flushStreamBuffer() {
  streamFlushPending = false
  if (streamTimer) {
    clearTimeout(streamTimer)
    streamTimer = null
  }
  if (!streamBuffer) return
  appendToTarget(streamBuffer)
  streamBuffer = ''
  scrollToBottom()
}

/** 通过响应式索引把文本追加到当前流式目标消息 */
function appendToTarget(text) {
  if (streamTargetIndex < 0 || !messages.value[streamTargetIndex]) return
  messages.value[streamTargetIndex].text += text
}

/** 流结束后强制刷掉剩余缓冲 */
function finalizeStreamBuffer() {
  if (streamTimer) {
    clearTimeout(streamTimer)
    streamTimer = null
  }
  streamFlushPending = false
  if (streamBuffer) {
    appendToTarget(streamBuffer)
    streamBuffer = ''
  }
  if (scrollRaf) cancelAnimationFrame(scrollRaf)
  scrollRaf = 0
  if (scrollBox.value) scrollBox.value.scrollTop = scrollBox.value.scrollHeight
}

onBeforeUnmount(() => {
  finalizeStreamBuffer()
  streamTargetIndex = -1
})

/** 确保当前会话存在：无 sessionId 时先向后端创建会话 */
async function ensureSession() {
  if (sessionId.value) return
  try {
    const data = await createSession(3)
    if (data && data.sessionId) {
      sessionId.value = data.sessionId
      await fetchSessions()
    }
  } catch {
    // 创建失败时允许继续发送（后端会再次处理）
  }
}

async function send(text) {
  const content = (typeof text === 'string' ? text : input.value).trim()
  if (!content || sending.value) return

  await ensureSession()

  messages.value.push({ role: 'user', text: content })
  input.value = ''
  scrollToBottom()

  sending.value = true
  const aiMsg = { role: 'ai', text: '', stats: null }
  const aiIndex = messages.value.push(aiMsg) - 1
  streamTargetIndex = aiIndex

  abortController = streamMessage({
    question: content,
    sessionId: sessionId.value,
    onData: (textChunk) => {
      toolStatus.value = ''
      const t = textChunk || ''
      // 过滤路由智能体内部转发名（防御，正常流式不包含）
      if (isRouteAgentName(t.trim())) return
      // 进入节流缓冲区，还原打字机流式效果
      streamAppend(t)
    },
    onStop: () => {
      finalizeStreamBuffer()
      streamTargetIndex = -1
      sending.value = false
      abortController = null
      fetchSessions()
    },
    onDone: () => {
      finalizeStreamBuffer()
      streamTargetIndex = -1
      sending.value = false
      abortController = null
    },
    onError: (err) => {
      finalizeStreamBuffer()
      streamTargetIndex = -1
      toolStatus.value = ''
      sending.value = false
      abortController = null
      messages.value[aiIndex].text = (messages.value[aiIndex].text || '') + '\n\n⚠️ ' + ((err && err.message) || '助手暂时无法回应，请稍后再试')
      scrollToBottom()
    },
  })
}

function stop() {
  if (!sending.value) return
  finalizeStreamBuffer()
  streamTargetIndex = -1
  if (abortController) {
    abortController.abort()
    abortController = null
  }
  if (sessionId.value) stopChat(sessionId.value)
  sending.value = false
  ElMessage.info('已停止生成')
}

async function resetChat() {
  if (sending.value) stop()
  sessionId.value = ''
  try {
    const data = await createSession(3)
    if (data && data.sessionId) {
      sessionId.value = data.sessionId
    }
  } catch {
    // 创建失败不阻断重置
  }
  messages.value = [
    { role: 'ai', text: '对话已重置，有什么想聊的？' },
  ]
  await fetchSessions()
  ElMessage.success('已开启新对话')
}

// ---- 会话切换 ----
async function loadSession(session) {
  if (sending.value) {
    stop()
  }
  sessionId.value = session.sessionId
  messages.value = []
  try {
    const history = await getSession(session.sessionId)
    if (Array.isArray(history) && history.length > 0) {
      messages.value = history.map(h => ({
        role: h.type === 'USER' ? 'user' : 'ai',
        text: h.content,
      }))
    } else {
      messages.value = [{ role: 'ai', text: '该会话暂无消息记录。' }]
    }
  } catch {
    messages.value = [{ role: 'ai', text: '加载历史消息失败，请重试。' }]
  }
  scrollToBottom()
}

async function removeSession(session, event) {
  event.stopPropagation()
  try {
    await ElMessageBox.confirm('确定要删除该会话吗？', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteSession(session.sessionId)
    if (sessionId.value === session.sessionId) {
      await resetChat()
    } else {
      await fetchSessions()
    }
    ElMessage.success('会话已删除')
  } catch {
    // 用户取消删除
  }
}

// ---- 时间格式化 ----
function formatTime(timeStr) {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
  return d.toLocaleDateString()
}

onMounted(() => {
  fetchSessions()
})
</script>

<template>
  <div class="flex h-[calc(100vh-4rem)] relative z-10">
    <!-- 会话侧边栏 -->
    <transition name="slide">
      <aside v-if="sidebarOpen" class="w-64 shrink-0 glass-strong border-r border-white/20 flex flex-col overflow-hidden">
        <!-- 侧边栏头部 -->
        <div class="p-4 border-b border-white/10 flex items-center justify-between">
          <span class="text-sm font-semibold text-slate-700">历史会话</span>
          <button
            class="w-8 h-8 rounded-lg flex items-center justify-center text-slate-500 hover:text-brand-soft hover:bg-white/50 transition-colors"
            title="新建对话"
            @click="resetChat"
          ><Plus :size="16" /></button>
        </div>
        <!-- 会话列表 -->
        <div class="flex-1 overflow-y-auto p-2 space-y-1">
          <div v-if="sessionLoading" class="text-center text-xs text-slate-400 py-8">加载中…</div>
          <div v-else-if="sessions.length === 0" class="text-center text-xs text-slate-400 py-8 px-4">
            <MessageSquare :size="24" class="mx-auto mb-2 opacity-40" />
            暂无历史会话<br/>点击上方 + 开始新对话
          </div>
          <button
            v-for="s in sessions"
            :key="s.sessionId"
            class="w-full text-left px-3 py-2.5 rounded-xl flex items-center gap-2 group transition-all"
            :class="sessionId === s.sessionId
              ? 'bg-brand/10 border-l-2 border-brand text-slate-800'
              : 'hover:bg-white/50 text-slate-600 border-l-2 border-transparent'"
            @click="loadSession(s)"
          >
            <div class="flex-1 min-w-0">
              <div class="text-xs font-medium truncate">{{ s.title || '新会话' }}</div>
              <div class="text-[10px] text-slate-400 mt-0.5">{{ formatTime(s.lastMessageTime) }}</div>
            </div>
            <Trash2
              :size="14"
              class="text-slate-300 hover:text-red-400 opacity-0 group-hover:opacity-100 transition-all shrink-0"
              @click="(e) => removeSession(s, e)"
            />
          </button>
        </div>
        <!-- 折叠按钮 -->
        <div class="border-t border-white/10 p-2 flex justify-end">
          <button
            class="w-7 h-7 rounded-lg flex items-center justify-center text-slate-400 hover:text-slate-600 hover:bg-white/50 transition-colors"
            title="收起侧边栏"
            @click="sidebarOpen = false"
          ><PanelLeftClose :size="14" /></button>
        </div>
      </aside>
    </transition>

    <!-- 右侧消息区 -->
    <div class="flex-1 flex flex-col min-w-0 px-5 py-5">
      <!-- 顶部标题区 -->
      <div class="flex items-center gap-2 mb-4 shrink-0">
        <button
          v-if="!sidebarOpen"
          class="w-8 h-8 rounded-lg flex items-center justify-center text-slate-500 hover:text-brand-soft hover:bg-white/30 transition-colors shrink-0"
          title="展开侧边栏"
          @click="sidebarOpen = true"
        ><PanelLeftOpen :size="16" /></button>
        <div class="h-9 w-9 rounded-xl bg-grad-primary flex items-center justify-center text-white shadow-glow">
          <Sparkles :size="18" />
        </div>
        <div class="flex-1">
          <h1 class="text-2xl font-semibold text-slate-800">AI 建议</h1>
          <p class="text-xs text-slate-500">基于你的习惯数据，给出专属养成建议</p>
        </div>
        <button
          class="w-9 h-9 rounded-full flex items-center justify-center text-slate-500 border border-slate-200 hover:text-brand-soft transition-colors"
          title="知识库"
          @click="knowledgeOpen = true"
        ><BookOpen :size="16" /></button>
        <button
          class="w-9 h-9 rounded-full flex items-center justify-center text-slate-500 border border-slate-200 hover:text-brand-soft transition-colors"
          title="开启新对话"
          @click="resetChat"
        ><RotateCcw :size="16" /></button>
      </div>

      <!-- 消息卡片区 -->
      <div class="glass-strong rounded-card-xl flex-1 flex flex-col overflow-hidden">
        <div ref="scrollBox" class="flex-1 overflow-y-auto p-4 space-y-4">
          <div
            v-for="(m, i) in messages"
            :key="i"
            class="flex gap-2 animate-rise"
            :class="m.role === 'user' ? 'flex-row-reverse' : ''"
          >
            <div
              class="w-9 h-9 rounded-full flex items-center justify-center shrink-0 shadow"
              :class="m.role === 'ai' ? 'bg-grad-primary text-white' : 'bg-white text-brand border border-indigo-100'"
            >
              <component :is="m.role === 'ai' ? Bot : User" :size="18" />
            </div>
            <div
              class="max-w-[75%] rounded-2xl px-4 py-2.5 text-sm leading-relaxed whitespace-pre-wrap"
              :class="m.role === 'ai'
                ? (sending && m === messages[messages.length - 1] && m.text === ''
                    ? 'bg-white/80 text-slate-400 border border-indigo-50'
                    : 'bg-white/80 text-slate-700 border border-indigo-50')
                : 'bg-brand/10 text-slate-700'"
            >
              <template v-if="m.role === 'ai' && m.text === '' && sending && m === messages[messages.length - 1]">
                <span class="inline-flex gap-1 items-center">
                  <template v-if="toolStatus">
                    <span class="text-xs text-brand-soft">{{ toolStatus }}</span>
                    <i class="w-1.5 h-1.5 rounded-full bg-brand/60 animate-pulse" />
                  </template>
                  <template v-else>
                    <i class="w-1.5 h-1.5 rounded-full bg-brand/60 animate-bounce" />
                    <i class="w-1.5 h-1.5 rounded-full bg-brand/60 animate-bounce" style="animation-delay:.15s" />
                    <i class="w-1.5 h-1.5 rounded-full bg-brand/60 animate-bounce" style="animation-delay:.3s" />
                  </template>
                </span>
              </template>
              <template v-else>
                <span>{{ m.text }}<span v-if="m.role === 'ai' && sending && m === messages[messages.length - 1]" class="caret">▋</span></span>
                <!-- AI 回复底部：Token 用量与耗时（新协议暂无真实用量，保留占位） -->
                <div v-if="m.role === 'ai' && m.stats" class="mt-1.5 text-[10px] text-slate-400 leading-none">
                  <span v-if="m.stats.totalTokens != null">Tokens: {{ m.stats.totalTokens }}</span>
                  <span v-if="m.stats.duration != null"> · {{ m.stats.duration }}ms</span>
                  <span v-if="m.stats.firstTokenLatency != null"> · 首字 {{ m.stats.firstTokenLatency }}ms</span>
                </div>
              </template>
            </div>
          </div>
        </div>

        <!-- 建议快捷词 -->
        <div v-if="!sending" class="px-4 pt-2 flex flex-wrap gap-2">
          <button
            v-for="s in suggestions"
            :key="s"
            class="text-xs rounded-full px-3 py-1.5 bg-white/70 border border-indigo-100 text-slate-600 hover:text-brand-soft hover:border-brand/40 transition-colors"
            @click="send(s)"
          >{{ s }}</button>
        </div>

        <!-- 输入区 -->
        <div class="border-t border-white/60 p-3 flex items-center gap-2">
          <input
            v-model="input"
            type="text"
            placeholder="和习惯助手聊聊…（可询问你的目标 / 打卡 / 趋势）"
            class="flex-1 rounded-full border border-slate-200 bg-white/70 px-4 py-2 text-sm outline-none focus:border-brand transition-colors"
            @keyup.enter="send(input)"
          />
          <button
            v-if="!sending"
            class="btn-grad w-10 h-10 rounded-full flex items-center justify-center text-white transition-opacity disabled:opacity-60"
            :disabled="!input.trim()"
            @click="send(input)"
          ><Send :size="18" /></button>
          <button
            v-else
            class="w-10 h-10 rounded-full flex items-center justify-center text-slate-500 border border-slate-200 hover:text-red-400 hover:border-red-200 transition-colors"
            @click="stop"
            title="停止生成"
          ><Square :size="16" /></button>
        </div>
      </div>
    </div>

    <!-- 知识库管理抽屉 -->
    <KnowledgeDrawer v-model="knowledgeOpen" />
  </div>
</template>

<style scoped>
.slide-enter-active,
.slide-leave-active {
  transition: width 0.3s ease, opacity 0.3s ease;
}
.slide-enter-from,
.slide-leave-to {
  width: 0;
  opacity: 0;
}

/* 流式生成时的打字机光标闪烁 */
.caret {
  display: inline-block;
  margin-left: 1px;
  color: #6366f1;
  animation: caret-blink 1s step-end infinite;
}
@keyframes caret-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
</style>
