// 全局视觉令牌与目标配色映射，集中管理避免散落
// 莫兰迪低饱和主色：灰青绿 → 灰靛蓝 → 灰紫

export const GRADIENTS = {
  primary: 'linear-gradient(135deg, #6f8a82 0%, #7e88a3 50%, #8f8196 100%)',
  primarySoft: 'linear-gradient(135deg, rgba(111,138,130,0.14), rgba(143,129,150,0.14))',
  sleep: 'linear-gradient(135deg, #6f7a99, #7e88a3)',
  water: 'linear-gradient(135deg, #5f8a92, #6f97a0)',
  exercise: 'linear-gradient(135deg, #b08a6f, #c39b7e)',
  diet: 'linear-gradient(135deg, #6f9a8a, #7faa9a)',
  mood: 'linear-gradient(135deg, #a87f8e, #b88f9e)',
  custom: 'linear-gradient(135deg, #8a7fa0, #9a8fb0)',
}

// 目标类型 → 配色（莫兰迪低饱和，用于任务目标板块预留色彩）
export const GOAL_COLORS = {
  SLEEP: { from: '#6f7a99', to: '#7e88a3', label: '睡眠目标', unit: 'h' },
  EXERCISE: { from: '#b08a6f', to: '#c39b7e', label: '运动目标', unit: 'min' },
  WATER: { from: '#5f8a92', to: '#6f97a0', label: '饮水目标', unit: 'ml' },
  DIET: { from: '#6f9a8a', to: '#7faa9a', label: '饮食目标', unit: '/5' },
  CUSTOM: { from: '#8a7fa0', to: '#9a8fb0', label: '自定义目标', unit: '' },
}

export function goalColor(type) {
  return GOAL_COLORS[type] || GOAL_COLORS.CUSTOM
}

// 心情配色（1-5），莫兰迪化
export const MOOD_COLORS = {
  1: '#b88a8a',
  2: '#c39b7e',
  3: '#b3a36f',
  4: '#7faa9a',
  5: '#6f9a8a',
}
export const MOOD_LABELS = {
  1: '很差',
  2: '较差',
  3: '一般',
  4: '不错',
  5: '很棒',
}

// ECharts 统一主题色板（莫兰迪低饱和）
export const CHART_PALETTE = [
  '#7e88a3',
  '#6f8a82',
  '#6f97a0',
  '#c39b7e',
  '#8f8196',
  '#b88f9e',
]
