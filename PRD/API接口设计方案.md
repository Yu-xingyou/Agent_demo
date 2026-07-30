# API 接口文档设计方案 — 生活习惯助手 Agent

## Summary

基于《agent_demo开发计划.md》和《详细模块开发流程.md》，设计覆盖全部 9 个功能模块、47 个接口端点的完整 REST API 接口文档。包含统一 Result 封装规范、SSE 多事件类型格式定义、完整错误码体系、VO 数据模型清单。

## Current State Analysis

- 项目当前仅有 PRD 文件和计划文档（开发计划 + 模块流程 + API 设计计划），尚无 Java 代码
- 数据存储采用 MongoDB + MySQL 双库架构
- 9 个 Controller 模块：HabitController / GoalController / ChatController / SessionController / AnalysisController / AiAnalysisController / RagController / PageController / ReminderController

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

### 2. 九大模块接口（共 47 个端点）

| 模块 | Controller | 端点数 | 核心接口 |
|---|---|---|---|
| 习惯记录 CRUD | HabitController | 7 | POST/GET/DELETE `/api/habits` |
| 习惯目标管理 | GoalController | 6 | CRUD `/api/goals` |
| AI 对话 | ChatController | 2 | SSE 流式 + 停止生成（仅流式，无普通对话） |
| 会话管理 | SessionController | 7 | 会话 CRUD + 消息历史 |
| 趋势分析 | AnalysisController | 4 | 趋势/达成率/概览/雷达图 |
| AI 分析结果 | AiAnalysisController | 6 | 列表/详情/触发/删除/每日评价 |
| RAG 知识库 | RagController | 5 | 导入/检索/列表/删除/上传 |
| 页面路由 | PageController | 5 | 首页/打卡/历史/趋势/AI建议 |
| 打卡提醒 | ReminderController | 5 | CRUD + 启用/禁用切换 |

### 3. SSE 特殊响应格式
- 5 种事件类型：`meta` / `chunk` / `tool_call` / `done` / `error`
- 前端 EventSource 对接规范
- 事件序列图

### 4. VO 数据模型汇总
- 22 个 VO 类定义

### 5. 接口总览表 + 实施依赖说明

## Assumptions & Decisions
- SSE 用 GET（EventSource 原生仅支持 GET）
- 仅采用 SSE 流式对话（用户要求，流式输出观感更好，移除普通对话端点）
- 默认 userId=1（单用户演示场景）
- 会话管理独立 Controller（关注点分离）
- 提醒模块独立 Controller（对应 PRD 可选功能「打卡提醒」）

## Verification Steps
1. 接口文档覆盖 PRD 所有必做功能（打卡/历史/AI分析/建议/每日评价）
2. 接口文档覆盖 PRD 所有可选功能（提醒/周月报/图表/目标/达成率）
3. 每个模块接口与开发计划阶段对应
4. SSE 事件格式定义完整可实施
5. VO 数据模型覆盖所有响应场景
6. 47 个端点均有完整 JSON 请求/响应示例
