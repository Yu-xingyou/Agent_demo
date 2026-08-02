# API 接口文档设计方案 — 生活习惯助手 Agent

## Summary

基于《agent_demo开发计划.md》和《详细模块开发流程.md》，设计覆盖全部 9 个功能模块、48 个接口端点的完整 REST API 接口文档。包含统一 Result 封装规范、SSE 多事件类型格式定义、完整错误码体系、VO 数据模型清单。

> 文档状态：已与当前代码实现同步。已实现接口（HabitController、GoalController 含自定义目标打卡记录）均已标注；超出原接口设计的「自定义目标打卡记录」5 个接口已整合进目标模块；尚未实现的 7 个 Controller 模块已标注「未实现，待模块化开发」。

## Current State Analysis

- 项目已有部分 Java 代码落地：已实现 `HabitController`（习惯记录）与 `GoalController`（习惯目标 + 自定义目标打卡记录）两大模块。
- 数据存储采用 MongoDB + MySQL 双库架构。
- 已实现 Controller（2 个，均含 Swagger/OpenAPI 文档注解与 Bean Validation 参数校验）：
  - `HabitController`（`/api/habits`，7 端点）
  - `GoalController`（`/api/goals` + `/api/goal-records`，14 端点，含超出原接口设计的「自定义目标打卡记录」5 个接口，见下文"目标模块扩展接口"）
- 尚未实现（待模块化开发）的 Controller（7 个）：ChatController / SessionController / AnalysisController / AiAnalysisController / RagController / PageController / ReminderController。

## 接口实现状态总览

> 下表在原计划 9 大模块基础上补充「实现状态」与「备注」，便于跟踪开发进度。

| 模块 | Controller | 端点数 | 实现状态 | 备注 |
|---|---|---|---|---|
| 习惯记录 CRUD | HabitController | 7 | ✅ 已实现 | 含 @Validated + @Tag + 参数校验 |
| 习惯目标管理 | GoalController | 9 | ✅ 已实现 | 原设计 6 端点，实际扩展至 9（含 active-with-custom 等） |
| 自定义目标打卡记录 | GoalController | 5 | ✅ 已实现（超纲） | 原接口文档未定义，见"目标模块扩展接口" |
| AI 对话 | ChatController | 3 | ❌ 未实现 | 待模块化开发（阶段五） |
| 会话管理 | SessionController | 7 | ❌ 未实现 | 待模块化开发（阶段五） |
| 趋势分析 | AnalysisController | 4 | ❌ 未实现 | 待模块化开发 |
| AI 分析结果 | AiAnalysisController | 6 | ❌ 未实现 | 待模块化开发 |
| RAG 知识库 | RagController | 5 | ❌ 未实现 | 待模块化开发 |
| 页面路由 | PageController | 5 | ❌ 未实现 | 待模块化开发（阶段三） |
| 打卡提醒 | ReminderController | 5 | ❌ 未实现 | 待模块化开发（对应 PRD 可选功能「打卡提醒」） |

## 目标模块扩展接口（自定义目标打卡记录）

> 以下 5 个接口为代码实现中新增、**超出原《API接口设计方案》接口定义**的部分，现整合进「目标模块」一并管理，统一前缀 `/api/goal-records`。

| 方法 | 路径 | 说明 | 参数 |
|---|---|---|---|
| POST | `/api/goal-records/records` | 录入/更新自定义目标打卡 | `@RequestBody HabitGoalRecord`（@Valid 校验：goalId、recordDate 必填） |
| GET | `/api/goal-records/records/today` | 查询今日所有自定义目标打卡 | 无 |
| GET | `/api/goal-records/records?startDate=&endDate=` | 按日期范围查询 | startDate、endDate（可选，ISO 日期） |
| GET | `/api/goal-records/records/recent/{days}` | 查询最近 N 天打卡 | days（路径参数，必填） |
| GET | `/api/goal-records/records/goal/{goalId}/recent/{days}` | 按目标查询最近 N 天打卡 | goalId（路径，必填）、days（路径，必填） |

**请求示例（录入打卡）**

```json
POST /api/goal-records/records
{
  "goalId": 1,
  "recordDate": "2026-08-02",
  "value": 10000,
  "remark": "完成"
}
```

**响应示例**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "goalId": 1,
    "recordDate": "2026-08-02",
    "value": 10000,
    "remark": "完成"
  }
}
```

> 上述接口均已接入全局异常处理：校验失败返回 `Result.error(40001, "参数校验失败: ...")`，与现有错误码体系一致。

## PRD 覆盖评审结果

对比 PRD 需求文档逐项检查，所有功能均已覆盖：

| PRD 功能项 | 覆盖状态 | 对应模块 |
|---|---|---|
| 每日录入作息/饮食/运动数据 | 已覆盖 | HabitController |
| 查看历史记录 | 已覆盖 | HabitController |
| AI 分析结果 | 已覆盖 | AiAnalysisController |
| 生活习惯改善建议 | 已覆盖 | AiAnalysisController suggestion 字段 |
| 每日评价（输出数据） | 已覆盖 | AiAnalysisController POST `/api/ai-analysis/daily` |
| 周期趋势总结（输出数据） | 已覆盖 | AiAnalysisController |
| 生活习惯风险提示（输出数据） | 已覆盖 | AiAnalysisController riskWarning 字段 |
| 周报/月报统计（可选） | 已覆盖 | AnalysisController + AiAnalysisController |
| 图表展示趋势（可选） | 已覆盖 | AnalysisController |
| 自定义习惯目标（可选） | 已覆盖 | GoalController |
| 达成率统计（可选） | 已覆盖 | AnalysisController |
| 打卡提醒（可选功能） | 已覆盖 | ReminderController |

## Proposed Changes

创建一份完整的 API 接口文档（MD 格式），包含以下内容：

### 1. 通用约定
- Base URL: `http://localhost:8080`
- 统一响应 `Result<T>`：`{code, message, data}`
- 错误码体系：200/400/404/409/429/503/500（共 19 个业务码）
- 全局异常处理映射

### 2. 九大模块接口（共 48 个端点）

| 模块 | Controller | 端点数 | 核心接口 |
|---|---|---|---|
| 习惯记录 CRUD | HabitController | 7 | POST/GET/DELETE `/api/habits` |
| 习惯目标管理 | GoalController | 6 | CRUD `/api/goals` |
| AI 对话 | ChatController | 3 | 非流式对话 + SSE 流式 + 停止生成 |
| 会话管理 | SessionController | 7 | 会话 CRUD + 消息历史 |
| 趋势分析 | AnalysisController | 4 | 趋势/达成率/概览/雷达图 |
| AI 分析结果 | AiAnalysisController | 6 | 列表/详情/触发/删除/每日评价 |
| RAG 知识库 | RagController | 5 | 导入/检索/列表/删除/上传 |
| 页面路由 | PageController | 5 | 首页/打卡/历史/趋势/AI建议 |
| 打卡提醒 | ReminderController | 5 | CRUD + 启用/禁用切换 |

### 3. SSE 特殊响应格式（仅 `GET /api/chat/stream` 流式端点使用）
- 非流式端点 `POST /api/chat` 返回标准 `Result<ChatResponseVO>` JSON 响应
- 5 种事件类型：`meta` / `chunk` / `tool_call` / `done` / `error`
- 前端 EventSource 对接规范
- 事件序列图

### 4. VO 数据模型汇总
- 22 个 VO 类定义

### 5. 接口总览表 + 实施依赖说明

## Assumptions & Decisions
- SSE 用 GET（EventSource 原生仅支持 GET）
- 采用非流式 + SSE 流式双模式对话（对标 OpenAI/Anthropic/DashScope 企业标准，stream 参数切换两种模式）
- 非流式 `POST /api/chat` 用于后端任务调用和测试调试，流式 `GET /api/chat/stream` 用于前端交互
- 默认 userId=1（单用户演示场景）
- 会话管理独立 Controller（关注点分离）
- 提醒模块独立 Controller（对应 PRD 可选功能「打卡提醒」）

## Verification Steps
1. 接口文档覆盖 PRD 所有必做功能（打卡/历史/AI分析/建议/每日评价）
2. 接口文档覆盖 PRD 所有可选功能（提醒/周月报/图表/目标/达成率）
3. 每个模块接口与开发计划阶段对应
4. SSE 事件格式定义完整可实施
5. VO 数据模型覆盖所有响应场景
6. 48 个端点均有完整 JSON 请求/响应示例
