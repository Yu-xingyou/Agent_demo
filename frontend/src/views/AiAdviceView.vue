<template>
  <div class="ai-advice">
    <!-- 子 Tab 切换 -->
    <div class="flex items-center gap-1 px-6 pt-5">
      <button v-for="tab in tabs" :key="tab.key"
        class="px-5 py-2.5 rounded-t-xl text-sm font-medium transition"
        :class="activeTab === tab.key
          ? 'bg-white/70 text-blue-600 shadow-sm'
          : 'text-slate-500 hover:text-slate-700'"
        @click="activeTab = tab.key">
        <el-icon class="align-middle mr-1"><component :is="tab.icon" /></el-icon>
        {{ tab.label }}
      </button>
    </div>

    <div class="bg-gradient-to-b from-white/40 to-transparent min-h-[calc(100vh-180px)]">
      <AiChatView v-show="activeTab === 'chat'" />
      <AiAnalysisView v-if="activeTab === 'report'" tab-id="ai-analysis" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ChatDotRound, DataAnalysis } from '@element-plus/icons-vue'
import AiChatView from '@/views/AiChatView.vue'
import AiAnalysisView from '@/views/AiAnalysisView.vue'

const tabs = [
  { key: 'chat', label: '对话建议', icon: ChatDotRound },
  { key: 'report', label: '分析报告', icon: DataAnalysis }
]
const activeTab = ref('chat')
</script>

<style scoped>
.ai-advice {
  background: linear-gradient(135deg, #eff6ff 0%, #eef2ff 100%);
  min-height: 100%;
  border-radius: 0 0 24px 24px;
}
</style>
