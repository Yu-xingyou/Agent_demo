// 全局视觉令牌与目标配色映射，集中管理避免散落
// 流动渐变主色：青绿 → 靛蓝 → 紫

export const GRADIENTS = {
  primary: 'linear-gradient(135deg, #0f766e 0%, #6366f1 50%, #a855f7 100%)',
  primarySoft: 'linear-gradient(135deg, rgba(15,118,110,0.18), rgba(168,85,247,0.18))',
  sleep: 'linear-gradient(135deg, #4f46e5, #6366f1)',
  water: 'linear-gradient(135deg, #0891b2, #06b6d4)',
  exercise: 'linear-gradient(135deg, #ea580c, #f59e0b)',
  diet: 'linear-gradient(135deg, #0d9488, #14b8a6)',
  mood: 'linear-gradient(135deg, #db2777, #ec4899)',
  custom: 'linear-gradient(135deg, #7c3aed, #a855f7)',
}

// 目标类型 → 配色（用于任务目标板块预留色彩）
export const GOAL_COLORS = {
  SLEEP: { from: '#4f46e5', to: '#6366f1', label: '睡眠目标', unit: 'h' },
  EXERCISE: { from: '#ea580c', to: '#f59e0b', label: '运动目标', unit: 'min' },
  WATER: { from: '#0891b2', to: '#06b6d4', label: '饮水目标', unit: 'ml' },
  DIET: { from: '#0d9488', to: '#14b8a6', label: '饮食目标', unit: '/5' },
  CUSTOM: { from: '#7c3aed', to: '#a855f7', label: '自定义目标', unit: '' },
}

export function goalColor(type) {
  return GOAL_COLORS[type] || GOAL_COLORS.CUSTOM
}

// 心情配色（1-5）
export const MOOD_COLORS = {
  1: '#ef4444',
  2: '#f97316',
  3: '#eab308',
  4: '#22c55e',
  5: '#0d9488',
}
export const MOOD_LABELS = {
  1: '很差',
  2: '较差',
  3: '一般',
  4: '不错',
  5: '很棒',
}

// ECharts 统一主题色板
export const CHART_PALETTE = [
  '#6366f1',
  '#0f766e',
  '#06b6d4',
  '#f59e0b',
  '#a855f7',
  '#ec4899',
]
