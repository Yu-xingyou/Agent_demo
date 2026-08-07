// 路由智能体内部名称集合，与后端 AgentTypeEnum.agentNameOf 保持一致。
// 这些名称是内部实现细节（多智能体路由产物），不应展示给最终用户。
export const ROUTE_AGENT_NAMES = [
  'HEALTH',
  'ROUTE',
  'SLEEP',
  'DIET',
  'EXERCISE',
  'CHECKIN',
  'KNOWLEDGE'
]

// 判断一段文本是否仅为路由智能体内部名称（含被包裹在 ``` 代码块中的情况）
export function isRouteAgentName(text) {
  if (!text) return false
  const trimmed = String(text).trim()
  if (ROUTE_AGENT_NAMES.includes(trimmed)) return true
  // 处理 ```SLEEP``` 代码块形态
  const fenceMatch = trimmed.match(/^```([A-Z_]+)```$/)
  if (fenceMatch && ROUTE_AGENT_NAMES.includes(fenceMatch[1])) return true
  return false
}
