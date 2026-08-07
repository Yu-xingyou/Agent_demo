// 路由智能体内部名称集合，与后端 AgentTypeEnum.agentName 字段值保持一致（大写）。
// 作为前端「展示侧」过滤路由智能体中间转发产物的单一数据源：
// 路由智能体在分发时可能仅输出内部分类名（如 SLEEP/DIET），这些属于内部实现细节，
// 不应展示给最终用户。若后端枚举新增/调整分类名，仅需在此处同步更新。
export const ROUTE_AGENT_NAMES = [
  'HEALTH',
  'ROUTE',
  'SLEEP',
  'DIET',
  'EXERCISE',
  'CHECKIN',
  'KNOWLEDGE',
]

/**
 * 判断文本是否为路由智能体的内部转发名称（与后端 AgentTypeEnum.agentNameOf 语义对齐）。
 * 用于实时流式与历史回显环节识别并隐藏内部名称，避免泄露实现细节。
 * @param {string} text 待判断文本
 * @returns {boolean} 整段文本等于某个路由智能体名称时为 true
 */
export function isRouteAgentName(text) {
  if (!text) return false
  return ROUTE_AGENT_NAMES.includes(text.trim().toUpperCase())
}
