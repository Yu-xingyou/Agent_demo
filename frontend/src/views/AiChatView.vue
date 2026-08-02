<script setup>
import { ref } from 'vue'
import { Sparkles, Send, Square, Bot, User } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'

const messages = ref([
  { role: 'ai', text: '你好，我是你的习惯助手 🌼 想聊聊今天的打卡，还是需要一些习惯养成建议？' },
])
const input = ref('')
const sending = ref(false)

function send() {
  const text = input.value.trim()
  if (!text || sending.value) return
  messages.value.push({ role: 'user', text })
  input.value = ''
  sending.value = true
  // 阶段五将替换为 SSE 流式调用 api/chat.js 的 streamMessage
  messages.value.push({ role: 'ai', text: '（AI 流式对话将在阶段五接入，当前为骨架演示）' })
  sending.value = false
}

function stop() {
  sending.value = false
  ElMessage.info('已停止（阶段五接入真实流式后生效）')
}
</script>

<template>
  <div class="max-w-3xl mx-auto px-5 py-6 flex flex-col h-[calc(100vh-4rem)] relative z-10">
    <div class="flex items-center gap-2 mb-4">
      <div class="h-9 w-9 rounded-xl bg-grad-primary flex items-center justify-center text-white shadow-glow">
        <Sparkles :size="18" />
      </div>
      <div>
        <h1 class="text-2xl font-semibold text-slate-800">AI 建议</h1>
        <p class="text-xs text-slate-500">基于你的习惯数据，给出专属养成建议</p>
      </div>
    </div>

    <div class="glass-strong rounded-card-xl flex-1 flex flex-col overflow-hidden">
      <div class="flex-1 overflow-y-auto p-4 space-y-4">
        <div
          v-for="(m, i) in messages"
          :key="i"
          class="flex gap-2 animate-rise"
          :class="m.role === 'user' ? 'flex-row-reverse' : ''"
        >
          <div
            class="w-9 h-9 rounded-full flex items-center justify-center shrink-0 shadow"
            :class="m.role === 'ai' ? 'bg-grad-primary text-white' : 'bg-white text-brand-indigo border border-indigo-100'"
          >
            <component :is="m.role === 'ai' ? Bot : User" :size="18" />
          </div>
          <div
            class="max-w-[75%] rounded-2xl px-4 py-2.5 text-sm leading-relaxed"
            :class="m.role === 'ai' ? 'bg-white/80 text-slate-700 border border-indigo-50' : 'bg-brand-indigo/10 text-slate-700'"
          >{{ m.text }}</div>
        </div>
      </div>

      <div class="border-t border-white/60 p-3 flex items-center gap-2">
        <input
          v-model="input"
          type="text"
          placeholder="和习惯助手聊聊…"
          class="flex-1 rounded-full border border-slate-200 bg-white/70 px-4 py-2 text-sm outline-none focus:border-brand-indigo transition-colors"
          @keyup.enter="send"
        />
        <button
          class="btn-grad w-10 h-10 rounded-full flex items-center justify-center text-white transition-opacity disabled:opacity-60"
          :disabled="sending"
          @click="send"
        ><Send :size="18" /></button>
        <button
          class="w-10 h-10 rounded-full flex items-center justify-center text-slate-500 border border-slate-200 hover:text-brand-purple"
          @click="stop"
        ><Square :size="16" /></button>
      </div>
    </div>
  </div>
</template>
