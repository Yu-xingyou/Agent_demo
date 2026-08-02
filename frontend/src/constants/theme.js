// 全局视觉令牌与目标配色映射，集中管理避免散落
// 明亮柔和现代 SaaS 配色：靛蓝 → 紫 主渐变，维度用柔和蓝/青/橙/绿/粉/紫

export const GRADIENTS = {
  primary: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  primarySoft: 'linear-gradient(135deg, rgba(102,126,234,0.16), rgba(118,75,162,0.16))',
  sleep: 'linear-gradient(135deg, #60a5fa, #6366f1)',
  water: 'linear-gradient(135deg, #22d3ee, #38bdf8)',
  exercise: 'linear-gradient(135deg, #fb923c, #f59e0b)',
  diet: 'linear-gradient(135deg, #34d399, #10b981)',
  mood: 'linear-gradient(135deg, #f472b6, #ec4899)',
  custom: 'linear-gradient(135deg, #a78bfa, #8b5cf6)',
}

// 目标类型 → 配色（明亮柔和，用于任务目标板块预留色彩）
export const GOAL_COLORS = {
  SLEEP: { from: '#6366f1', to: '#60a5fa', label: '睡眠目标', unit: 'h' },
  EXERCISE: { from: '#f59e0b', to: '#fb923c', label: '运动目标', unit: 'min' },
  WATER: { from: '#38bdf8', to: '#22d3ee', label: '饮水目标', unit: 'ml' },
  DIET: { from: '#10b981', to: '#34d399', label: '饮食目标', unit: '/5' },
  CUSTOM: { from: '#8b5cf6', to: '#a78bfa', label: '自定义目标', unit: '' },
}

export function goalColor(type) {
  return GOAL_COLORS[type] || GOAL_COLORS.CUSTOM
}

// 心情配色（1-5），明亮柔和，用于图表/光环
export const MOOD_COLORS = {
  1: '#fb7185',
  2: '#fbbf24',
  3: '#38bdf8',
  4: '#34d399',
  5: '#a78bfa',
}
export const MOOD_LABELS = {
  1: '很差',
  2: '较差',
  3: '一般',
  4: '不错',
  5: '很棒',
}
// 心情 emoji 映射（1-5）
export const MOOD_EMOJIS = {
  1: '😫',
  2: '😟',
  3: '😐',
  4: '😊',
  5: '😄',
}

// ECharts 统一主题色板（明亮柔和）
export const CHART_PALETTE = [
  '#6366f1',
  '#22d3ee',
  '#f59e0b',
  '#10b981',
  '#ec4899',
  '#8b5cf6',
]
