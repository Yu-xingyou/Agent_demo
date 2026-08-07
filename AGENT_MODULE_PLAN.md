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
- 新对话的 `conversationId` 由后端在**流式响应头** `X-Conversation-Id` 下发（控制面信息，不进入事件流，满足「只回聊天记录」），前端据此写入会话上下文。
- 错误处理：不走 SSE 事件，直接以 HTTP 状态码表达（前端对 `res.ok` 校验 + `catch` 处理）。

### 1.5 认证
本期为**单用户演示场景**，默认 `DEFAULT_USER_ID = 1`（`AgentConstants`）。接口不要求传用户身份，后端统一兜底为默认用户；无 `Authorization` 校验。

### 1.6 术语说明：会话ID（sessionId）与 对话ID（conversationId）
本系统中「**会话**」与「**对话**」是两个不同的概念，对应不同的标识，**不可混用**：

| 概念 | 标识 | 说明 | 对应存储 |
|---|---|---|---|
| 会话（Session） | `sessionId` | 用户与助手之间的一段持续交互上下文；一个会话可包含多轮对话，支持列表 / 重命名 / 关闭 / 删除（见第 2 节） | MongoDB `chatSession`（**本地场景持久化、不自动删除，无 TTL 索引**，见 8.4 节） |
| 对话（Conversation） | `conversationId` | 一次具体的问答线程 / 消息流；流式对话、历史消息、停止生成均以此为准（见第 3 节） | MongoDB `chatMessage` |

> 约定：**会话管理类接口（第 2 节）一律使用 `sessionId`；聊天 / 流式 / 历史类接口（第 3 节）一律使用 `conversationId`**。两者不可互相替换。

---

## 2. 会话接口（`/api/sessions`）

> 对应前端 `src/api/session.js`。本接口管理「会话」，统一使用 `sessionId`（与第 3 节的 `conversationId` 是不同概念，见 1.6）。

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

> 对应前端 `src/api/chat.js`。用户消息字段命名为 `message`，对话标识命名为 `conversationId`（对应「对话」，见 1.6；非外部模板的 `question`）。

### 3.1 发送消息（非流式）
`POST /api/chat`

**请求体**
```json
{ "message": "帮我记录今天23点睡觉、喝了2升水", "conversationId": "f1dbf6ca0ed34eeda02ec0d0545a4429" }
```
- `message`：用户消息（必填）。
- `conversationId`：对话 ID（可选，不传则新建对话）。

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
| conversationId | query | string | 否 | 对话 ID，不传则新建 |

**响应**：SSE 流，协议见 1.4。示例流（每行一个 JSON，事件之间空行分隔）：
```
data:{"eventData":"好的，已为您记录今日作息。","eventType":1001}

data:{"eventData":"","eventType":1001}

data:{"eventType":1002}
```
（新对话的 `conversationId` 通过响应头 `X-Conversation-Id` 下发，见 1.4。）

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
6. **字段命名与标识一致性（中）**：非流式统一 `message` 与 `Result{code,message,data}`；`sessionId`（会话，第 2 节）与 `conversationId`（对话，第 3 节）是不同概念、不可混用，后端实现须与前端 `src/api/*.js` 严格对齐（前端当前 `session.js` 仍用 `conversationId` 作路径参数，须后续统一为 `sessionId`，见 1.6）。
7. **RAG 导入幂等（低）**：`/api/rag/import` 靠文档标识去重，重复调用安全；无需也不引入幂等键机制。
8. **会话持久化、不自动删除（低·已确认）**：本地场景 `chatSession` **不启用 TTL**，无 `expireAt` 字段、无 `ttl_expire_at` 索引（与之对应，`aiAnalysis` 的 DAILY 仍保留 1 天缓存 TTL）。`ChatSession` 实体与 `mongo-init.js` 已移除相关字段/索引，`sql/README.md` 索引表同步更新。

---

*文档生成时间：2026-08-07 · 形态：接口开发文档（单体 Spring Boot 4 应用内 Agent 接口模块，契约以本项目代码与前端封装为准）*
