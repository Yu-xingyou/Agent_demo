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
`GET /api/chat/stream` 采用 **SSE（`text/event-stream`）**，**每行是一个 JSON**，靠 `eventType` / `eventData` 两个字段区分事件；**只回聊天内容本身，不回工具调用过程、不回 token 统计等附加信息**。

- 每个事件以 `data:` 前缀单行给出，事件之间以空行分隔（标准 SSE 分帧）。
- `eventType` 取值：
  - `1001` —— 数据事件（内容分片），`eventData` 为增量文本，逐字/逐句下推。
  - `1002` —— 停止事件（流结束，无 `eventData`）。
  - （原 `1003` 参数事件对应「工具调用消息」，本期不暴露，已去除。）
- 示例流：
```
data:{"eventData":"好的，已为您记录今日作息。","eventType":1001}

data:{"eventData":"","eventType":1001}

data:{"eventType":1002}
```
- 新会话的 `sessionId` 由后端在**流式响应头** `X-Session-Id` 下发（控制面信息，不进入事件流，满足「只回聊天记录」），前端据此写入会话上下文。
- 错误处理：不走 SSE 事件，直接以 HTTP 状态码表达（前端对 `res.ok` 校验 + `catch` 处理）。

### 1.5 认证
本期为**单用户演示场景**，默认 `DEFAULT_USER_ID = 1`（`AgentConstants`）。接口不要求传用户身份，后端统一兜底为默认用户；无 `Authorization` 校验。

### 1.6 术语说明：会话ID（sessionId）与 对话ID（conversationId）
本系统中「**会话**」与「**对话**」是两个不同的概念，对应不同的标识，**不可混用**：

| 概念 | 标识 | 说明 | 对应存储 |
|---|---|---|---|
| 会话（Session） | `sessionId` | 用户与助手之间的一段持续交互上下文；一个会话可包含多轮对话，支持列表 / 重命名 / 关闭 / 删除（见第 2 节） | MongoDB `chatSession`（**本地场景持久化、不自动删除，无 TTL 索引、实体无 status 字段**，见 8.4 节） |
| 对话（Conversation） | `conversationId` | 一次具体的问答线程 / 消息流；历史消息等对话内操作以此为准（见第 3 节 history 子接口） | MongoDB `chatMessage` |

> 约定：**会话管理类接口（第 2 节）与流式聊天类接口（第 3 节）统一使用 `sessionId`**（与 `ChatSession.sessionId` 及外部参考实现一致）；历史 / 停止等对话内操作沿用 `conversationId`。本期流式主链路已对齐示例采用 `sessionId`。

---

## 2. 会话接口（`/api/sessions`）

> 对应前端 `src/api/session.js`。本接口管理「会话」，统一使用 `sessionId`（与 `ChatSession.sessionId` 及第 3 节流式对话一致，见 1.6）。

### 2.1 会话列表
`GET /api/sessions`

**返回示例**
```json
{
  "code": 200, "message": "success",
  "data": [
    { "sessionId": "f1dbf6ca0ed34eeda02ec0d0545a4429", "title": "今日作息记录", "updateTime": "2026-08-07 21:30:12" }
  ]
}
```
**data**：数组，每项 `{ sessionId:string, title:string, updateTime:string }`，按最后消息时间倒序。

**调用实例**
```bash
curl "http://localhost:8080/api/sessions"
```

---

### 2.2 会话详情（历史消息）
`GET /api/sessions/{sessionId}`

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
`PUT /api/sessions/{sessionId}/rename?title={title}`

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
`POST /api/sessions/{sessionId}/close`

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
`DELETE /api/sessions/{sessionId}`

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

> 对应前端 `src/api/chat.js`。用户消息字段命名为 `message`，会话标识命名为 `sessionId`（对齐外部参考实现与 `ChatSession.sessionId`，见 1.6）。

### 3.1 发送消息（非流式）
`POST /api/chat`

**请求体**
```json
{ "message": "帮我记录今天23点睡觉、喝了2升水", "sessionId": "f1dbf6ca0ed34eeda02ec0d0545a4429" }
```
- `message`：用户消息（必填）。
- `sessionId`：会话 ID（可选，不传则新建会话）。

**返回示例**
```json
{ "code": 200, "message": "success", "data": { "sessionId": "f1dbf6ca0ed34eeda02ec0d0545a4429", "content": "已为您记录：23:00 入睡、饮水 2000 毫升。" } }
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
`POST /api/chat`，请求体为 `ChatMessageDTO`（`message` + `sessionId`），响应 `text/event-stream` 流式 `Flux<ChatEventVO>`（不被 `Result` 包裹，等价于 `@NoWrapper`）。

**请求体（application/json）**
```json
{
  "message": "帮我记录我今天 23 点睡觉",
  "sessionId": "c8f0e1a2-3b4d-4e5f"
}
```

| 字段 | 类型 | 必选 | 说明 |
|---|---|---|---|
| message | string | 是 | 用户消息 |
| sessionId | string | 否 | 会话 ID，不传则新建 |

**响应**：SSE 流，协议见 1.4。示例流（每行一个 JSON，事件之间空行分隔）：
```
data:{"eventData":"好的，已为您记录今日作息。","eventType":1001}

data:{"eventData":"","eventType":1001}

data:{"eventType":1002}
```
（新会话的 `sessionId` 通过响应头 `X-Session-Id` 下发，见 1.4。）

**调用实例**
```bash
curl -N -X POST "http://localhost:8080/api/chat" \
  -H "Content-Type: application/json" \
  -d '{"message":"帮我记录我今天 23 点睡觉"}'
```
**实现备注**：`ChatController.chat(@RequestBody ChatMessageDTO)` → `ChatService.chat(message, sessionId)`；`USER`/`ASSISTANT` 消息落库（MongoDB `chat_message`）；意图经 `RouterAgent` 结构化分类，子智能体复用现有 `HabitService` 等（Spring AI Tool 适配器）；`KNOWLEDGE_QA` 走 RAG 向量检索（见第 4 节）。本期已实现主链路（请求→流式回答），落库/路由/RAG 为后续扩展。

**会话记忆（多轮对话）**：已接入 Spring AI 2.0 的 `MessageChatMemoryAdvisor` + `MessageWindowChatMemory`，底层仓库使用官方 `MongoChatMemoryRepository`（替代参考实现中的 Redis 版），记忆持久化于 MongoDB 集合 `ai_chat_memory`。每次请求通过 advisor 参数 `ChatMemory.CONVERSATION_ID` 指定会话，会话标识规则为 `{userId}_{sessionId}`（参考实现为 `UserContext.getUser() + "_" + sessionId`，本项目单用户场景用 `AgentConstants.DEFAULT_USER_ID`）。`habit.ai.memory.max`（默认 100）控制滑动窗口最大消息数。

---

### 3.3 停止生成
`POST /api/chat/stop?sessionId={sessionId}`

**返回示例**
```json
{ "code": 200, "message": "success" }
```

**调用实例**
```bash
curl -X POST "http://localhost:8080/api/chat/stop?sessionId=f1dbf6ca0ed34eeda02ec0d0545a4429"
```
**实现备注**：`ChatController.stop` → `ChatService.stop(sessionId)`。服务端维护内存并发标记 `GENERATE_STATUS`（ConcurrentHashMap，key=sessionId），
流式 `chat` 在 `doFirst` 置 true、`doOnError`/`doOnComplete` 移除标记，并经 `takeWhile(status)` 控制流是否继续；
`stop` 移除该 sessionId 标记后，对应流的 `takeWhile` 判定为 false 而终止输出（随后由 `concatWith` 补发 1002 停止事件）。
单体实现；分布式环境可替换为 Redis。

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
| `LOCAL_MONGO_URI` | 本地 MongoDB（会话/消息）连接串 | `application-local.yml` 占位（环境变量） |
| `REMOTE_MONGO_URI` | 远程 MongoDB Atlas（向量库 `habit_knowledge`）连接串 | `application-local.yml` 占位（环境变量，明文不入库） |
| `QWEN_API_KEY` | 千问 API Key（DashScope） | `application-local.yml` 占位（环境变量） |
| `spring.ai.openai.base-url` | `https://dashscope.aliyuncs.com/compatible-mode/v1` | `application.yml` |
| `spring.ai.openai.chat.options.model` | `qwen-plus` | `application.yml` |
| `spring.ai.openai.embedding.options.model` | `text-embedding-v3` | `application.yml` |
| `spring.ai.vectorstore.mongodb.collection-name` | `habit_knowledge`（与 `mongo-init.js` 对齐） | `application.yml` |
| `spring.ai.vectorstore.mongodb.index-name` | `habit_knowledge_vector_index`（Atlas 向量检索索引） | `application.yml` |
| `spring.ai.vectorstore.mongodb.initialize-schema` | `false`（索引由 `mongo-init.js` 的 `createSearchIndex` 建） | `application.yml` |

新增依赖：`spring-ai-bom`、`spring-ai-starter-model-openai`、`spring-ai-starter-vector-store-mongodb-atlas`（Spring AI 2.0 中 MongoDB 向量库 starter 的正确 artifactId，非 `...-mongodb`）、`spring-boot-starter-data-mongodb`。

> **两套 MongoDB 连接分离说明**：`spring-boot-starter-data-mongodb` 与 `spring-ai-starter-vector-store-mongodb-atlas` 默认**共用同一个 `spring.data.mongodb.uri`**。本系统要求本地实例（会话/消息/记忆/分析）与远程 Atlas（向量库 `habit_knowledge`）分离，因此：
> - 本地实例：用 `spring.data.mongodb.host/port/database` 指向本地 `habit_agent`。
> - 远程向量库：通过独立的 `REMOTE_MONGO_URI` 环境变量注入，由**后续代码层**新增一个 `MongoDatabaseFactory`（或 `MongoClient`）Bean 绑定到 `REMOTE_MONGO_URI`，专供 `MongoDbAtlasVectorStore` 使用，避免与本地实例冲突。
> - `initialize-schema: false`，向量检索索引（`habit_knowledge_vector_index`）由 `sql/mongo-init.js` 的 `db.habit_knowledge.createSearchIndex(...)` 建立，不在应用启动时自动建。

---

## 8. 风险与待确认（自我检查）

1. **Spring AI 2.0 版本 vs Spring Boot 4 兼容（高）**：须先验证 `spring-ai-bom` 存在匹配 SB4 版本，否则依赖无法解析，开工第 1 步前必验。
2. **DashScope 兼容 `/embeddings` 对 `text-embedding-v3` 开放（中）**：RAG embedding 是否支持需实测，否则改用原生 embedding 接口。
3. **MongoDB Atlas Vector Search 配置（中）**：`MongoVectorStore` 需正确配置 search index，否则检索失败。
4. **SSE 缓冲（低）**：须逐片 `flush` 且 Vite 代理去缓冲，否则前端收不到实时分片。
5. **流式仅回聊天内容（低）**：SSE 只发 `eventType:1001`（内容）/ `1002`（结束），不含 token 统计；前端不得依赖任何统计字段。
6. **字段命名与标识一致性（中）**：非流式统一 `message` 与 `Result{code,message,data}`；流式主链路与第 2 节会话接口统一使用 `sessionId`（对齐外部参考实现与 `ChatSession.sessionId`），历史/停止等对话内操作沿用 `conversationId`；后端实现须与前端 `src/api/*.js` 严格对齐（前端当前 `session.js` 仍用 `conversationId` 作路径参数，须后续统一，见 1.6）。
7. **RAG 导入幂等（低）**：`/api/rag/import` 靠文档标识去重，重复调用安全；无需也不引入幂等键机制。
8. **会话持久化、不自动删除（低·已确认）**：本地场景 `chatSession` **不启用 TTL**，无 `expireAt` 字段、无 `ttl_expire_at` 索引（与之对应，`aiAnalysis` 的 DAILY 仍保留 1 天缓存 TTL）。`ChatSession` 实体与 `mongo-init.js` 已移除相关字段/索引，`sql/README.md` 索引表同步更新。
9. **`ChatSession` 实体字段规范（低·已确认）**：按演示规范，`ChatSession` 不含 `status` 冗余字段（「关闭会话」为轻量业务操作，不靠状态位表达），字段为 `id / sessionId / userId / title / createTime / updateTime / creater / updater`。原 `idx_user_status` 复合索引改为 `idx_user`（userId 单字段）。

---

## 9. 前端落地计划（流式协议改造）

> 目标：把流式对话契约从前一版错误的「Spring AI 具名事件（`meta`/`tool_call`/`chunk`/`done`/`error`）」纠正为第 1.4 / 3.2 节定义的**行式 `eventType` 协议（1001=内容分片 / 1002=结束）**，使「只回聊天记录本身」在前端彻底落地。后端本期无代码改动，仅以本文档约束。
> 对应开发计划（plan 状态 `ready`）：`doc-sse-protocol` → `chatjs-parse` → `view-toolstatus` → `verify-build`。

### 9.1 现状与不一致点
- `frontend/src/api/chat.js` 的 `streamMessage` 仍按 Spring AI 具名事件解析：`parseSseEvent` 依赖 `event:` 行，分发走 `meta/tool_call/chunk/done/error` 五分支，顶部 JSDoc 仍描述 DashScope token 字段。
- `frontend/src/views/AiChatView.vue` 仍存在 `toolStatus` 指示器（「工具调用中」加载态）、`onToolCall` 回调，以及 AI 回复底部的 Token 用量与耗时展示（`aiMsg.stats`），与「只回聊天记录、不回 token 统计」冲突。
- 上述二者与本文档 1.4 / 3.2 / 8-5 已定义的 `eventType` 行式协议严重不一致，需补齐前端落地。

### 9.2 改动清单

#### 9.2.1 `frontend/src/api/chat.js`（解析与分发层）
1. **解析层**：`parseSseEvent` 改为仅从 `data:` 行解析 JSON，取 `eventType` / `eventData` 字段（不再依赖 `event:` 行）；JSON 解析失败时跳过该事件，避免整条流中断。
2. **分发层**：`eventType === 1001` → `onChunk({ content: eventData })`；`eventType === 1002` → `onDone()`；删除 `meta`/`tool_call`/`chunk`/`done`/`error` 具名分支，删除 `onToolCall` 回调参数。
3. **会话 ID 透传**：`fetch` 解析后读取响应头 `X-Session-Id`（`res.headers.get('X-Session-Id')`），调用保留的 `onMeta(id)`（仅承载会话 ID，不进入事件流）。
4. **错误处理**：真实协议无 `error` 事件，保留 HTTP 级错误（`res.ok` 校验 + `catch` → `onError`），不新增 SSE 错误事件。
5. **注释**：更新顶部 JSDoc，移除 `tool_call` / token 统计相关描述，改为 `eventType` 1001/1002 语义。

#### 9.2.2 `frontend/src/views/AiChatView.vue`（UI 清理层）
1. 删除 `const toolStatus = ref('')` 及其全部读写：`onToolCall` 回调、`onChunk` 中清空、`onDone` 清空、`onError` 清空。
2. 删除模板中 `toolStatus` 指示器（发送中加载态块内「工具调用中」文案与脉冲点）。
3. 保留 `onMeta: (id) => { if (id) sessionId.value = id }`，仅将响应头的会话 ID 写入上下文。
4. `onDone` 不再写 `aiMsg.stats`（token 统计），`stats` 保持 `null`，模板 `v-if="m.stats"` 自动隐藏；保留 `sending=false`、`abortController=null` 与首轮标题生成调用。
5. 保留 `send` 函数首轮 `isFirstMessageRound` 逻辑与标题生成（依赖 `sessionId`），不受影响。

### 9.3 关键决策（与 1.4 一致）
- **会话 ID 走响应头而非新增事件**：事件流须纯净（只含聊天文本），`X-Session-Id` 是控制面信息，用响应头既满足需求又无需新增事件类型，且对 `onMeta` 调用方改动最小。
- **不新增 SSE `error` 事件**：协议仅定义 1001/1002，错误走 HTTP 状态码，避免协议膨胀与两端不一致。
- **复用而非重写**：保留现有 `fetch` / `ReadableStream` / 分帧缓冲（`buffer.split('\n\n')`）与 `AbortController` 中断逻辑，仅替换事件语义，降低回归风险。

### 9.4 验证与收尾
- 执行前用 code-explorer 二次核查 `frontend/src`（chat.js、AiChatView.vue、session.js 及其他引用 `toolStatus`/`onToolCall` 的组件），确保旧协议引用 100% 清除。
- 前端构建 / lint 验证无报错。
- 本文档 8-5 已明确「前端不得依赖任何统计字段」，本期改造后即满足；无需在文档层面新增约束。

---

*文档生成时间：2026-08-07 · 形态：接口开发文档（单体 Spring Boot 4 应用内 Agent 接口模块，契约以本项目代码与前端封装为准）*
