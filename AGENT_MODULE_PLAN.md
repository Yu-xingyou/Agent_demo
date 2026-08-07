# 生活习惯助手 Agent 模块 · 接口开发文档

> 本文档为 `habit-agent`（**单体 Spring Boot 4 应用，前后端分离**）新增 Agent 接口模块的**接口开发文档**。
> 后端统一以 `/api` 前缀提供 REST 接口，前端通过 Vite 代理 `/api` 转发，跨域由后端 `CorsConfig` 处理。
> 接口契约以**本项目实际代码与前端封装为准**（前端 `src/api/*.js`、后端 `Result`、`AgentConstants`、各 `Controller`），不套用外部模板的字段与路径。

---

## 1. 文档说明

### 1.1 适用范围
- 会话管理（`/api/sessions`）、流式 AI 对话（`/api/chat`）、历史消息、RAG 知识库（`/api/rag`）、习惯分析（`/api/analysis` / `/api/ai-analysis`）。
- 不在此文档范围：既有业务接口（`/api/habits`、`/api/goals`、`/api/reminders`，见各 `Controller`）。

### 1.2 Base URL
```
http://localhost:8080
```
前端 `src/utils/request.js` 中 `baseURL: '/api'`，开发期 Vite 已配置 `server.proxy['/api'] -> http://localhost:8080`，无需额外处理跨域。

### 1.3 统一响应结构
**本项目不使用幂等键（Idempotency-Key / requestId）**，也没有任何去重请求头。所有非流式接口统一返回后端 `Result<T>`：

```json
{ "code": 200, "message": "success", "data": {} }
```

- `code`：200 成功；非 200 为业务/系统错误码（见 `AgentConstants`：`40001` 参数校验、`40402` 记录不存在、`40901` 重复目标、`50001` 系统错误等）。
- `message`：提示信息，成功时固定为 `"success"`。
- `data`：业务数据，结构见各接口；无数据时省略或为 `null`。

> 说明：本项目的"幂等"仅出现在 RAG 导入接口的设计属性上（`/api/rag/import` 重复调用不会重复膨胀，靠文档标识去重，而非请求级幂等键），不涉及任何 `requestId` / `Idempotency-Key` 字段。

### 1.4 流式响应协议（SSE）
`GET /api/chat/stream` 采用 **Spring AI 2.0 真流式 SSE**，`Content-Type: text/event-stream`，以空行分隔事件，每个事件形如：
```
event: chunk
data: {"content":"好的","index":0}

event: done
data: {"conversationId":"...","streamingMode":"streaming"}
```
事件类型（`event` 字段）：

| event | data 字段 | 说明 |
|---|---|---|
| `meta` | `{ conversationId, timestamp, model, intent }` | 会话/模型/意图元数据，流开始时发出 |
| `tool_call` | `{ status, message, toolName }`（`status`: `start`/`end`） | 框架驱动工具调用轮次时发出 |
| `chunk` | `{ content, index }` | 真流式逐字增量分片，一条消息含多条 `chunk` |
| `done` | `{ conversationId, promptTokens, completionTokens, totalTokens, duration, firstTokenLatency, streamingMode }` | 流结束统计；`*Tokens` 可能为 `null`（DashScope 未回传 usage），前端需容忍 `null` |
| `error` | `{ errorCode, message, conversationId, retryable }` | 出错时发出 |

前端采用 Framework-Controlled 模式：工具调用循环由框架透明驱动，最终回复真流式逐字下推。

### 1.5 认证
本期为**单用户演示场景**，默认 `DEFAULT_USER_ID = 1`（`AgentConstants`）。接口不要求传用户身份，后端统一兜底为默认用户；无 `Authorization` 校验。

---

## 2. 会话接口（`/api/sessions`）

> 对应前端 `src/api/session.js`。会话 ID 在本项目中命名为 `conversationId`（非 `sessionId`）。

### 2.1 会话列表
`GET /api/sessions`

**返回示例**
```json
{
  "code": 200, "message": "success",
  "data": [
    { "conversationId": "f1dbf6ca0ed34eeda02ec0d0545a4429", "title": "今日作息记录", "updateTime": "2026-08-07 21:30:12" }
  ]
}
```
**data**：数组，每项 `{ conversationId:string, title:string, updateTime:string }`，按最后消息时间倒序。

**调用实例**
```bash
curl "http://localhost:8080/api/sessions"
```

---

### 2.2 会话详情（历史消息）
`GET /api/sessions/{conversationId}`

**返回示例**
```json
{
  "code": 200, "message": "success",
  "data": [
    { "role": "USER", "content": "帮我记录今天23点睡觉、喝了2升水" },
    { "role": "ASSISTANT", "content": "已为您记录：23:00 入睡、饮水 2000 毫升。" }
  ]
}
```
**data**：数组，每项 `{ role:"USER"|"ASSISTANT", content:string }`。

**调用实例**
```bash
curl "http://localhost:8080/api/sessions/f1dbf6ca0ed34eeda02ec0d0545a4429"
```

---

### 2.3 重命名会话
`PUT /api/sessions/{conversationId}/rename?title={title}`

**返回示例**
```json
{ "code": 200, "message": "success" }
```

**调用实例**
```bash
curl -X PUT "http://localhost:8080/api/sessions/f1dbf6ca0ed34eeda02ec0d0545a4429/rename?title=%E4%BB%8A%E6%81%AF%E8%AE%B0%E5%BD%95"
```

---

### 2.4 关闭会话
`POST /api/sessions/{conversationId}/close`

**返回示例**
```json
{ "code": 200, "message": "success" }
```

**调用实例**
```bash
curl -X POST "http://localhost:8080/api/sessions/f1dbf6ca0ed34eeda02ec0d0545a4429/close"
```

---

### 2.5 删除会话
`DELETE /api/sessions/{conversationId}`

**返回示例**
```json
{ "code": 200, "message": "success" }
```

**调用实例**
```bash
curl -X DELETE "http://localhost:8080/api/sessions/f1dbf6ca0ed34eeda02ec0d0545a4429"
```

---

## 3. 聊天接口（`/api/chat`）

> 对应前端 `src/api/chat.js`。用户消息字段命名为 `message`，会话字段命名为 `conversationId`（非 `question` / `sessionId`）。

### 3.1 发送消息（非流式）
`POST /api/chat`

**请求体**
```json
{ "message": "帮我记录今天23点睡觉、喝了2升水", "conversationId": "f1dbf6ca0ed34eeda02ec0d0545a4429" }
```
- `message`：用户消息（必填）。
- `conversationId`：会话 ID（可选，不传则新建会话）。

**返回示例**
```json
{ "code": 200, "message": "success", "data": { "conversationId": "f1dbf6ca0ed34eeda02ec0d0545a4429", "content": "已为您记录：23:00 入睡、饮水 2000 毫升。" } }
```

**调用实例**
```bash
curl -X POST "http://localhost:8080/api/chat" \
  -H "Content-Type: application/json" \
  -d '{"message":"帮我记录今天23点睡觉、喝了2升水"}'
```
**实现备注**：`POST /api/chat` 为一次性响应（不落库或不流式）；流式走 3.2。

---

### 3.2 流式对话（核心，SSE）
`GET /api/chat/stream?message={message}&conversationId={conversationId}`

**请求参数**

| 名称 | 位置 | 类型 | 必选 | 说明 |
|---|---|---|---|---|
| message | query | string | 是 | 用户消息 |
| conversationId | query | string | 否 | 会话 ID，不传则新建 |

**响应**：SSE 流，事件协议见 1.4。示例流（逐事件）：
```
event: meta
data: {"conversationId":"f1dbf6ca0ed34eeda02ec0d0545a4429","timestamp":1754573412000,"model":"qwen-plus","intent":"HABIT_RECORD"}

event: tool_call
data: {"status":"start","message":"调用习惯记录工具","toolName":"saveHabitRecord"}

event: chunk
data: {"content":"好的","index":0}

event: chunk
data: {"content":"，已为您记录今日作息。","index":1}

event: tool_call
data: {"status":"end","message":"工具调用完成","toolName":"saveHabitRecord"}

event: done
data: {"conversationId":"f1dbf6ca0ed34eeda02ec0d0545a4429","promptTokens":null,"completionTokens":null,"totalTokens":null,"duration":1820,"firstTokenLatency":420,"streamingMode":"streaming"}
```

**调用实例**
```bash
curl -N "http://localhost:8080/api/chat/stream?message=%E5%B8%AE%E6%88%91%E8%AE%B0%E5%BD%95%E4%BB%8A%E5%A4%A9%E4%BB%8A%E7%82%B9%E7%9D%A1%E8%A7%89"
```
**实现备注**：`ChatController.chatStream` → `ChatService`；`USER`/`ASSISTANT` 消息落库（MongoDB `chat_message`）；意图经 `RouterAgent` 结构化分类，子智能体复用现有 `HabitService` 等（Spring AI Tool 适配器）；`KNOWLEDGE_QA` 走 RAG 向量检索（见第 4 节）。

---

### 3.3 停止生成
`POST /api/chat/stop?conversationId={conversationId}`

**返回示例**
```json
{ "code": 200, "message": "success" }
```

**调用实例**
```bash
curl -X POST "http://localhost:8080/api/chat/stop?conversationId=f1dbf6ca0ed34eeda02ec0d0545a4429"
```
**实现备注**：取消该会话当前流式任务（内存 `Cancellable` / `AbortController`），流收到取消后输出 `done` 结束。

---

### 3.4 会话历史消息（按会话）
`GET /api/chat/history?conversationId={conversationId}`

**返回示例**
```json
{
  "code": 200, "message": "success",
  "data": [
    { "role": "USER", "content": "帮我记录今天23点睡觉" },
    { "role": "ASSISTANT", "content": "已记录：23:00 入睡。" }
  ]
}
```

**调用实例**
```bash
curl "http://localhost:8080/api/chat/history?conversationId=f1dbf6ca0ed34eeda02ec0d0545a4429"
```

---

### 3.5 生成会话标题
`POST /api/chat/title?message={message}`

**返回示例**
```json
{ "code": 200, "message": "success", "data": { "title": "今日作息记录" } }
```

**调用实例**
```bash
curl -X POST "http://localhost:8080/api/chat/title?message=%E5%B8%AE%E6%88%91%E8%AE%B0%E5%BD%95%E4%BB%8A%E5%A4%A9%E4%BB%8A%E7%82%B9%E7%9D%A1%E8%A7%89"
```

---

## 4. 知识库 RAG（`/api/rag`）

> 对应前端 `src/api/rag.js` 与 `RagController`。底层为 **MongoDB Atlas Vector Search**，文档分块 + 向量化后存入 `habit_knowledge` 集合。

### 4.1 导入预设知识文档
`POST /api/rag/import`

向量化耗时较长，前端单独放宽超时（120s）。**幂等（设计属性）**：重复调用不会重复膨胀，靠文档标识去重，**不依赖任何幂等键**。

**返回示例**
```json
{ "code": 200, "message": "success", "data": { "imported": 12 } }
```

**调用实例**
```bash
curl -X POST "http://localhost:8080/api/rag/import"
```

---

### 4.2 上传自定义文档
`POST /api/rag/upload`（multipart/form-data，字段 `file`，`.md`/`.txt`，≤ 2MB）

**返回示例**
```json
{ "code": 200, "message": "success", "data": { "docId": "64f...", "docType": "custom" } }
```

**调用实例**
```bash
curl -X POST "http://localhost:8080/api/rag/upload" -F "file=@睡眠指南.md"
```

---

### 4.3 语义检索
`GET /api/rag/search?query={query}&topK={topK}`

**返回示例**
```json
{
  "code": 200, "message": "success",
  "data": [
    { "content": "成年人建议每日饮水 1500-2000 毫升……", "score": 0.92, "docType": "diet" }
  ]
}
```
**data**：数组，`topK` 默认 3，每项 `{ content:string, score:number, docType:string }`。

**调用实例**
```bash
curl "http://localhost:8080/api/rag/search?query=%E5%A4%B1%E7%9C%A0%E6%80%8E%E4%B9%88%E5%8A%9E&topK=3"
```

---

### 4.4 文档片段列表
`GET /api/rag/documents?docType={docType}`

`docType` 可选：`sleep` / `exercise` / `diet` / `custom`。

**返回示例**
```json
{
  "code": 200, "message": "success",
  "data": [ { "id": "64f...", "docType": "sleep", "title": "睡眠指南", "chunkCount": 5 } ]
}
```

**调用实例**
```bash
curl "http://localhost:8080/api/rag/documents?docType=sleep"
```

---

### 4.5 删除文档片段
`DELETE /api/rag/documents/{id}`

**返回示例**
```json
{ "code": 200, "message": "success" }
```

**调用实例**
```bash
curl -X DELETE "http://localhost:8080/api/rag/documents/64f..."
```

---

## 5. 习惯分析接口

### 5.1 阶段九：结构化分析（`/api/analysis`）
> 对应 `AnalysisController`，供前端图表消费。

| 接口 | 说明 | data 类型 |
|---|---|---|
| `GET /api/analysis/trends?days=7` | 近 days 天睡眠/运动/饮水/心情序列 | `TrendDataVO` |
| `GET /api/analysis/overview?days=7` | 各维度平均值与打卡天数 | `Map<String,Object>` |
| `GET /api/analysis/achievement?days=7` | 各激活目标达成率与总体达成率 | `AchievementRateVO` |
| `GET /api/analysis/radar?days=7` | 睡眠/运动/饮水/饮食/心情五维 0-100 | `RadarDataVO` |

`userId` 可选，不传则使用 `DEFAULT_USER_ID`。

**返回示例（trends）**
```json
{
  "code": 200, "message": "success",
  "data": { "dates": ["08-01","08-02"], "sleep": [7.5, 8.0], "exercise": [30, 0], "water": [1500, 2000], "mood": [4, 5] }
}
```

---

### 5.2 阶段十：AI 智能分析（`/api/ai-analysis`）
> 对应前端 `src/api/aiAnalysis.js`。

| 接口 | 说明 |
|---|---|
| `POST /api/ai-analysis/generate?days=7` | 触发生成 AI 分析任务 |
| `GET /api/ai-analysis/{id}` | 查询指定分析任务 |
| `GET /api/ai-analysis/history?limit=10` | 历史分析列表 |
| `GET /api/ai-analysis/latest` | 最新一条分析 |
| `POST /api/ai-analysis/regenerate?days=7` | 重新生成 |

**返回示例（latest）**
```json
{
  "code": 200, "message": "success",
  "data": { "id": "a1", "days": 7, "summary": "本周睡眠时长较上周提升，饮水达标率 80%。", "createTime": "2026-08-07 21:00:00" }
}
```

---

## 6. 接口调用实例汇总（可复制）

```bash
# 会话
curl "http://localhost:8080/api/sessions"
curl "http://localhost:8080/api/sessions/f1dbf6ca0ed34eeda02ec0d0545a4429"
curl -X PUT "http://localhost:8080/api/sessions/f1dbf6ca0ed34eeda02ec0d0545a4429/rename?title=作息记录"
curl -X DELETE "http://localhost:8080/api/sessions/f1dbf6ca0ed34eeda02ec0d0545a4429"

# 聊天
curl -X POST "http://localhost:8080/api/chat" \
  -H "Content-Type: application/json" \
  -d '{"message":"帮我记录今天23点睡觉、喝了2升水"}'
curl -N "http://localhost:8080/api/chat/stream?message=查询近7天睡眠趋势"
curl -X POST "http://localhost:8080/api/chat/stop?conversationId=f1dbf6ca0ed34eeda02ec0d0545a4429"
curl "http://localhost:8080/api/chat/history?conversationId=f1dbf6ca0ed34eeda02ec0d0545a4429"
curl -X POST "http://localhost:8080/api/chat/title?message=帮我记录今天23点睡觉"

# 知识库
curl -X POST "http://localhost:8080/api/rag/import"
curl "http://localhost:8080/api/rag/search?query=失眠怎么办&topK=3"
curl "http://localhost:8080/api/rag/documents?docType=sleep"
curl -X DELETE "http://localhost:8080/api/rag/documents/64f..."

# 分析
curl "http://localhost:8080/api/analysis/trends?days=7"
curl "http://localhost:8080/api/ai-analysis/latest"
```

---

## 7. 配置与运行（单体应用）

本项目为**单一 Spring Boot 4 应用**，Agent 接口与既有习惯/目标/打卡业务共存于同一进程。运行需配置：

| 配置项 | 说明 | 位置 |
|---|---|---|
| `LOCAL_MONGO_URI` | 本地 MongoDB（会话/消息）连接串 | `application-local.yml` 占位 |
| `REMOTE_MONGO_URI` | 远程 MongoDB Atlas（向量库 `habit_knowledge`）连接串 | `application-local.yml` 占位 |
| `QWEN_API_KEY` | 千问 API Key（DashScope） | `application-local.yml` 占位 |
| `spring.ai.openai.base-url` | `https://dashscope.aliyuncs.com/compatible-mode/v1` | `application.yml` |
| `spring.ai.openai.chat.options.model` | `qwen-plus` | `application.yml` |
| `spring.ai.openai.embedding.options.model` | `text-embedding-v3` | `application.yml` |

新增依赖：`spring-ai-bom`、`spring-ai-starter-model-openai`、`spring-ai-starter-vector-store-mongodb`、`spring-boot-starter-data-mongodb`。

---

## 8. 风险与待确认（自我检查）

1. **Spring AI 2.0 版本 vs Spring Boot 4 兼容（高）**：须先验证 `spring-ai-bom` 存在匹配 SB4 版本，否则依赖无法解析，开工第 1 步前必验。
2. **DashScope 兼容 `/embeddings` 对 `text-embedding-v3` 开放（中）**：RAG embedding 是否支持需实测，否则改用原生 embedding 接口。
3. **MongoDB Atlas Vector Search 配置（中）**：`MongoVectorStore` 需正确配置 search index，否则检索失败。
4. **SSE 缓冲（低）**：须逐片 `flush` 且 Vite 代理去缓冲，否则前端收不到实时分片。
5. **`*Tokens` 可能为 null（低）**：前端须容忍 DashScope 未回传 usage 的情况。
6. **字段命名一致性（中）**：本项目统一用 `message` / `conversationId`（非外部模板的 `question` / `sessionId` / `requestId`），后端实现须与前端 `src/api/*.js` 严格对齐。
7. **RAG 导入幂等（低）**：`/api/rag/import` 靠文档标识去重，重复调用安全；无需也不引入幂等键机制。

---

*文档生成时间：2026-08-07 · 形态：接口开发文档（单体 Spring Boot 4 应用内 Agent 接口模块，契约以本项目代码与前端封装为准）*
