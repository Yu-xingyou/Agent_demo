# API 接口文档设计方案 — 生活习惯助手 Agent

## Summary

基于《agent_demo开发计划.md》和《详细模块开发流程.md》，设计完整 REST API 接口文档。真实代码已落地 5 个后端功能模块、35 个 `/api` 端点（Habit 7 + Goal 14 + Chat 3 + Session 7 + Analysis 4）；另有 3 个模块（16 个端点）规划中尚未实现。文档包含统一 Result 封装规范、CORS 与跨域策略、鉴权约定、API 版本化、SSE 多事件类型格式定义、完整错误码体系、VO 数据模型清单。前端为单仓库内 Vue 3 + Vite 工程（`agent_demo/frontend/`），通过 HTTPS/JSON 调用本 API。

> 文档状态：已与当前代码实现同步。已实现接口（HabitController、GoalController 含自定义目标打卡记录、AnalysisController）均已标注；「自定义目标打卡记录」6 个接口已整合进目标模块并提升为正式功能；尚未实现的 5 个 Controller 模块已标注「未实现，待模块化开发」。

## Current State Analysis

- 项目已有部分 Java 代码落地：已实现 `HabitController`（习惯记录）、`GoalController`（习惯目标 + 自定义目标打卡记录）、`AnalysisController`（趋势分析）三大模块。
- 数据存储采用 MongoDB + MySQL 双库架构。
- 已实现 Controller（3 个，均含 Swagger/OpenAPI 文档注解与 Bean Validation 参数校验）：
  - `HabitController`（`/api/habits`，7 端点）
  - `GoalController`（`/api/goals` + `/api/goal-records`，14 端点：目标 CRUD 8 + 自定义目标打卡记录 6，见下文"目标模块扩展接口"）
  - `AnalysisController`（`/api/analysis`，4 端点：趋势/达成率/概览/雷达）
- 尚未实现（待模块化开发）的 Controller（3 个）：AiAnalysisController / RagController / ReminderController。

> **架构说明**：后端仅提供 REST API，**不含任何页面路由**（原 PageController 的 Thymeleaf 页面已移除）。前端页面由单仓库 Vue 工程 `agent_demo/frontend/` 实现，通过 `/api` 调用本接口。

## 接口实现状态总览

> 下表在原计划 9 大模块基础上补充「实现状态」与「备注」，便于跟踪开发进度。

| 模块 | Controller | 端点数 | 实现状态 | 备注 |
|---|---|---|---|---|
| 习惯记录 CRUD | HabitController | 7 | ✅ 已实现 | 含 @Validated + @Tag + 参数校验 |
| 习惯目标管理 | GoalController | 8 | ✅ 已实现 | `/api/goals` 目标 CRUD，含 CUSTOM 类型 |
| 自定义目标打卡记录 | GoalController | 6 | ✅ 已实现（正式功能） | 见"目标模块扩展接口"，原为超纲现提升为正式 |
| 趋势分析 | AnalysisController | 4 | ✅ 已实现 | `/api/analysis` 趋势/达成率/概览/雷达 |
| AI 对话 | ChatController | 3 | ✅ 已实现 | 非流式 + SSE 流式 + 停止 |
| 会话管理 | SessionController | 7 | ✅ 已实现 | 会话元数据 CRUD + 清理过期 |
| AI 分析结果 | AiAnalysisController | 6 | ❌ 未实现 | 待模块化开发 |
| RAG 知识库 | RagController | 5 | ❌ 未实现 | 待模块化开发 |
| 打卡提醒 | ReminderController | 5 | ❌ 未实现 | 待模块化开发（对应 PRD 可选功能「打卡提醒」） |

## 目标模块扩展接口（自定义目标打卡记录，正式功能）

> 以下 6 个接口为自定义目标打卡记录管理，统一前缀 `/api/goal-records`。原属超纲扩展接口，现提升为正式功能（对应 agent 模型「自定义习惯目标（可选）」），与目标 CRUD（8 端点）共同构成 GoalController 的 14 端点。

| 方法 | 路径 | 说明 | 参数 |
|---|---|---|---|
| POST | `/api/goal-records/records` | 录入/更新自定义目标打卡 | `@RequestBody HabitGoalRecord`（@Valid 校验：goalId、recordDate 必填） |
| GET | `/api/goal-records/records/today` | 查询今日所有自定义目标打卡 | 无 |
| GET | `/api/goal-records/records?startDate=&endDate=` | 按日期范围查询 | startDate、endDate（可选，ISO 日期） |
| GET | `/api/goal-records/records/recent/{days}` | 查询最近 N 天打卡 | days（路径参数，必填） |
| GET | `/api/goal-records/records/goal/{goalId}/recent/{days}` | 按目标查询最近 N 天打卡 | goalId（路径，必填）、days（路径，必填） |
| GET | `/api/goal-records/records/goal/{goalId}/achievement?period=WEEKLY` | 按目标/周期查询达成率 | goalId（路径，必填）、period（查询参数，WEEKLY/DAILY/MONTHLY，默认 WEEKLY，驱动 HabitGoalVO.weeklyAchievement） |

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

#### 1.1 基础信息
- **Base URL（开发）**：`http://localhost:8080`
- **Base URL（生产）**：前端独立部署域名经反向代理或 CORS 指向后端
- **API 前缀**：所有接口统一以 `/api` 开头（后端不提供任何页面路由）
- **API 版本化**：建议路径版本前缀 `/api/v1`，本期可直接使用 `/api`；新增不兼容变更时升级 `v2`，旧版本保留过渡期
- **数据格式**：请求/响应均为 `application/json`（SSE 端点除外）；时间统一 ISO-8601（`yyyy-MM-dd'T'HH:mm:ss`）；日期 `yyyy-MM-dd`
- **字符编码**：UTF-8

#### 1.2 统一响应体 `Result<T>`
```json
{
  "code": 0,
  "message": "success",
  "data": { }
}
```
- `code`：业务状态码，`0` 表示成功，非 `0` 表示失败（错误码见 1.5）
- `message`：提示信息，成功为 `success`，失败为具体错误描述
- `data`：业务数据载体，失败或无数据时可为 `null`

> 注意：HTTP 状态码与业务 `code` 分离。`Result.code` 为业务语义码；HTTP 状态统一 `200`（校验/业务异常也由全局异常处理返回 `200` + 非零 `code`），或按规范对 4xx/5xx 直接返回对应 HTTP 状态。本项目采用"HTTP 200 + 业务 code"模式，前端 Axios 拦截器统一按 `code` 判定成功与否。

#### 1.3 CORS 跨域策略
- 后端通过 `CorsConfiguration` / `@CrossOrigin` 或 Spring Security 配置允许前端域名：
  - 开发：`http://localhost:5173`（Vite 默认端口）
  - 生产：前端独立部署域名（如 `https://habit-agent.example.com`）
- 允许方法：`GET/POST/PUT/DELETE/OPTIONS`
- 允许头：`Content-Type`、`Authorization`
- 允许携带凭证：`AllowCredentials=true`（若使用 Cookie/Session）
- 预检缓存：`maxAge=3600`
- 开发联调也可由前端 Vite `server.proxy['/api']` 代理到 `http://localhost:8080` 规避跨域

#### 1.4 鉴权约定
- 本期为单用户演示（默认 `userId=1`），暂不强制 Token；接口通过请求头 `X-User-Id` 或统一登录上下文传入用户标识
- 后续生产化建议：
  - 增加 `Authorization: Bearer <JWT>` 请求头
  - 网关/拦截器校验 JWT，解析 `userId` 注入 `SecurityContext`
  - 所有数据查询强制带 `userId` 条件（防越权，见《agent_demo开发计划.md》6.3）

#### 1.5 错误码体系
- HTTP 层：`200/400/401/403/404/409/429/500/503`
- 业务 `code` 体系（非 0 为失败，共 19 个业务码）：

| code | 含义 |
|---|---|
| 0 | 成功 |
| 40001 | 参数校验失败 |
| 40002 | 请求格式错误 |
| 40101 | 未认证/Token 缺失 |
| 40102 | Token 过期 |
| 40301 | 无权限（越权访问） |
| 40401 | 资源不存在 |
| 40901 | 数据冲突（如重复打卡） |
| 42901 | 请求过于频繁（限流） |
| 50001 | 服务内部错误 |
| 50002 | AI 服务调用失败 |
| 50003 | 向量检索失败 |
| 50301 | 服务暂不可用（依赖宕机） |
| … | 其余业务码见完整文档 |

#### 1.6 全局异常处理映射
- `@RestControllerAdvice` 统一拦截：
  - `MethodArgumentNotValidException` → `Result.error(40001, "参数校验失败: ...")`
  - 业务自定义异常 → 对应业务码
  - 未知异常 → `Result.error(50001, "系统异常")`
- 前端 `utils/request.js` 拦截器：非 `code===0` 时 `ElMessage.error(message)` 并 reject

### 2. 后端模块接口统计（最终实现全量 51 端点）

> 原"页面路由 PageController（5 端点）"已移除：后端不再渲染页面，前端由单仓库 Vue 工程 `agent_demo/frontend/` 实现。以下按真实代码口径分「已落地 / 待开发」两表，替代原「8 模块 46 端点」规划值。

**已落地（5 模块，35 端点）**：

| 模块 | Controller | 端点数 | 核心接口 |
|---|---|---|---|
| 习惯记录 CRUD | HabitController | 7 | POST/GET/DELETE `/api/habits` |
| 习惯目标管理 | GoalController | 8 | CRUD `/api/goals`（含 CUSTOM 自定义目标） |
| 自定义目标打卡记录 | GoalController | 6 | `/api/goal-records/records`（含达成率接口） |
| AI 对话 | ChatController | 3 | 非流式对话 + SSE 流式 + 停止生成 |
| 会话管理 | SessionController | 7 | 会话 CRUD + 清理过期 |
| 趋势分析 | AnalysisController | 4 | 趋势/达成率/概览/雷达图 `/api/analysis` |
| **小计** | | **35** | |

**待开发（3 模块，16 端点）**：

| 模块 | Controller | 端点数 | 核心接口 |
|---|---|---|---|
| AI 分析结果 | AiAnalysisController | 6 | 列表/详情/触发/删除/每日评价 |
| RAG 知识库 | RagController | 5 | 导入/检索/列表/删除/上传 |
| 打卡提醒 | ReminderController | 5 | CRUD + 启用/禁用切换 |
| **小计** | | **16** | |

> 合计（最终实现全量）：35 + 16 = 51 个后端 REST 端点。

### 3. SSE 特殊响应格式（仅 `GET /api/chat/stream` 流式端点使用）
- 非流式端点 `POST /api/chat` 返回标准 `Result<ChatResponseVO>` JSON 响应
- 5 种事件类型：`meta` / `chunk` / `tool_call` / `done` / `error`
- 前端 EventSource 对接规范
- 事件序列图

### 4. VO 数据模型汇总
- 22 个 VO 类定义

### 5. 接口总览表 + 实施依赖说明

## Assumptions & Decisions
- **前后端分离**：后端仅产出 JSON API，不渲染页面；前端为单仓库 Vue 工程 `agent_demo/frontend/`，经 `/api` 调用（见《agent_demo开发计划.md》2.3）
- **SSE 用 GET**（EventSource 原生仅支持 GET）；开发联调经 Vite 代理 `/api` 转发
- 采用非流式 + SSE 流式双模式对话（对标 OpenAI/Anthropic/DashScope 企业标准，stream 参数切换两种模式）
- 非流式 `POST /api/chat` 用于后端任务调用和测试调试，流式 `GET /api/chat/stream` 用于前端交互
- 统一响应采用"HTTP 200 + 业务 code"模式，前端 Axios 拦截器按 `code` 判定
- 默认 userId=1（单用户演示场景）；预留 JWT（`Authorization: Bearer`）鉴权扩展（见 1.4）
- 会话管理独立 Controller（关注点分离）
- 提醒模块独立 Controller（对应 PRD 可选功能「打卡提醒」）
- API 路径版本化 `/api/v1`（本期可用 `/api`，兼容升级）

## Verification Steps
1. 接口文档覆盖 PRD 所有必做功能（打卡/历史/AI分析/建议/每日评价）
2. 接口文档覆盖 PRD 所有可选功能（提醒/周月报/图表/目标/达成率）
3. 每个模块接口与开发计划阶段对应
4. SSE 事件格式定义完整可实施
5. VO 数据模型覆盖所有响应场景
6. 已落地 35 个 `/api` 端点均有完整 JSON 请求/响应示例；待开发 16 个端点给出接口契约说明
