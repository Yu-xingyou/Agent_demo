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
  <div class="max-w-3xl mx-auto px-5 py-6 flex flex-col h-[calc(100vh-4rem)]">
    <div class="flex items-center gap-2 mb-4">
      <Sparkles class="text-teal-700" />
      <h1 class="text-2xl font-semibold text-slate-800">AI 建议</h1>
    </div>

    <div class="panel-card flex-1 flex flex-col overflow-hidden">
      <div class="flex-1 overflow-y-auto p-4 space-y-4">
        <div
          v-for="(m, i) in messages"
          :key="i"
          class="flex gap-2"
          :class="m.role === 'user' ? 'flex-row-reverse' : ''"
        >
          <div
            class="w-8 h-8 rounded-full flex items-center justify-center shrink-0"
            :class="m.role === 'ai' ? 'bg-teal-700 text-white' : 'bg-sky-100 text-sky-600'"
          >
            <component :is="m.role === 'ai' ? Bot : User" :size="18" />
          </div>
          <div
            class="max-w-[75%] rounded-2xl px-4 py-2 text-sm leading-relaxed"
            :class="m.role === 'ai' ? 'bg-teal-50 text-slate-700' : 'bg-sky-100 text-slate-700'"
          >{{ m.text }}</div>
        </div>
      </div>

      <div class="border-t border-slate-100 p-3 flex items-center gap-2">
        <input
          v-model="input"
          type="text"
          placeholder="和习惯助手聊聊…"
          class="flex-1 rounded-full border border-slate-200 px-4 py-2 text-sm outline-none focus:border-teal-700"
          @keyup.enter="send"
        />
        <button
          class="w-10 h-10 rounded-full flex items-center justify-center text-white transition-opacity disabled:opacity-60"
          style="background: linear-gradient(135deg, #0f766e, #134e4a)"
          :disabled="sending"
          @click="send"
        ><Send :size="18" /></button>
        <button
          class="w-10 h-10 rounded-full flex items-center justify-center text-slate-500 border border-slate-200 hover:text-amber-500"
          @click="stop"
        ><Square :size="16" /></button>
      </div>
    </div>
  </div>
</template>
