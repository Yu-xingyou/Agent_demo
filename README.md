# habit-agent

生活习惯助手 —— 基于 **Spring Boot 3.5 + Spring AI（DashScope / 通义千问）+ Vue 3** 的前后端分离全栈 AI Agent 应用。

> 后端为 Spring Boot 3.5.3 + Java 21 的 AI Agent 服务：包含多智能体（路由 + 5 个领域子智能体）、SSE 流式对话、MongoDB 会话/记忆/分析存储以及基于 MongoDB Atlas 的 RAG 知识库。前端为 Vue 3 + Vite 工程，提供习惯打卡、目标、历史、趋势与 AI 建议界面。

## 仓库结构（单仓库 Monorepo）

```
agent_demo/
├── src/                 # 后端：Spring Boot 3.5.3 应用（/api REST + SSE 流式）
├── pom.xml              # 后端构建（Maven）
├── sql/                 # 数据库初始化脚本与说明（见 sql/README.md）
└── frontend/            # 前端：Vue 3 + Vite 工程（见 frontend/README.md）
```

前端与后端同仓管理，开发期通过 Vite 代理将 `/api` 转发到后端 8080，跨域由后端 `CorsConfig` 允许 `http://localhost:5173`。

## 快速开始

### 1. 前置依赖

| 依赖 | 说明 |
|---|---|
| JDK 21 | 后端运行环境 |
| Maven 3.8+ | 后端构建 |
| MySQL 8.0+ | 业务库 `habit_agent` |
| MongoDB 7.0+ | 本地文档库（会话/消息/记忆/分析） |
| Node.js 18+ | 前端构建 |
| DashScope API Key | 通义千问大模型（qwen-plus）与 embedding（text-embedding-v3） |

### 2. 初始化数据库

```bash
# MySQL 业务库（建表 + 种子数据）
mysql -u root -p < sql/schema.sql
```

MongoDB 本地集合由应用自动创建（Spring AI 管理）；RAG 向量库使用 MongoDB Atlas，需在 Atlas Console 手动创建向量搜索索引 `habit_vector_index`（配置见 `sql/README.md`）。

### 3. 启动后端（端口 8080）

```bash
mvn spring-boot:run
```

> 后端提供 `http://localhost:8080/api/**` 的 REST 接口与 SSE 流式对话。
> 本地默认 profile 为 `local`，配置见 `application-local.yml`（含 MySQL 连接、本地 MongoDB、DashScope Key、Atlas 向量库 URI）。
> 启动类通过 `@SpringBootApplication(exclude = MongoDBAtlasVectorStoreAutoConfiguration.class)` 允许在缺少 Atlas 环境时以降级模式运行。

### 4. 启动前端（端口 5173）

```bash
cd frontend
npm install
npm run dev
```

浏览器打开 `http://localhost:5173` 即可访问「习惯记」界面。

### 5. 生产构建前端

```bash
cd frontend
npm install
npm run build      # 产物输出到 frontend/dist/
npm run preview    # 本地预览构建产物
```

## 技术栈

- 后端：Spring Boot 3.5.3 + Java 21
  - Spring AI 1.0.0 + spring-ai-alibaba 1.0.0.2（DashScope：qwen-plus / text-embedding-v3，1024 维）
  - Spring Data JPA / MySQL、Spring Data MongoDB（本地库 + Atlas 向量库）
  - WebFlux（SSE 流式响应）、springdoc-openapi 2.8.9、Validation、Lombok
- 前端：Vue 3 + Vite 5 + Pinia 2 + Vue Router 4 + Axios 1 + Element Plus 2 + ECharts 5 + Tailwind CSS 3.4.17

## AI 架构与能力

- **多智能体**：路由 Agent（`route-agent`）根据用户输入分发到 5 个领域子智能体——`sleep`（睡眠）、`diet`（饮食）、`exercise`（运动）、`checkin`（打卡）、`knowledge`（知识）。
- **系统提示词**：`application.yml` 中配置 7 份提示词（1 个 chat 入口 + 1 个路由 + 5 个子智能体）。
- **会话与记忆**：基于 MongoDB 的 `chatSession` / `chatMessage` / `chatMemory` 集合实现多轮对话与记忆。
- **RAG 知识库**：基于 MongoDB Atlas Vector Store，`habit_knowledge` 集合存储 1024 维向量，向量搜索索引 `habit_vector_index`（cosine）。
- **流式输出**：后端通过 SSE 将 Assistant 回复实时推送到前端 `/ai-chat` 等页面。

## 安全提示

⚠️ **`src/main/resources/application-local.yml` 包含真实密钥与凭据**（DashScope API Key、MySQL 密码、MongoDB Atlas 连接串），该文件已被仓库根目录 `.gitignore` 忽略（`application-local.yml`、`.env`、`*.env`），**请勿提交到版本库**。

本地配置方式：

1. 复制 `application-local.yml.example`（若不存在，直接新建 `application-local.yml`）并填入本地环境的真实值。
2. 该文件仅存在于本地，不会被 Git 跟踪；团队成员各自维护自己的本地配置。
3. 生产部署请将密钥通过环境变量或密钥管理服务注入，避免硬编码。

## 相关文档

- 前端工程说明：`frontend/README.md`
- 数据库初始化与存储架构：`sql/README.md`
