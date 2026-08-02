# API 接口文档设计方案 — 执行计划

## 任务概述

基于《agent_demo开发计划.md》和《详细模块开发流程.md》，创建一份完整的、可执行的 API 接口文档（MD 格式），覆盖全部 8 个后端功能模块、46 个 `/api` 接口端点。**每个接口必须包含完整的 JSON 请求示例和 JSON 响应示例**，将所有参数字段用实际值写出，而非仅用表格列 VO 类名。

> **前后端分离约定**：后端仅提供 REST API（无页面路由），前端为独立 Vue 3 + Vite 仓库 `habit-agent-web`，通过 `/api` 调用。本文档为纯 API 文档，不再描述 Thymeleaf 页面；文档同时以 OpenAPI 3.0 规范描述，便于 Swagger UI 浏览与前端通过 OpenAPI 生成请求代码。文档替换现有骨架文件 `API接口设计方案.md`。

## PRD 覆盖评审（本次优化）

对比 PRD 需求文档逐项检查，发现 2 项缺失功能已补充：

| PRD 功能项 | 覆盖状态 | 处理方式 |
|---|---|---|
| 每日录入作息/饮食/运动数据 | 已覆盖 | HabitController |
| 查看历史记录 | 已覆盖 | HabitController |
| AI 分析结果 | 已覆盖 | AiAnalysisController |
| 生活习惯改善建议 | 已覆盖 | AiAnalysisController suggestion 字段 |
| 周报/月报统计（可选） | 已覆盖 | AnalysisController + AiAnalysisController |
| 图表展示趋势（可选） | 已覆盖 | AnalysisController |
| 自定义习惯目标（可选） | 已覆盖 | GoalController |
| 达成率统计（可选） | 已覆盖 | AnalysisController |
| 周期趋势总结（输出数据） | 已覆盖 | AiAnalysisController |
| 生活习惯风险提示（输出数据） | 已覆盖 | AiAnalysisController riskWarning 字段 |
| **每日评价（输出数据）** | **缺失→已补充** | AiAnalysisController 新增 POST `/api/ai-analysis/daily` |
| **打卡提醒（可选功能）** | **缺失→已补充** | 新增 ReminderController `/api/reminders`（5 端点） |

同时根据企业标准规范（OpenAI Chat Completions API、Anthropic Messages API、阿里云 DashScope API 均通过 `stream` 参数同时支持流式与非流式两种模式），**ChatController 同时提供非流式对话 `POST /api/chat` 和 SSE 流式对话 `GET /api/chat/stream`**。非流式端点用于后端定时任务调用（如每日评价生成）、服务间调用（如周报/月报自动生成）和测试调试场景；流式端点用于前端用户交互，提供逐字打字机效果。

## 核心要求（用户反馈优化点）

每个接口在文档中的呈现格式必须如下：

```
### 接口名称

**请求方法** `POST` **路径** `/api/habits`

**请求参数**（JSON Body）

字段说明表格：字段名 | 类型 | 必填 | 说明

请求示例：
```json
{
  "recordDate": "2026-07-30",
  "sleepTime": "23:30",
  "wakeTime": "07:00",
  "sleepQuality": 4,
  ...
}
```

**响应示例**（成功 200）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "userId": 1,
    "recordDate": "2026-07-30",
    "sleepTime": "23:30",
    "wakeTime": "07:00",
    "sleepDuration": 7.5,
    ...
  }
}
```

**错误响应示例**：
```json
{
  "code": 40001,
  "message": "参数校验失败：sleepTime 不能为空",
  "data": null
}
```
```

GET 请求用 Query 参数表格 + 响应 JSON 示例；DELETE 请求无请求体，仅给响应 JSON 示例。

## 文档整体结构

输出路径：`d:\javacode\agent_demo\PRD\API接口设计方案.md`（替换现有骨架）

> **文档形态（纯 API）**：除 MD 文档外，后端通过 SpringDoc OpenAPI 自动生成 OpenAPI 3.0 描述，并挂载 Swagger UI（`/swagger-ui.html`）便于在线浏览与调试；前端可基于 OpenAPI 描述用 `openapi-typescript-codegen` 生成 TypeScript 请求客户端，保证前后端契约一致。本文档即是该 OpenAPI 的 Markdown 化呈现。

```
## 1. 文档概述（项目背景、前后端分离架构、技术栈、通用约定）
## 2. 通用规范（Result<T> JSON 格式、CORS、鉴权、API 版本化、错误码体系、日期时间格式）
## 3. SSE 流式输出规范（5种事件类型 JSON 格式、时序图、前端 EventSource 对接代码）
## 4. 接口总览表（46 端点速查矩阵）
## 5. 各模块接口详细设计（8 个后端模块，每接口含完整 JSON 请求/响应示例）
## 6. VO 数据模型清单（VO 类，每个含字段表格 + JSON 示例）
## 7. 错误码完整对照表
## 8. 前端消费方式（Axios 封装、Pinia 状态、Vue Router 与 API 映射、OpenAPI 代码生成）
## 9. 附录（接口与开发阶段映射表、数据存储分层、命名约定）
```

## 各模块接口 JSON 示例格式

以下展示每个模块接口在最终文档中的 JSON 示例格式。每个接口都包含：请求参数表格 + JSON 请求示例 + JSON 响应示例 + 错误响应示例。

### 5.1 习惯记录模块（HabitController `/api/habits`）— 7 端点

**端点 1：POST `/api/habits` — 录入/更新打卡**

请求参数（JSON Body）：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| recordDate | String (yyyy-MM-dd) | 是 | 打卡日期 |
| sleepTime | String (HH:mm) | 否 | 入睡时间 |
| wakeTime | String (HH:mm) | 否 | 起床时间 |
| sleepQuality | Integer | 否 | 睡眠质量 1-5 |
| dietDesc | String | 否 | 饮食描述 |
| dietScore | Integer | 否 | 饮食健康评分 1-5 |
| exerciseType | String | 否 | 运动类型 |
| exerciseDuration | Integer | 否 | 运动时长（分钟） |
| waterIntake | Integer | 否 | 饮水量（ml） |
| mood | Integer | 否 | 心情 1-5 |
| remark | String | 否 | 备注 |

请求示例：
```json
{
  "recordDate": "2026-07-30",
  "sleepTime": "23:30",
  "wakeTime": "07:00",
  "sleepQuality": 4,
  "dietDesc": "早餐燕麦，午餐鸡胸肉沙拉，晚餐轻食",
  "dietScore": 4,
  "exerciseType": "跑步",
  "exerciseDuration": 45,
  "waterIntake": 1800,
  "mood": 4,
  "remark": "今天感觉精神不错"
}
```

成功响应示例（200）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 15,
    "userId": 1,
    "recordDate": "2026-07-30",
    "sleepTime": "23:30",
    "wakeTime": "07:00",
    "sleepDuration": 7.50,
    "sleepQuality": 4,
    "dietDesc": "早餐燕麦，午餐鸡胸肉沙拉，晚餐轻食",
    "dietScore": 4,
    "exerciseType": "跑步",
    "exerciseDuration": 45,
    "waterIntake": 1800,
    "mood": 4,
    "remark": "今天感觉精神不错",
    "createTime": "2026-07-30T10:30:00",
    "updateTime": "2026-07-30T10:30:00"
  }
}
```

错误响应示例（400）：
```json
{
  "code": 40001,
  "message": "参数校验失败：recordDate 不能为空",
  "data": null
}
```

**端点 2：GET `/api/habits/today` — 查询今日记录**

请求参数：无

成功响应示例（200）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 15,
    "userId": 1,
    "recordDate": "2026-07-30",
    "sleepTime": "23:30",
    "wakeTime": "07:00",
    "sleepDuration": 7.50,
    "sleepQuality": 4,
    "dietDesc": "早餐燕麦，午餐鸡胸肉沙拉，晚餐轻食",
    "dietScore": 4,
    "exerciseType": "跑步",
    "exerciseDuration": 45,
    "waterIntake": 1800,
    "mood": 4,
    "remark": "今天感觉精神不错",
    "createTime": "2026-07-30T10:30:00",
    "updateTime": "2026-07-30T10:30:00"
  }
}
```

今日未打卡时响应示例（200，data 为 null）：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**端点 3：GET `/api/habits?startDate=&endDate=` — 按日期范围查询**

请求参数（Query）：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| startDate | String (yyyy-MM-dd) | 是 | 开始日期 |
| endDate | String (yyyy-MM-dd) | 是 | 结束日期 |

请求示例：`GET /api/habits?startDate=2026-07-23&endDate=2026-07-30`

成功响应示例（200）：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 15,
      "userId": 1,
      "recordDate": "2026-07-30",
      "sleepTime": "23:30",
      "wakeTime": "07:00",
      "sleepDuration": 7.50,
      "sleepQuality": 4,
      "dietDesc": "早餐燕麦，午餐鸡胸肉沙拉，晚餐轻食",
      "dietScore": 4,
      "exerciseType": "跑步",
      "exerciseDuration": 45,
      "waterIntake": 1800,
      "mood": 4,
      "remark": "今天感觉精神不错",
      "createTime": "2026-07-30T10:30:00",
      "updateTime": "2026-07-30T10:30:00"
    },
    {
      "id": 14,
      "userId": 1,
      "recordDate": "2026-07-29",
      "sleepTime": "00:15",
      "wakeTime": "07:30",
      "sleepDuration": 7.25,
      "sleepQuality": 3,
      "dietDesc": "午餐外卖，晚餐火锅",
      "dietScore": 2,
      "exerciseType": "散步",
      "exerciseDuration": 20,
      "waterIntake": 1200,
      "mood": 3,
      "remark": "",
      "createTime": "2026-07-29T09:00:00",
      "updateTime": "2026-07-29T09:00:00"
    }
  ]
}
```

**端点 4-7**：`GET /api/habits/recent/{days}`、`GET /api/habits/all`、`GET /api/habits/{id}`、`DELETE /api/habits/{id}` — 响应格式同上（单个或列表），DELETE 响应为 `{"code":200,"message":"success","data":null}`

### 5.2 习惯目标模块（GoalController `/api/goals`）— 6 端点

**端点 1：POST `/api/goals` — 创建目标**

请求参数（JSON Body）：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| goalType | String | 是 | 目标类型：SLEEP/EXERCISE/WATER/DIET |
| targetValue | Number | 是 | 目标值 |
| unit | String | 否 | 单位（如 hours/minutes/ml） |
| period | String | 否 | 周期：DAILY/WEEKLY/MONTHLY，默认 DAILY |
| isActive | Boolean | 否 | 是否启用，默认 true |

请求示例：
```json
{
  "goalType": "SLEEP",
  "targetValue": 8.0,
  "unit": "hours",
  "period": "DAILY",
  "isActive": true
}
```

成功响应示例（200）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "userId": 1,
    "goalType": "SLEEP",
    "targetValue": 8.0,
    "unit": "hours",
    "period": "DAILY",
    "isActive": true,
    "createTime": "2026-07-30T10:30:00",
    "updateTime": "2026-07-30T10:30:00"
  }
}
```

错误响应示例（409，同类型目标已存在）：
```json
{
  "code": 40901,
  "message": "该类型目标已存在，请使用 PUT 更新",
  "data": null
}
```

**端点 2-6**：`GET /api/goals`、`GET /api/goals/active`、`GET /api/goals/{type}`、`PUT /api/goals/{id}`、`DELETE /api/goals/{id}` — GET 响应为 `Result<List<HabitGoalVO>>` 或 `Result<HabitGoalVO>`，PUT 请求/响应同 POST 格式，DELETE 响应为 `{"code":200,"message":"success","data":null}`

GET 列表响应示例：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {"id":1,"userId":1,"goalType":"SLEEP","targetValue":8.0,"unit":"hours","period":"DAILY","isActive":true,"createTime":"2026-07-30T10:30:00","updateTime":"2026-07-30T10:30:00"},
    {"id":2,"userId":1,"goalType":"EXERCISE","targetValue":30.0,"unit":"minutes","period":"DAILY","isActive":true,"createTime":"2026-07-30T10:30:00","updateTime":"2026-07-30T10:30:00"},
    {"id":3,"userId":1,"goalType":"WATER","targetValue":2000.0,"unit":"ml","period":"DAILY","isActive":true,"createTime":"2026-07-30T10:30:00","updateTime":"2026-07-30T10:30:00"},
    {"id":4,"userId":1,"goalType":"DIET","targetValue":4.0,"unit":"score","period":"DAILY","isActive":false,"createTime":"2026-07-30T10:30:00","updateTime":"2026-07-30T10:30:00"}
  ]
}
```

### 5.3 AI 对话模块（ChatController `/api/chat`）— 3 端点

> 对标企业标准规范：OpenAI Chat Completions API 通过 `stream=true/false` 参数同时支持流式与非流式；Anthropic Messages API 同样通过 `stream` 布尔参数切换；阿里云 DashScope 通过 `stream` + `incremental_output` 参数组合支持。本项目遵循同一端点前缀 `/api/chat`，以不同 HTTP 方法和路径区分两种模式。

**端点 1：POST `/api/chat` — 非流式对话（企业标准）**

> 对标 OpenAI `POST /v1/chat/completions`（stream=false）和 DashScope（stream=false）。
> 适用场景：后端定时任务调用（如每日评价生成）、服务间调用（如周报/月报自动生成）、测试调试。

请求参数（JSON Body）：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| message | String | 是 | 用户消息 |
| conversationId | String | 否 | 会话 ID，不传则自动获取/创建 |

请求示例：
```json
{
  "message": "分析我最近的睡眠",
  "conversationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

成功响应示例（200）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "conversationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "role": "ASSISTANT",
    "content": "根据您最近一周的睡眠记录，平均睡眠时长为7.25小时，达标率85.7%。建议每天提前30分钟入睡，保持规律作息。",
    "metadata": {
      "tokensUsed": 356,
      "model": "qwen-plus",
      "duration": 3200
    },
    "createTime": "2026-07-30T10:30:00"
  }
}
```

错误响应示例（503）：
```json
{
  "code": 50301,
  "message": "AI 服务暂时不可用",
  "data": null
}
```

**端点 2：GET `/api/chat/stream` — SSE 流式对话**

请求参数（Query）：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| message | String | 是 | 用户消息（需 URL 编码） |
| conversationId | String | 否 | 会话 ID，不传则自动获取/创建 |

请求示例：`GET /api/chat/stream?message=分析我最近的睡眠&conversationId=a1b2c3d4-e5f6-7890-abcd-ef1234567890`

响应格式（SSE 事件流，非 JSON Result 封装）：

```
event: meta
data: {"conversationId":"a1b2c3d4-e5f6-7890-abcd-ef1234567890","timestamp":"2026-07-30T10:30:00","model":"qwen-plus"}

event: tool_call
data: {"toolName":"getRecentHabits","arguments":{"userId":1,"days":7},"result":"查询到7条记录"}

event: chunk
data: {"content":"根据您最近","index":0}

event: chunk
data: {"content":"一周的睡眠","index":1}

event: chunk
data: {"content":"记录...","index":2}

event: done
data: {"conversationId":"a1b2c3d4-e5f6-7890-abcd-ef1234567890","totalTokens":356,"duration":3200,"intent":"DATA_ANALYSIS"}
```

错误事件示例：
```
event: error
data: {"errorCode":"AI_TIMEOUT","message":"AI 响应超时","conversationId":"a1b2c3d4-e5f6-7890-abcd-ef1234567890"}
```

**端点 3：POST `/api/chat/stop` — 停止生成**

请求参数（Query）：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| conversationId | String | 是 | 要停止的会话 ID |

请求示例：`POST /api/chat/stop?conversationId=a1b2c3d4-e5f6-7890-abcd-ef1234567890`

成功响应示例（200）：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### 5.4 会话管理模块（SessionController `/api/sessions`）— 7 端点

**端点 1：GET `/api/sessions` — 用户会话列表**

请求参数：无

成功响应示例（200）：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "conversationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "userId": 1,
      "title": "睡眠分析对话",
      "status": "ACTIVE",
      "messageCount": 12,
      "lastMessageTime": "2026-07-30T10:30:00",
      "createTime": "2026-07-29T10:00:00",
      "expireAt": "2026-08-05T10:00:00"
    },
    {
      "conversationId": "b2c3d4e5-f6a7-8901-bcde-f23456789012",
      "userId": 1,
      "title": "运动建议",
      "status": "CLOSED",
      "messageCount": 6,
      "lastMessageTime": "2026-07-28T15:00:00",
      "createTime": "2026-07-28T14:00:00",
      "expireAt": "2026-08-04T14:00:00"
    }
  ]
}
```

**端点 2：POST `/api/sessions` — 创建新会话**

请求参数（Query）：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| title | String | 否 | 会话标题，不传则默认"新对话" |

请求示例：`POST /api/sessions?title=睡眠分析对话`

成功响应示例（200）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "conversationId": "c3d4e5f6-a7b8-9012-cdef-345678901234",
    "userId": 1,
    "title": "睡眠分析对话",
    "status": "ACTIVE",
    "messageCount": 0,
    "lastMessageTime": null,
    "createTime": "2026-07-30T10:30:00",
    "expireAt": "2026-08-06T10:30:00"
  }
}
```

**端点 3-7**：`GET /api/sessions/active`、`GET /api/sessions/{conversationId}`、`DELETE /api/sessions/{conversationId}`、`GET /api/sessions/{conversationId}/messages`、`PUT /api/sessions/{conversationId}/close`

消息历史响应示例（200）：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": "60f1a2b3c4d5e6f7a8b9c0d1",
      "conversationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "role": "USER",
      "content": "分析一下我最近一周的睡眠",
      "metadata": null,
      "createTime": "2026-07-30T10:29:00"
    },
    {
      "id": "60f1a2b3c4d5e6f7a8b9c0d2",
      "conversationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "role": "ASSISTANT",
      "content": "根据您最近一周的睡眠记录，平均睡眠时长为7.2小时...",
      "metadata": {"tokensUsed": 356, "model": "qwen-plus"},
      "createTime": "2026-07-30T10:30:00"
    }
  ]
}
```

会话不存在错误响应示例（404）：
```json
{
  "code": 40403,
  "message": "会话不存在或已过期",
  "data": null
}
```

### 5.5 趋势分析模块（AnalysisController `/api/analysis`）— 4 端点

**端点 1：GET `/api/analysis/trends?days=30` — 趋势数据**

请求参数（Query）：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| days | Integer | 否 | 天数，默认 30 |

请求示例：`GET /api/analysis/trends?days=7`

成功响应示例（200）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "dates": ["2026-07-24","2026-07-25","2026-07-26","2026-07-27","2026-07-28","2026-07-29","2026-07-30"],
    "sleepDurations": [7.5, 6.8, 7.2, 8.0, 6.5, 7.25, 7.5],
    "exerciseDurations": [45, 0, 30, 60, 0, 20, 45],
    "waterIntakes": [1800, 1500, 2000, 2200, 1200, 1200, 1800]
  }
}
```

**端点 2：GET `/api/analysis/achievement?period=WEEKLY` — 达成率**

请求参数（Query）：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| period | String | 否 | 周期：WEEKLY/MONTHLY，默认 WEEKLY |

请求示例：`GET /api/analysis/achievement?period=WEEKLY`

成功响应示例（200）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "sleepRate": 85.7,
    "exerciseRate": 57.1,
    "waterRate": 71.4,
    "dietRate": 62.9,
    "overallRate": 69.3,
    "period": "WEEKLY"
  }
}
```

**端点 3：GET `/api/analysis/overview?days=7` — 概览统计**

成功响应示例（200）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "avgSleepDuration": 7.25,
    "avgExerciseDuration": 28,
    "avgWaterIntake": 1671,
    "avgMood": 3.4,
    "totalDays": 7,
    "checkedDays": 7
  }
}
```

**端点 4：GET `/api/analysis/radar?period=WEEKLY` — 雷达图数据**

成功响应示例（200）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "indicators": ["睡眠","运动","饮水","饮食"],
    "values": [85.7, 57.1, 71.4, 62.9],
    "targets": [100.0, 100.0, 100.0, 100.0]
  }
}
```

### 5.6 AI 分析结果模块（AiAnalysisController `/api/ai-analysis`）— 6 端点

**端点 1：GET `/api/ai-analysis?type=WEEKLY` — AI 分析列表**

请求参数（Query）：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| type | String | 否 | 分析类型：WEEKLY/MONTHLY/CUSTOM，默认 WEEKLY |

请求示例：`GET /api/ai-analysis?type=WEEKLY`

成功响应示例（200）：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": "60f1a2b3c4d5e6f7a8b9c0d3",
      "userId": 1,
      "analysisType": "WEEKLY",
      "periodStart": "2026-07-23",
      "periodEnd": "2026-07-30",
      "content": "## 本周习惯分析\n\n### 睡眠\n平均睡眠时长7.25小时，达标率85.7%...\n### 运动\n本周运动3天...",
      "suggestion": "1. 建议每天提前30分钟入睡\n2. 增加运动频率至每周5次\n3. 保持每日饮水量2000ml以上",
      "riskWarning": "运动量连续3天不足，建议增加日常活动量",
      "metadata": {"tokensUsed": 800, "model": "qwen-plus"},
      "createTime": "2026-07-30T11:00:00"
    }
  ]
}
```

**端点 2-5**：`GET /api/ai-analysis/latest`、`GET /api/ai-analysis/{id}`、`POST /api/ai-analysis/trigger`、`DELETE /api/ai-analysis/{id}`

触发分析请求示例：
```json
{
  "type": "WEEKLY",
  "startDate": "2026-07-23",
  "endDate": "2026-07-30"
}
```

触发分析成功响应示例（200）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "60f1a2b3c4d5e6f7a8b9c0d4",
    "userId": 1,
    "analysisType": "WEEKLY",
    "periodStart": "2026-07-23",
    "periodEnd": "2026-07-30",
    "content": "## 本周习惯分析\n\n### 睡眠\n平均睡眠时长7.25小时...",
    "suggestion": "1. 建议每天提前30分钟入睡\n2. 增加运动频率至每周5次",
    "riskWarning": "运动量连续3天不足",
    "metadata": {"tokensUsed": 850, "model": "qwen-plus"},
    "createTime": "2026-07-30T11:05:00"
  }
}
```

**端点 6：POST `/api/ai-analysis/daily` — 生成每日评价**

> 对应 PRD 输出数据「每日评价」功能，基于当日打卡数据生成 AI 当日评价。

请求参数（JSON Body）：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| recordDate | String (yyyy-MM-dd) | 否 | 评价日期，不传则默认今天 |

请求示例：
```json
{
  "recordDate": "2026-07-30"
}
```

成功响应示例（200）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "60f1a2b3c4d5e6f7a8b9c0d5",
    "userId": 1,
    "analysisType": "DAILY",
    "recordDate": "2026-07-30",
    "content": "## 今日评价\n\n睡眠时长7.5小时，质量良好。饮食结构均衡，运动量达标。饮水量充足，整体状态优秀。",
    "score": 85,
    "suggestion": "建议明天保持当前作息节奏，可尝试增加10分钟拉伸运动以进一步提升恢复质量",
    "riskWarning": null,
    "metadata": {"tokensUsed": 320, "model": "qwen-plus"},
    "createTime": "2026-07-30T22:00:00"
  }
}
```

当日未打卡时错误响应示例（400）：
```json
{
  "code": 40004,
  "message": "当日尚未打卡，请先完成打卡再生成评价",
  "data": null
}
```

### 5.7 RAG 知识库模块（RagController `/api/rag`）— 5 端点

**端点 1：POST `/api/rag/import` — 导入预设文档**

请求参数：无（导入 `rag-docs/` 下所有文档）

成功响应示例（200）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalDocs": 3,
    "totalChunks": 15,
    "success": true,
    "errors": []
  }
}
```

部分失败响应示例（200）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalDocs": 3,
    "totalChunks": 12,
    "success": false,
    "errors": ["exercise-guide.md 第3段向量化失败：Embedding 服务超时"]
  }
}
```

**端点 2：POST `/api/rag/upload` — 上传文档**

请求参数（multipart/form-data）：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| file | File | 是 | 文档文件，支持 .md/.txt |

请求示例（curl）：
```bash
curl -X POST http://localhost:8080/api/rag/upload \
  -F "file=@sleep-guide.md"
```

成功响应示例（200）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": "60f1a2b3c4d5e6f7a8b9c0d5",
    "content": "成人每天建议睡眠7-9小时...",
    "metadata": {
      "doc_type": "sleep_guide",
      "source": "sleep-guide.md",
      "chunk_index": 0
    }
  }
}
```

**端点 3：GET `/api/rag/search?query=&topK=3` — 语义检索**

请求参数（Query）：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| query | String | 是 | 检索关键词（需 URL 编码） |
| topK | Integer | 否 | 返回结果数，默认 3，范围 1-10 |

请求示例：`GET /api/rag/search?query=每天应该睡多久&topK=3`

成功响应示例（200）：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "content": "成人每天建议睡眠7-9小时，最佳入睡时间为22:00-23:00...",
      "score": 0.92,
      "metadata": {
        "doc_type": "sleep_guide",
        "source": "sleep-guide.md",
        "chunk_index": 0
      }
    },
    {
      "content": "睡眠质量评估标准：入睡时间<30分钟为优秀...",
      "score": 0.78,
      "metadata": {
        "doc_type": "sleep_guide",
        "source": "sleep-guide.md",
        "chunk_index": 1
      }
    }
  ]
}
```

**端点 4-5**：`GET /api/rag/documents`、`DELETE /api/rag/documents/{id}` — 列表响应同 search 格式但无 score 字段，DELETE 响应为 `{"code":200,"message":"success","data":null}`

### 5.8 前端页面路由（Vue Router，前端独立仓库）

> 后端已无页面路由（PageController 移除）。以下 5 个页面由前端 `habit-agent-web` 的 Vue Router 实现，均为 SPA 客户端路由，调用对应 `/api` 端点获取数据：

| 前端路由 | 对应页面 | 主要调用的后端接口 |
|---|---|---|
| `/` | 首页 HomeView | `GET /api/habits/today`、`GET /api/habits/recent/{days}` |
| `/checkin` | 每日打卡 CheckinView | `GET /api/habits/today`、`POST /api/habits` |
| `/history` | 历史记录 HistoryView | `GET /api/habits?startDate=&endDate=` |
| `/trend` | 趋势分析 TrendView | `GET /api/analysis/*` |
| `/ai-chat` | AI 建议 AiChatView | `POST /api/chat`、`GET /api/chat/stream`、`POST /api/chat/stop` |

> 前端页面交互细节见《详细模块开发流程.md》阶段三与《agent_demo开发计划.md》2.3。

### 5.9 打卡提醒模块（ReminderController `/api/reminders`）— 5 端点

> 对应 PRD 可选功能「打卡提醒」，支持用户自定义提醒时间和方式，定时推送打卡通知。

**端点 1：GET `/api/reminders` — 获取提醒列表**

请求参数：无

成功响应示例（200）：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "userId": 1,
      "title": "睡眠打卡提醒",
      "reminderTime": "23:00",
      "reminderType": "SLEEP",
      "isActive": true,
      "weekdays": [1, 2, 3, 4, 5],
      "createTime": "2026-07-30T10:30:00",
      "updateTime": "2026-07-30T10:30:00"
    },
    {
      "id": 2,
      "userId": 1,
      "title": "饮水提醒",
      "reminderTime": "10:00",
      "reminderType": "WATER",
      "isActive": true,
      "weekdays": [1, 2, 3, 4, 5, 6, 7],
      "createTime": "2026-07-30T10:30:00",
      "updateTime": "2026-07-30T10:30:00"
    }
  ]
}
```

**端点 2：POST `/api/reminders` — 创建提醒**

请求参数（JSON Body）：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| title | String | 是 | 提醒标题 |
| reminderTime | String (HH:mm) | 是 | 提醒时间 |
| reminderType | String | 是 | 提醒类型：SLEEP/DIET/EXERCISE/WATER/CUSTOM |
| weekdays | Array\<Integer\> | 否 | 星期几触发，1-7（周一到周日），不传则每天触发 |
| isActive | Boolean | 否 | 是否启用，默认 true |

请求示例：
```json
{
  "title": "运动打卡提醒",
  "reminderTime": "18:00",
  "reminderType": "EXERCISE",
  "weekdays": [1, 2, 3, 4, 5],
  "isActive": true
}
```

成功响应示例（200）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 3,
    "userId": 1,
    "title": "运动打卡提醒",
    "reminderTime": "18:00",
    "reminderType": "EXERCISE",
    "isActive": true,
    "weekdays": [1, 2, 3, 4, 5],
    "createTime": "2026-07-30T10:35:00",
    "updateTime": "2026-07-30T10:35:00"
  }
}
```

错误响应示例（400）：
```json
{
  "code": 40001,
  "message": "参数校验失败：reminderTime 格式必须为 HH:mm",
  "data": null
}
```

**端点 3：PUT `/api/reminders/{id}` — 更新提醒**

请求参数（JSON Body）：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| title | String | 否 | 提醒标题 |
| reminderTime | String (HH:mm) | 否 | 提醒时间 |
| reminderType | String | 否 | 提醒类型 |
| weekdays | Array\<Integer\> | 否 | 星期几触发 |
| isActive | Boolean | 否 | 是否启用 |

请求示例：`PUT /api/reminders/3`

```json
{
  "reminderTime": "18:30",
  "weekdays": [1, 2, 3, 4, 5, 6]
}
```

成功响应示例（200）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 3,
    "userId": 1,
    "title": "运动打卡提醒",
    "reminderTime": "18:30",
    "reminderType": "EXERCISE",
    "isActive": true,
    "weekdays": [1, 2, 3, 4, 5, 6],
    "createTime": "2026-07-30T10:35:00",
    "updateTime": "2026-07-30T10:40:00"
  }
}
```

提醒不存在错误响应示例（404）：
```json
{
  "code": 40406,
  "message": "提醒不存在",
  "data": null
}
```

**端点 4：POST `/api/reminders/toggle/{id}` — 启用/禁用提醒**

请求参数：无（在 URL 中传 id，切换启用状态）

请求示例：`POST /api/reminders/toggle/3`

成功响应示例（200）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 3,
    "isActive": false,
    "updateTime": "2026-07-30T10:45:00"
  }
}
```

**端点 5：DELETE `/api/reminders/{id}` — 删除提醒**

请求示例：`DELETE /api/reminders/3`

成功响应示例（200）：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

## SSE 事件格式定义

5 种事件类型，每种都有完整 JSON data 格式：

| 事件 | 触发时机 | JSON data 格式 |
|---|---|---|
| `meta` | 流开始 | `{"conversationId":"uuid","timestamp":"2026-07-30T10:30:00","model":"qwen-plus"}` |
| `chunk` | Token 片段 | `{"content":"你好","index":0}` |
| `tool_call` | 工具调用 | `{"toolName":"getRecentHabits","arguments":{"userId":1,"days":7},"result":"查询到7条记录"}` |
| `done` | 流结束 | `{"conversationId":"uuid","totalTokens":356,"duration":3200,"intent":"DATA_ANALYSIS"}` |
| `error` | 流出错 | `{"errorCode":"AI_TIMEOUT","message":"AI 响应超时","conversationId":"uuid"}` |

文档中包含完整的事件序列时序图和前端 EventSource 对接代码示例。

## VO 数据模型清单（22 个）

每个 VO 在文档中用两种方式呈现：字段定义表格 + 完整 JSON 示例。

| 类别 | VO 类 | 数量 |
|---|---|---|
| 通用 | `Result<T>`、`ErrorResponseVO` | 2 |
| 习惯记录 | `HabitRecordVO`、`HabitRecordFormVO` | 2 |
| 目标管理 | `HabitGoalVO`、`HabitGoalFormVO` | 2 |
| AI 对话 | `ChatRequestVO`、`ChatResponseVO`、`ChatStopVO` | 3 |
| 会话管理 | `ChatSessionVO`、`ChatMessageVO` | 2 |
| 趋势分析 | `TrendDataVO`、`AchievementRateVO`、`AnalysisOverviewVO`、`RadarDataVO` | 4 |
| AI 分析结果 | `AiAnalysisVO`、`AiAnalysisTriggerVO`、`DailyEvaluationVO` | 3 |
| RAG 知识库 | `RagDocumentVO`、`RagSearchResultVO`、`ImportResultVO` | 3 |
| 打卡提醒 | `ReminderVO` | 1 |

示例（HabitRecordVO 在文档中的呈现）：

字段定义：
| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 记录 ID |
| userId | Long | 用户 ID |
| recordDate | String | 打卡日期 yyyy-MM-dd |
| sleepTime | String | 入睡时间 HH:mm |
| wakeTime | String | 起床时间 HH:mm |
| sleepDuration | BigDecimal | 睡眠时长（小时，服务端自动计算） |
| sleepQuality | Integer | 睡眠质量 1-5 |
| ... | ... | ... |

JSON 示例：
```json
{
  "id": 15,
  "userId": 1,
  "recordDate": "2026-07-30",
  "sleepTime": "23:30",
  "wakeTime": "07:00",
  "sleepDuration": 7.50,
  "sleepQuality": 4,
  "dietDesc": "早餐燕麦，午餐鸡胸肉沙拉，晚餐轻食",
  "dietScore": 4,
  "exerciseType": "跑步",
  "exerciseDuration": 45,
  "waterIntake": 1800,
  "mood": 4,
  "remark": "今天感觉精神不错",
  "createTime": "2026-07-30T10:30:00",
  "updateTime": "2026-07-30T10:30:00"
}
```

## 错误码体系（19 个）

| HTTP | 业务码 | 名称 | 场景 | 响应示例 |
|---|---|---|---|---|
| 200 | 200 | SUCCESS | 成功 | `{"code":200,"message":"success","data":{...}}` |
| 400 | 40001 | INVALID_PARAM | 参数校验失败 | `{"code":40001,"message":"recordDate 不能为空","data":null}` |
| 400 | 40002 | INVALID_DATE_RANGE | 日期范围无效 | `{"code":40002,"message":"startDate 不能晚于 endDate","data":null}` |
| 400 | 40003 | INVALID_ENUM_VALUE | 枚举值不合法 | `{"code":40003,"message":"goalType 必须为 SLEEP/EXERCISE/WATER/DIET 之一","data":null}` |
| 400 | 40004 | DAILY_NO_RECORD | 当日未打卡无法生成评价 | `{"code":40004,"message":"当日尚未打卡，请先完成打卡再生成评价","data":null}` |
| 404 | 40401 | HABIT_NOT_FOUND | 习惯记录不存在 | `{"code":40401,"message":"习惯记录不存在","data":null}` |
| 404 | 40402 | GOAL_NOT_FOUND | 目标不存在 | `{"code":40402,"message":"目标不存在","data":null}` |
| 404 | 40403 | SESSION_NOT_FOUND | 会话不存在或已过期 | `{"code":40403,"message":"会话不存在或已过期","data":null}` |
| 404 | 40404 | ANALYSIS_NOT_FOUND | AI 分析结果不存在 | `{"code":40404,"message":"AI 分析结果不存在","data":null}` |
| 404 | 40405 | DOCUMENT_NOT_FOUND | RAG 文档不存在 | `{"code":40405,"message":"文档不存在","data":null}` |
| 404 | 40406 | REMINDER_NOT_FOUND | 提醒不存在 | `{"code":40406,"message":"提醒不存在","data":null}` |
| 409 | 40901 | DUPLICATE_GOAL | 同类型目标已存在 | `{"code":40901,"message":"该类型目标已存在","data":null}` |
| 429 | 42901 | AI_RATE_LIMIT | AI 调用频率过高 | `{"code":42901,"message":"请求过于频繁，请3秒后重试","data":null}` |
| 503 | 50301 | AI_SERVICE_UNAVAILABLE | AI 服务不可用 | `{"code":50301,"message":"AI 服务暂时不可用","data":null}` |
| 503 | 50302 | AI_TIMEOUT | AI 调用超时 | `{"code":50302,"message":"AI 响应超时","data":null}` |
| 503 | 50303 | AI_SAFETY_BLOCKED | 输入被安全过滤器拦截 | `{"code":50303,"message":"输入内容被安全过滤拦截","data":null}` |
| 500 | 50001 | INTERNAL_ERROR | 服务器内部错误 | `{"code":50001,"message":"服务器内部错误","data":null}` |
| 500 | 50002 | DATABASE_ERROR | 数据库操作异常 | `{"code":50002,"message":"数据库操作异常","data":null}` |
| 500 | 50003 | VECTOR_STORE_ERROR | 向量存储操作异常 | `{"code":50003,"message":"向量存储操作异常","data":null}` |

## 接口与开发阶段映射

| 开发阶段 | 模块 | 端点数 |
|---|---|---|
| 阶段二 | HabitController + GoalController | 13 |
| 阶段三 | 前端独立仓库 SPA 路由（不占后端端点） | 0 |
| 阶段四 | Spring AI 配置（ChatClient + SystemPrompt，无对外端点） | 0 |
| 阶段五 | ChatController 非流式+SSE+停止 + SessionController | 10 |
| 阶段七 | RagController | 5 |
| 阶段九 | AnalysisController | 4 |
| 阶段十 | AiAnalysisController + ReminderController | 11 |
| **合计（后端 REST 端点，按实际代码落地）** | | **46** |

> 说明：目标模块含 5 个自定义目标打卡记录扩展接口（`/api/goal-records/records`），故 Controller 实际端点总数为 46。与《API接口设计方案.md》口径一致。

## 关键设计决策

| 决策点 | 选择 | 理由 |
|---|---|---|
| SSE 用 GET 还是 POST | GET | EventSource 原生仅支持 GET |
| 对话方式 | 非流式 + SSE 流式 | 企业标准规范（OpenAI/Anthropic/DashScope 均同时支持两种模式）；非流式用于后端任务调用和测试，流式用于前端交互 |
| 默认 userId | 1 | 单用户演示场景 |
| 会话管理是否独立 Controller | 是 | 关注点分离 |
| SSE 是否经过 Result 封装 | 否 | 直接发送 SSE 事件 |
| 日期格式 | yyyy-MM-dd / HH:mm / ISO 8601 时间戳 | Jackson 默认支持 |
| 分页 | 暂不需要 | 演示项目数据量小 |

## 实施步骤

1. 编写文档头部与通用规范（第 1-2 章）：Base URL、Result<T> 的 JSON 格式定义、错误码框架、日期时间格式
2. 编写 SSE 规范章节（第 3 章）：5 种事件类型的完整 JSON data 格式、时序图、前端 EventSource 对接代码示例、停止机制
3. 编写接口总览表（第 4 章）：46 端点速查矩阵
4. 逐模块编写接口详情（第 5 章）：每个接口包含：
   - 请求方法 + 路径
   - 请求参数表格（字段名/类型/必填/说明）
   - **完整 JSON 请求示例**（所有字段带实际值）
   - **完整 JSON 成功响应示例**（所有字段带实际值）
   - **JSON 错误响应示例**（该接口可能的错误码）
   - 开发阶段标注
5. 编写 VO 数据模型清单（第 6 章）：22 个 VO 类，每个含字段定义表格 + 完整 JSON 示例
6. 编写错误码完整对照表（第 7 章）：19 个错误码，每个含 JSON 响应示例
7. 编写附录（第 8 章）：接口与开发阶段映射、数据存储分层、命名约定

## 验证步骤

1. 每个 REST 接口都有完整 JSON 请求示例（GET 请求有 Query 参数示例 URL）
2. 每个 REST 接口都有完整 JSON 成功响应示例（所有字段带实际值）
3. 每个 REST 接口都有至少一个 JSON 错误响应示例
4. SSE 每种事件类型都有完整 JSON data 格式示例
5. 22 个 VO 类都有字段表格 + JSON 示例
6. 19 个错误码都有 JSON 响应示例
7. 文档覆盖 PRD 所有必做功能（打卡/历史/AI分析/建议）
8. 46 个端点的 JSON 示例可直接复制到 Postman 测试

## 假设与约束

- 文档为 MD 格式，保存到 `d:\javacode\agent_demo\.trae\documents\API接口设计方案.md`
- 所有接口默认 userId=1（单用户演示场景）
- SSE 端点不经过 Result<T> 封装，其余 REST 端点统一用 Result<T>
- VO 字段命名与开发计划中的 Java 实体/文档模型严格一致
- 所有 JSON 示例中的值使用合理的模拟数据（与 DataInitializer 生成的 14 天数据风格一致）
