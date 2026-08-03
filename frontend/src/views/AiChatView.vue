<script setup>
import { ref, nextTick } from 'vue'
import { Sparkles, Send, Square, Bot, User, RotateCcw } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { streamMessage, stopChat } from '@/api/chat'

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
const conversationId = ref('')
const toolStatus = ref('')    // 工具调用过渡提示文本（空字符串表示无工具调用）
let abortController = null

const scrollBox = ref(null)
function scrollToBottom() {
  nextTick(() => {
    if (scrollBox.value) scrollBox.value.scrollTop = scrollBox.value.scrollHeight
  })
}

function send(text) {
  const content = (text ?? input.value).trim()
  if (!content || sending.value) return
  messages.value.push({ role: 'user', text: content })
  input.value = ''
  scrollToBottom()

  sending.value = true
  // 先放一条空的助手消息，流式逐 chunk 拼接
  const aiMsg = { role: 'ai', text: '' }
  messages.value.push(aiMsg)

  abortController = streamMessage({
    message: content,
    conversationId: conversationId.value || undefined,
    onMeta: (meta) => {
      if (meta && meta.conversationId) conversationId.value = meta.conversationId
    },
    onToolCall: (tc) => {
      // 工具调用降级信号：显示过渡提示
      if (tc && tc.message) toolStatus.value = tc.message
    },
    onChunk: (chunk) => {
      // 收到第一个文本 chunk 时清除工具调用状态
      if (toolStatus.value) toolStatus.value = ''
      aiMsg.text += chunk.content
      scrollToBottom()
    },
    onDone: () => {
      toolStatus.value = ''
      sending.value = false
      abortController = null
    },
    onError: (err) => {
      toolStatus.value = ''
      sending.value = false
      abortController = null
      aiMsg.text = (aiMsg.text || '') + '\n\n⚠️ ' + ((err && err.message) || '助手暂时无法回应，请稍后再试')
      scrollToBottom()
    },
  })
}

function stop() {
  if (!sending.value) return
  if (abortController) {
    abortController.abort()
    abortController = null
  }
  if (conversationId.value) stopChat(conversationId.value)
  sending.value = false
  ElMessage.info('已停止生成')
}

function resetChat() {
  conversationId.value = ''
  messages.value = [
    { role: 'ai', text: '对话已重置，有什么想聊的？' },
  ]
  ElMessage.success('已开启新对话')
}
</script>

<template>
  <div class="max-w-3xl mx-auto px-5 py-6 flex flex-col h-[calc(100vh-4rem)] relative z-10">
    <div class="flex items-center gap-2 mb-4">
      <div class="h-9 w-9 rounded-xl bg-grad-primary flex items-center justify-center text-white shadow-glow">
        <Sparkles :size="18" />
      </div>
      <div class="flex-1">
        <h1 class="text-2xl font-semibold text-slate-800">AI 建议</h1>
        <p class="text-xs text-slate-500">基于你的习惯数据，给出专属养成建议</p>
      </div>
      <button
        class="w-9 h-9 rounded-full flex items-center justify-center text-slate-500 border border-slate-200 hover:text-brand-soft transition-colors"
        title="开启新对话"
        @click="resetChat"
      ><RotateCcw :size="16" /></button>
    </div>

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
            <span v-if="m.text">{{ m.text }}</span>
            <span v-else-if="sending && m === messages[messages.length - 1]" class="inline-flex gap-1 items-center">
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
          </div>
        </div>
      </div>

      <div v-if="!sending" class="px-4 pt-2 flex flex-wrap gap-2">
        <button
          v-for="s in suggestions"
          :key="s"
          class="text-xs rounded-full px-3 py-1.5 bg-white/70 border border-indigo-100 text-slate-600 hover:text-brand-soft hover:border-brand/40 transition-colors"
          @click="send(s)"
        >{{ s }}</button>
      </div>

      <div class="border-t border-white/60 p-3 flex items-center gap-2">
        <input
          v-model="input"
          type="text"
          placeholder="和习惯助手聊聊…（可询问你的目标 / 打卡 / 趋势）"
          class="flex-1 rounded-full border border-slate-200 bg-white/70 px-4 py-2 text-sm outline-none focus:border-brand transition-colors"
          @keyup.enter="send"
        />
        <button
          class="btn-grad w-10 h-10 rounded-full flex items-center justify-center text-white transition-opacity disabled:opacity-60"
          :disabled="sending"
          @click="send"
        ><Send :size="18" /></button>
        <button
          class="w-10 h-10 rounded-full flex items-center justify-center text-slate-500 border border-slate-200 hover:text-brand-soft transition-colors"
          :disabled="!sending"
          @click="stop"
        ><Square :size="16" /></button>
      </div>
    </div>
  </div>
</template>
