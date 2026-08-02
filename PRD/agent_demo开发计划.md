# 生活习惯助手 Agent — 开发计划（MongoDB + MySQL 版）

> 面向黑马天机学堂 AI 智能体课程学习者的全栈 Spring AI 实践项目，覆盖课程 14 项技术点。
> 数据存储架构：MySQL（关系型业务数据）+ MongoDB（文档型 AI 数据 + 向量存储）。

---

## 一、技术选型详细说明

### 1.1 版本对应关系（关键决策）

Spring AI 2.0 GA 于 2026 年 6 月 12 日正式发布，基于 Spring Boot 4.1 和 Spring Framework 7.0 构建。Spring AI Alibaba（SAA）当前推荐版本 1.1.2.0 仍基于 Spring AI 1.1.2 + Spring Boot 3.5.x，**尚未兼容 Spring AI 2.0**。

| 组件 | 推荐版本 | 说明 |
|---|---|---|
| Spring AI | **2.0.0 GA** | 2026-06-12 发布；Tool Calling 一等公民、Advisor Chain 重构、增强 RAG 与记忆管理 |
| Spring Boot | **4.1.x** | Spring AI 2.0 对应；Spring Framework 7.0、Jackson 3 |
| JDK | **21** (LTS) | Spring Boot 4 最低 17，21 为当前 LTS 推荐 |
| 通义千问接入方式 | **OpenAI 兼容模式** | DashScope 提供 OpenAI 兼容端点，用 Spring AI 2.0 原生 OpenAI starter 接入 |
| Embedding | DashScope text-embedding-v3 | 通过 OpenAI 兼容端点调用；RAG 向量化 |
| 多智能体实现 | **手动路由（ChatClient + ToolCallingAdvisor）** | SAA 尚未兼容 Spring AI 2.0，多智能体用手动路由实现 |
| **文档数据库** | **MongoDB 7.0+** | 对话记忆、AI 分析结果、会话管理（文档型数据） |
| **向量库** | **MongoDB Atlas Vector Search** | Spring AI 原生支持 MongoDB Atlas 向量存储；RAG 知识库 |

**版本选择理由与权衡**：

- 用户明确要求 JDK 21 + Spring AI 最新版 + Spring Boot 4.0+，因此采用 **Spring AI 2.0 GA + Spring Boot 4.1 + JDK 21** 组合。
- **数据存储架构决策**：用户要求采用 MongoDB + MySQL 双库方案。MySQL 存储关系型业务数据（用户、习惯记录、目标），MongoDB 存储文档型 AI 数据（对话记忆、AI 分析结果、向量检索）。
- **通义千问接入方案**：由于 SAA 未兼容 Spring AI 2.0，改用 DashScope 的 OpenAI 兼容端点（`https://dashscope.aliyuncs.com/compatible-mode/v1`），通过 Spring AI 2.0 原生 `spring-ai-starter-model-openai` 接入。
- **多智能体方案**：SAA 的 Graph 引擎和 `ReactAgent`/`Supervisor` 暂不可用，多智能体路由用 Spring AI 2.0 的 `ChatClient` + 自定义路由逻辑手动实现。
- **降级方案**：若 Spring AI 2.0 + Spring Boot 4.1 遇到兼容性问题，可回退到 SAA 1.1.2.0 + Spring AI 1.1.2 + Spring Boot 3.5.x + JDK 17 方案。

### 1.2 技术选型清单与理由

| 层次 | 技术 | 版本 | 选择理由 |
|---|---|---|---|
| **构建工具** | Maven | 3.9+ | Spring 官方推荐，Spring AI BOM 依赖管理成熟 |
| **语言/运行时** | JDK | 21 (LTS) | Spring Boot 4 最低 17，21 为当前 LTS 推荐 |
| **框架** | Spring Boot | 4.1.x | Spring AI 2.0 对应；Spring Framework 7.0、Jackson 3 |
| **Web** | Spring Web (MVC) | 随 Boot | 后端仅提供 `/api` REST 接口；SSE 用 SseEmitter 实现 |
| **AI 框架** | Spring AI | 2.0.0 GA | ChatClient/Advisor/Memory/Tool/RAG 核心；Tool Calling 一等公民 |
| **AI 模型接入** | spring-ai-starter-model-openai | 随 Spring AI 2.0 | 通过 OpenAI 兼容模式接入通义千问 DashScope |
| **AI 模型** | 通义千问（DashScope 兼容模式） | qwen-plus / qwen-turbo | 课程指定；DashScope 提供 OpenAI 兼容端点 |
| **Embedding** | DashScope text-embedding-v3 | 通过 OpenAI 兼容端点 | RAG 向量化；与主模型同一平台 |
| **ORM (MySQL)** | Spring Data JPA | 随 Boot | 关系型业务数据访问；JDK 21 下配合 Hibernate 7 |
| **业务库** | MySQL | 8.0+ | 存储用户、习惯记录、目标等结构化关系数据 |
| **文档库** | MongoDB | 7.0+ | 对话记忆 ChatMemory + AI 分析结果 + 会话管理（文档型数据天然适配） |
| **MongoDB 访问** | Spring Data MongoDB | 随 Boot | MongoRepository + MongoTemplate；与 JPA 并存 |
| **向量库** | MongoDB Atlas Vector Search | spring-ai-starter-vector-store-mongodb-atlas | Spring AI 原生支持；RAG 知识库向量存储与检索 |
| **前端框架** | Vue 3 | 3.x | **独立前端仓库 `habit-agent-web`**，前端工程，与后端分离部署 |
| **前端构建** | Vite | 5.x | 开发服务器（默认 5173）+ `/api` 代理到后端 8080 |
| **前端状态** | Pinia | 2.x | 全局状态管理（今日记录、会话等） |
| **前端请求** | Axios | 1.x | 封装统一请求，解析后端 `Result` 响应 |
| **前端路由** | Vue Router | 4.x | SPA 路由（首页/打卡/历史/趋势/AI建议） |
| **前端 UI** | Element Plus | 2.x | 响应式组件库，替代 Bootstrap |
| **图表** | ECharts | 5.5 | 趋势分析页（前端 Vue 组件）数据可视化（折线图/柱状图/雷达图） |
| **流式输出** | SseEmitter (MVC) | 随 Boot | 后端 SSE 端点；前端 Vue 用 EventSource 接收 |

### 1.3 数据存储分工（MySQL vs MongoDB）

| 数据类型 | 存储位置 | 理由 |
|---|---|---|
| 用户信息 (User) | **MySQL** | 关系型，结构固定，适合 RDBMS |
| 习惯记录 (HabitRecord) | **MySQL** | 每日打卡数据，结构化，需要日期范围查询和统计 |
| 习惯目标 (HabitGoal) | **MySQL** | 目标配置，结构固定 |
| 对话消息 (ChatMessage) | **MongoDB** | 文档型数据，字段灵活，适合存储 AI 对话内容 |
| 会话管理 (ChatSession) | **MongoDB** | 会话元数据 + 状态，文档模型天然适配 |
| AI 分析结果 (AiAnalysis) | **MongoDB** | AI 生成内容为非结构化文本，含 Markdown，文档存储更灵活 |
| RAG 知识库向量 | **MongoDB Atlas** | 向量检索，Spring AI 原生支持 MongoDB Atlas Vector Store |
| 分析缓存 | **MongoDB** | TTL 索引实现自动过期，替代 Redis 缓存 |

### 1.4 pom.xml 关键依赖（供实施参考）

```xml
<properties>
    <java.version>21</java.version>
    <spring-ai.version>2.0.0</spring-ai.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- Web（仅提供 /api REST 接口，无页面渲染） -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- JPA + MySQL (关系型业务数据) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- MongoDB (文档型 AI 数据 + 对话记忆) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>

    <!-- Spring AI OpenAI Starter (通过兼容模式接入通义千问 DashScope) -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-openai</artifactId>
    </dependency>

    <!-- MongoDB Atlas 向量存储 (RAG 知识库) -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-vector-store-mongodb-atlas</artifactId>
    </dependency>
</dependencies>
```

> **注意**：不使用 `spring-ai-alibaba-starter-dashscope`，因为 SAA 尚未兼容 Spring AI 2.0。通义千问通过 DashScope 的 OpenAI 兼容端点接入。不使用 Redis，对话记忆和向量存储全部用 MongoDB。

### 1.5 application.yml 关键配置（供实施参考）

```yaml
spring:
  application:
    name: habit-agent

  # MySQL 数据源 (关系型业务数据)
  datasource:
    url: jdbc:mysql://localhost:3306/habit_agent?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: ${MYSQL_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver

  # JPA 配置
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

  # MongoDB 配置 (文档型 AI 数据 + 向量存储)
  data:
    mongodb:
      uri: mongodb://localhost:27017/habit_agent
      auto-index-creation: true

  # Spring AI 配置 - 通过 OpenAI 兼容模式接入通义千问 DashScope
  ai:
    openai:
      base-url: https://dashscope.aliyuncs.com/compatible-mode
      api-key: ${DASHSCOPE_API_KEY:sk-placeholder}
      chat:
        options:
          model: qwen-plus
          temperature: 0.7
      embedding:
        options:
          model: text-embedding-v3
    # MongoDB Atlas 向量存储配置 (RAG 知识库)
    vectorstore:
      mongodb:
        collection-name: habit_knowledge
        index-name: habit_vector_index
        path: embedding
        metadata-fields-to-filter: [ "doc_type", "source" ]
```

---

## 二、系统架构设计

### 2.1 模块划分

```
habit-agent/
├── src/main/java/com/habit/agent/
│   ├── HabitAgentApplication.java          # 启动类
│   │
│   ├── config/                             # 配置层
│   │   ├── ChatClientConfig.java           # ChatClient Bean 配置
│   │   ├── MongoConfig.java                # MongoDB 配置（序列化、TTL 索引等）
│   │   ├── VectorStoreConfig.java          # 向量库、EmbeddingModel 配置
│   │   └── AgentConfig.java                # 多智能体路由配置
│   │
│   ├── controller/                         # 控制层（8 个 REST Controller，46 个 /api 端点）
│   │   ├── HabitController.java            # 习惯记录 CRUD（REST，7 端点）
│   │   ├── GoalController.java             # 习惯目标管理（REST，6 端点）
│   │   ├── ChatController.java             # AI 对话（非流式 + SSE 流式 + 停止，3 端点）
│   │   ├── SessionController.java          # 会话管理（REST，7 端点）
│   │   ├── AnalysisController.java         # 趋势分析数据（REST，4 端点）
│   │   ├── AiAnalysisController.java       # AI 分析结果 + 每日评价（REST，6 端点）
│   │   ├── RagController.java              # RAG 知识库管理（REST，5 端点）
│   │   └── ReminderController.java         # 打卡提醒管理（REST，5 端点）
│   │
│   ├── service/                            # 业务层
│   │   ├── HabitService.java               # 习惯记录业务（MySQL）
│   │   ├── GoalService.java                # 目标管理（MySQL）
│   │   ├── AnalysisService.java            # 统计分析
│   │   ├── ChatService.java                # AI 对话编排
│   │   ├── RagService.java                 # RAG 知识库管理
│   │   ├── SessionService.java             # 会话管理业务（MongoDB）
│   │   ├── AiAnalysisService.java          # AI 分析结果业务（MongoDB）
│   │   └── ReminderService.java            # 打卡提醒业务（MySQL）
│   │
│   ├── agent/                              # AI Agent 层
│   │   ├── tools/                          # Tool Calling
│   │   │   ├── HabitQueryTools.java        # 查询历史记录工具
│   │   │   ├── HabitStatTools.java         # 统计数据工具
│   │   │   └── SuggestionTools.java        # 保存建议工具
│   │   ├── advisors/                       # 自定义 Advisor
│   │   │   ├── LoggingAdvisor.java         # 日志记录
│   │   │   ├── SafetyFilterAdvisor.java    # 安全过滤
│   │   │   └── ContextInjectionAdvisor.java# 业务上下文注入
│   │   ├── memory/                         # 对话记忆（MongoDB 实现）
│   │   │   └── MongoChatMemoryRepository.java # MongoDB 实现 ChatMemoryRepository
│   │   └── router/                         # 多智能体路由
│   │       ├── IntentRouter.java           # 意图路由
│   │       ├── DataAnalysisAgent.java      # 数据分析子 Agent
│   │       ├── SuggestionAgent.java        # 建议生成子 Agent
│   │       └── ChatAgent.java              # 日常对话子 Agent
│   │
│   ├── repository/                         # 数据访问层
│   │   ├── jpa/                            # JPA Repository (MySQL)
│   │   │   ├── UserRepository.java
│   │   │   ├── HabitRecordRepository.java
│   │   │   ├── HabitGoalRepository.java
│   │   │   └── ReminderRepository.java     # 打卡提醒 Repository
│   │   └── mongo/                          # MongoDB Repository
│   │       ├── ChatMessageRepository.java
│   │       ├── ChatSessionRepository.java
│   │       └── AiAnalysisRepository.java
│   │
│   ├── entity/                             # 实体层
│   │   ├── jpa/                            # JPA Entity (MySQL)
│   │   │   ├── User.java
│   │   │   ├── HabitRecord.java
│   │   │   ├── HabitGoal.java
│   │   │   └── Reminder.java               # 打卡提醒实体
│   │   └── mongo/                          # MongoDB Document
│   │       ├── ChatMessageDoc.java
│   │       ├── ChatSessionDoc.java
│   │       └── AiAnalysisDoc.java          # 含 score 字段（每日评价用）
│   │
│   └── common/                             # 公共组件
│       ├── exception/
│       │   ├── GlobalExceptionHandler.java # 19 个错误码统一映射
│       │   └── AiCallException.java
│       ├── result/
│       │   └── Result.java
│       ├── constant/
│       │   └── AgentConstants.java
│       └── vo/                             # 视图对象层（22 个 VO 类）
│           ├── HabitRecordVO.java
│           ├── HabitGoalVO.java
│           ├── ChatRequestVO.java
│           ├── ChatResponseVO.java
│           ├── ChatSessionVO.java
│           ├── ChatMessageVO.java
│           ├── TrendDataVO.java
│           ├── AchievementRateVO.java
│           ├── AiAnalysisVO.java
│           ├── DailyEvaluationVO.java
│           ├── ReminderVO.java
│           └── ...                         # 其余 VO 类
│
├── src/main/resources/
│   ├── application.yml
│   ├── static/                             # 静态资源（可选：favicon 等，无页面模板）
│   ├── prompts/                            # Prompt 模板
│   │   ├── system-prompt.st
│   │   ├── analysis-prompt.st
│   │   └── suggestion-prompt.st
│   └── rag-docs/                           # RAG 知识库源文档
│       ├── sleep-guide.md
│       ├── exercise-guide.md
│       └── diet-guide.md
│
├── sql/
│   └── schema.sql                          # MySQL 建表脚本
└── pom.xml
```

### 2.2 层次结构与请求流转

```
┌─────────────────────────────────────────────────────────┐
│  前端独立仓库 habit-agent-web (Vue3+Vite 独立部署)        │
│  Vue 组件渲染 + Axios 调 /api + EventSource 收 SSE       │
└───────────────┬──────────────────────────┬──────────────┘
                │ HTTPS /api (JSON)       │ SSE (流式)
┌───────────────▼──────────────────────────▼──────────────┐
│              后端 Controller 层 (纯 REST/SSE)             │
└───────────────┬──────────────────────────┬──────────────┘
                │                          │
┌───────────────▼──────────┐   ┌───────────▼──────────────┐
│      Service 层 (业务)    │   │     Agent 层 (AI 编排)    │
│  HabitService (MySQL)    │   │  ChatService → ChatClient │
│  AnalysisService         │   │  RagService → VectorStore │
└───────┬───────┬──────────┘   └───────────┬──────────────┘
        │       │                          │
┌───────▼──┐ ┌──▼───────────┐  ┌───────────▼──────────────┐
│ JPA Repo │ │ Mongo Repo   │  │  Advisor 链               │
│ (MySQL)  │ │ (MongoDB)    │  │  LoggingAdvisor           │
└───────┬──┘ └──┬───────────┘  │  SafetyFilterAdvisor      │
        │       │              │  RetrievalAugmentationAdvisor│
┌───────▼──┐ ┌──▼───────────┐  │  MessageChatMemoryAdvisor  │
│  MySQL   │ │   MongoDB    │  │  ↓                        │
│          │ │  + Atlas     │  │  Tool Calling (@Tool)     │
│          │ │  Vector      │  │  ↓                        │
│          │ │  Search      │  │  多智能体路由 (IntentRouter)│
└──────────┘ └──────────────┘  └────────────┬──────────────┘
                                ┌────────────▼──────────────┐
                                │  通义千问 (DashScope API)  │
                                └───────────────────────────┘
```

### 2.3 前端独立仓库结构（habit-agent-web）

前端为**独立 git 仓库**，与后端 `habit-agent` 分离部署，通过 HTTPS/JSON 调用 `/api`。

```
habit-agent-web/
├── package.json / vite.config.js          # Vue3 + Vite，/api 代理到后端 8080
├── index.html
└── src/
    ├── main.js / App.vue
    ├── router/index.js                    # 5 路由：首页/打卡/历史/趋势/AI建议
    ├── stores/                            # Pinia 状态（今日记录/会话等）
    ├── utils/request.js                   # Axios 封装，统一解析 Result
    ├── api/                               # 接口封装（habit/chat/analysis/rag...）
    ├── layouts/MainLayout.vue             # 公共布局（导航栏）
    └── views/                             # HomeView/CheckinView/HistoryView/TrendView/AiChatView
```

> **前后端分离要点**：后端只产出 JSON API，不再渲染任何页面；前端负责全部 UI 渲染与路由。开发联调用 Vite 代理规避跨域，生产部署前端可独立托管（Nginx/静态托管）并配置 CORS 或反向代理。

---

## 三、数据库设计

### 3.1 MySQL 业务表（关系型数据）

```sql
CREATE DATABASE IF NOT EXISTS `habit_agent` DEFAULT CHARACTER SET utf8mb4;
USE `habit_agent`;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT PRIMARY KEY AUTO_INCREMENT,
    `username`    VARCHAR(50)  NOT NULL UNIQUE,
    `nickname`    VARCHAR(50),
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 习惯记录表（核心业务表，每日打卡数据）
CREATE TABLE IF NOT EXISTS `habit_record` (
    `id`                BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id`           BIGINT       NOT NULL,
    `record_date`       DATE         NOT NULL,
    `sleep_time`        TIME,
    `wake_time`         TIME,
    `sleep_duration`    DECIMAL(4,2),
    `sleep_quality`     TINYINT,
    `diet_desc`         VARCHAR(500),
    `diet_score`        TINYINT,
    `exercise_type`     VARCHAR(100),
    `exercise_duration` INT,
    `water_intake`      INT,
    `mood`              TINYINT,
    `remark`            VARCHAR(500),
    `create_time`       DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_date` (`user_id`, `record_date`),
    INDEX `idx_user_date` (`user_id`, `record_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 习惯目标表
CREATE TABLE IF NOT EXISTS `habit_goal` (
    `id`            BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id`       BIGINT      NOT NULL,
    `goal_type`     VARCHAR(30) NOT NULL,
    `target_value`  DECIMAL(8,2) NOT NULL,
    `unit`          VARCHAR(20),
    `period`        VARCHAR(20) DEFAULT 'DAILY',
    `is_active`     TINYINT DEFAULT 1,
    `create_time`   DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 打卡提醒表（对应 PRD 可选功能「打卡提醒」）
CREATE TABLE IF NOT EXISTS `reminder` (
    `id`             BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id`        BIGINT       NOT NULL,
    `title`          VARCHAR(100) NOT NULL,
    `reminder_time`  TIME         NOT NULL,
    `reminder_type`  VARCHAR(30)  NOT NULL COMMENT 'SLEEP/DIET/EXERCISE/WATER/CUSTOM',
    `weekdays`       VARCHAR(20)  DEFAULT '1,2,3,4,5,6,7' COMMENT '星期几触发，逗号分隔',
    `is_active`      TINYINT      DEFAULT 1,
    `create_time`    DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_user_active` (`user_id`, `is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 3.2 MongoDB 文档集合（文档型 AI 数据）

#### ChatMessage 集合（对话消息）

```json
{
  "_id": ObjectId("..."),
  "conversationId": "uuid-string",
  "userId": 1,
  "role": "USER",
  "content": "分析一下我最近一周的睡眠",
  "metadata": {
    "tokensUsed": 150,
    "model": "qwen-plus"
  },
  "createTime": ISODate("2026-07-29T10:00:00Z")
}
```

#### ChatSession 集合（会话管理）

```json
{
  "_id": ObjectId("..."),
  "conversationId": "uuid-string",
  "userId": 1,
  "title": "睡眠分析对话",
  "status": "ACTIVE",
  "messageCount": 12,
  "lastMessageTime": ISODate("2026-07-29T10:30:00Z"),
  "createTime": ISODate("2026-07-29T10:00:00Z"),
  "expireAt": ISODate("2026-08-05T10:00:00Z")  // TTL 索引，7天后自动过期
}
```

#### AiAnalysis 集合（AI 分析结果 + 每日评价）

```json
{
  "_id": ObjectId("..."),
  "userId": 1,
  "analysisType": "WEEKLY",
  "periodStart": ISODate("2026-07-22T00:00:00Z"),
  "periodEnd": ISODate("2026-07-29T00:00:00Z"),
  "content": "## 本周习惯分析\n\n睡眠平均时长7.2小时...",
  "score": 85,
  "suggestion": "建议每天提前30分钟入睡...",
  "riskWarning": "运动量连续3天不足...",
  "metadata": {
    "tokensUsed": 800,
    "model": "qwen-plus"
  },
  "createTime": ISODate("2026-07-29T11:00:00Z"),
  "expireAt": ISODate("2026-07-30T11:00:00Z")  // TTL，1天过期
}
```

> `analysisType` 支持 `WEEKLY`/`MONTHLY`/`CUSTOM`/`DAILY` 四种类型。`DAILY` 类型使用 `recordDate` 字段记录评价日期，`score` 字段记录当日评分（0-100）。其余类型使用 `periodStart`/`periodEnd`。

#### habit_knowledge 集合（RAG 向量存储）

```json
{
  "_id": ObjectId("..."),
  "content": "成人每天建议睡眠7-9小时...",
  "metadata": {
    "doc_type": "sleep_guide",
    "source": "sleep-guide.md",
    "chunk_index": 0
  },
  "embedding": [0.012, -0.034, 0.078, ...]  // 向量数组
}
```

### 3.3 MongoDB 索引设计

| 集合 | 索引 | 类型 | 说明 |
|---|---|---|---|
| chatMessage | `{conversationId: 1, createTime: 1}` | 复合索引 | 按会话查询消息 |
| chatMessage | `{userId: 1, createTime: -1}` | 复合索引 | 按用户查询消息历史 |
| chatSession | `{conversationId: 1}` | 唯一索引 | 会话ID唯一 |
| chatSession | `{expireAt: 1}` | TTL 索引 | 7天自动过期 |
| aiAnalysis | `{userId: 1, analysisType: 1, createTime: -1}` | 复合索引 | 按用户和类型查询 |
| aiAnalysis | `{expireAt: 1}` | TTL 索引 | 缓存1天自动过期 |
| habit_knowledge | 向量索引 (Atlas) | Vector Search | RAG 语义检索 |

### 3.4 RAG 知识库设计（MongoDB Atlas Vector Search）

- **文档来源**：`src/main/resources/rag-docs/` 下的 Markdown 文档
- **处理流程**：`DocumentReader` 读取 Markdown → `TokenTextSplitter` 分块 → DashScope Embedding 向量化 → MongoDB Atlas VectorStore 存储
- **检索方式**：`RetrievalAugmentationAdvisor` 在对话时自动检索 Top-K 相关文档片段注入 Prompt
- **向量索引**：在 MongoDB Atlas 中创建 `habit_vector_index`，使用 `embedding` 字段进行向量检索

---

## 四、AI Agent 架构设计

### 4.1 课程 14 项技术点映射表

| # | 课程技术点 | 项目落地位置 | 实现方式 |
|---|---|---|---|
| 1 | AI 大模型核心原理 | 全项目理解基础 | 通过通义千问实践 Token/温度/上下文窗口 |
| 2 | OpenAI/百炼 API 对接 | `application.yml` + OpenAI 兼容模式 | spring-ai-starter-model-openai + DashScope |
| 3 | Tool Calling | `agent/tools/` | `@Tool` 注解定义业务工具 |
| 4 | Prompt Engineering | `resources/prompts/` | System/Analysis/Suggestion 提示词模板 |
| 5 | 流式对话 | `ChatController` | `stream()` SSE 流式输出 + `POST /api/chat` 非流式对话（企业标准双模式） |
| 6 | System 角色设定 | `ChatClientConfig` | `defaultSystem()` 设定助手人格 |
| 7 | Advisors 自定义增强 | `agent/advisors/` | 自定义 Logging/Safety/Context Advisor |
| 8 | Tool Calling 与业务整合 | `agent/tools/` | 工具调用 JPA/Mongo Repository 查询真实数据 |
| 9 | RAG + 向量检索 | `RagService` + MongoDB Atlas VectorStore | RetrievalAugmentationAdvisor |
| 10 | Spring AI 集成项目 | 整体 | AI 能力贯穿所有前端页面（Vue 组件） |
| 11 | 流式输出与停止生成 | `ChatController` + `chat-stream.js` | SseEmitter + Flux + 前端停止按钮 |
| 12 | 对话记忆管理 | `agent/memory/MongoChatMemoryRepository` | MongoDB 实现 ChatMemoryRepository |
| 13 | 路由工作流+多智能体 | `agent/router/` | ChatClient + 手动路由 |
| 14 | 百炼平台智能体 | 文档说明 | 百炼平台配置说明（可选演示） |

### 4.2 ChatClient 配置

```java
@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                  ChatMemory chatMemory,
                                  VectorStore vectorStore,
                                  HabitQueryTools habitQueryTools,
                                  HabitStatTools habitStatTools,
                                  SuggestionTools suggestionTools) {
        return builder
            .defaultSystem("classpath:prompts/system-prompt.st")
            .defaultTools(habitQueryTools, habitStatTools, suggestionTools)
            .defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory).build(),
                RetrievalAugmentationAdvisor.builder()
                    .queryTransformers(RewriteQueryTransformer.builder().build())
                    .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .topK(3)
                        .build())
                    .build(),
                new LoggingAdvisor(),
                new SafetyFilterAdvisor()
            )
            .build();
    }
}
```

### 4.3 ChatMemory 实现（MongoDB，技术点 12）

Spring AI 的 `ChatMemoryRepository` 接口需自定义 MongoDB 实现：

```java
@Component
public class MongoChatMemoryRepository implements ChatMemoryRepository {

    private final MongoTemplate mongoTemplate;
    private static final String COLLECTION = "chatMemory";

    @Override
    public List<String> findConversationIds() {
        return mongoTemplate.findDistinct(
            new Query(), "conversationId", COLLECTION, String.class);
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        Query query = Query.query(Criteria.where("conversationId").is(conversationId));
        query.with(Sort.by(Sort.Direction.ASC, "createTime"));
        List<ChatMessageDoc> docs = mongoTemplate.find(query, ChatMessageDoc.class, COLLECTION);
        return docs.stream().map(this::toMessage).toList();
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        // 清除旧消息，保存新消息（全量替换策略）
        mongoTemplate.remove(
            Query.query(Criteria.where("conversationId").is(conversationId)),
            COLLECTION);
        List<ChatMessageDoc> docs = messages.stream()
            .map(m -> toDoc(m, conversationId))
            .toList();
        mongoTemplate.insertAll(docs);
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        mongoTemplate.remove(
            Query.query(Criteria.where("conversationId").is(conversationId)),
            COLLECTION);
    }
}

// ChatMemory Bean 配置（滑动窗口，保留最近20条）
@Bean
public ChatMemory chatMemory(ChatMemoryRepository repository) {
    return MessageWindowChatMemory.builder()
        .chatMemoryRepository(repository)
        .maxMessages(20)
        .build();
}
```

### 4.4 Tool Calling 设计（技术点 3/8）

工具类让 AI 能查询真实业务数据并执行操作（通过 JPA 查 MySQL，通过 MongoRepository 查 MongoDB）：

```java
@Component
public class HabitQueryTools {

    private final HabitRecordRepository habitRecordRepository; // JPA -> MySQL

    @Tool(description = "查询用户指定日期范围内的历史习惯记录")
    public List<HabitRecord> getHabitRecords(
        @ToolParam(description = "用户ID") Long userId,
        @ToolParam(description = "开始日期，格式 yyyy-MM-dd") String startDate,
        @ToolParam(description = "结束日期，格式 yyyy-MM-dd") String endDate
    ) {
        return habitRecordRepository.findByUserIdAndRecordDateBetweenOrderByRecordDateDesc(
            userId, LocalDate.parse(startDate), LocalDate.parse(endDate));
    }

    @Tool(description = "查询用户最近N天的习惯记录")
    public List<HabitRecord> getRecentHabits(
        @ToolParam(description = "用户ID") Long userId,
        @ToolParam(description = "最近天数") int days
    ) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days);
        return habitRecordRepository.findByUserIdAndRecordDateBetweenOrderByRecordDateDesc(
            userId, start, end);
    }
}
```

### 4.5 RAG 知识库设计（技术点 9，MongoDB Atlas Vector Store）

```java
@Service
public class RagService {

    private final VectorStore vectorStore; // MongoDB Atlas VectorStore

    public void importKnowledgeDocs() {
        List<Document> docs = new MarkdownDocumentReader(
            "classpath:rag-docs/sleep-guide.md").get();
        TextSplitter splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.split(docs);
        vectorStore.add(chunks); // 向量化后存入 MongoDB Atlas
    }

    public List<Document> searchKnowledge(String query) {
        return vectorStore.similaritySearch(
            SearchRequest.builder().query(query).topK(3).build());
    }
}
```

### 4.6 多智能体路由设计（技术点 13）

采用 **Spring AI 2.0 原生 ChatClient + 手动路由** 方式实现：

```
用户输入 → 意图路由器(IntentRouter)
              ├── DATA_ANALYSIS → DataAnalysisAgent (调用工具查数据)
              ├── SUGGESTION    → SuggestionAgent (RAG+知识库生成建议)
              └── CHAT          → ChatAgent (记忆+日常对话)
```

### 4.7 对话接口设计：非流式 + 流式双模式（技术点 5/11）

对标企业标准（OpenAI `stream=true/false`、Anthropic `stream` 参数、DashScope `stream` + `incremental_output`），ChatController 同时提供两种对话模式：

**非流式对话 `POST /api/chat`**（用于后端任务调用、测试调试）：
```java
@PostMapping
public Result<ChatResponseVO> chat(@RequestBody ChatRequestVO request) {
    String conversationId = request.getConversationId() != null
        ? request.getConversationId()
        : chatService.getOrCreateSession(AgentConstants.DEFAULT_USER_ID);
    String content = chatClient.prompt()
        .user(request.getMessage())
        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
        .call()
        .content();
    return Result.success(chatService.buildResponse(conversationId, content));
}
```

**SSE 流式对话 `GET /api/chat/stream`**（用于前端交互，逐字打字机效果）：
```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter streamChat(@RequestParam String message,
                              @RequestParam String conversationId) {
    SseEmitter emitter = new SseEmitter(120_000L);
    chatClient.prompt()
        .user(message)
        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
        .stream()
        .chatResponse()
        .subscribe(
            response -> {
                String content = response.getResult().getOutput().getText();
                if (content != null) {
                    emitter.send(SseEmitter.event().data(content));
                }
            },
            emitter::completeWithError,
            emitter::complete
        );
    return emitter;
}
```

---

## 五、开发流程（按阶段划分）

> **重要**：严格按阶段推进，每阶段完成后需用户审核批准才能进入下一阶段。每阶段完成后自动 Git commit + push。

### 阶段一：环境搭建与项目骨架

**目标**：可运行的空项目 + 数据库就绪 + Git 仓库初始化

- 初始化 Git 仓库，关联远程 `https://github.com/Yu-xingyou/Agent_demo`
- 用 Spring Initializr 生成项目（JDK 21 + Spring Boot 4.1.x + Web + JPA + MySQL + MongoDB），**不引入 Thymeleaf**（前端为独立 Vue 工程）
- 配置 pom.xml 添加 Spring AI 2.0 BOM 和依赖
- 申请阿里云百炼 DashScope API Key，配置 application.yml
- 创建 MySQL 数据库 `habit_agent`，执行建表脚本
- 启动 MongoDB（本地或 Docker）
- 配置 MongoDB Atlas Vector Search 索引（或本地 MongoDB + 模拟向量检索）
- 验证：项目能正常启动，连接 MySQL/MongoDB 成功
- **Git commit + push**

### 阶段二：业务数据层与基础 CRUD（MySQL）

**目标**：习惯记录可录入、可查询，习惯目标可管理

> 对应 API 文档：HabitController（7 端点）+ GoalController（6 端点），共 13 端点。

- 创建 JPA Entity（User/HabitRecord/HabitGoal）
- 创建 JPA Repository 接口
- 创建 `HabitService` + `HabitController`（习惯记录 7 端点）
- 创建 `GoalService` + `GoalController`（习惯目标 6 端点）
- 创建 VO 类（HabitRecordVO/HabitGoalVO 等）
- 编写测试数据初始化（DataInitializer）
- 验证：通过 API 测试录入和查询习惯记录、创建和管理目标
- **Git commit + push**

### 阶段三：前端独立仓库（Vue 3 + Vite）

**目标**：5 个核心页面可用（前端独立仓库 `habit-agent-web`）

> 对应 API 文档：后端 `/api` REST 接口（前端经 Axios 调用，无 PageController）。

- 初始化 Vue3 + Vite 工程，配置 `/api` 代理到后端 8080
- 搭建 `MainLayout.vue`（Element Plus 导航栏）+ Vue Router 5 路由
- 首页 `HomeView.vue`：欢迎信息 + 快捷统计卡片（调 `/api/habits/today`）
- 每日打卡页 `CheckinView.vue`：表单录入数据（调 `POST /api/habits`）
- 历史记录页 `HistoryView.vue`：表格展示历史记录（调 `/api/habits`）
- 趋势分析页 `TrendView.vue`：预留 ECharts 容器
- AI 建议页 `AiChatView.vue`：对话界面 + 流式输出区域（EventSource 调 `/api/chat/stream`）
- 验证：各前端路由页面能正常渲染，打卡表单能提交保存
- **Git commit + push（前端仓库）**

### 阶段四：Spring AI 基础对接（技术点 1/2/4/6/10）

**目标**：ChatClient 配置就绪，通义千问连通，System Prompt 生效

> 本阶段不产出对外端点（0 端点）。ChatController 的非流式 `POST /api/chat` 和 SSE 流式 `GET /api/chat/stream` 端点在阶段五实现。本阶段仅完成 ChatClient Bean 配置和模型连通验证。

- 配置 DashScope API Key，验证模型连通
- 编写 System Prompt 模板（`system-prompt.st`）
- 创建 `ChatClientConfig`，配置 `defaultSystem`
- 创建 `ChatService` 基础结构（对话方法骨架，记忆/流式在阶段五实现）
- 验证：通过单元测试或临时 main 方法验证 `chatClient.prompt().user("你好").call().content()` 能返回通义千问回复
- **Git commit + push**

### 阶段五：对话记忆与流式输出（技术点 5/11/12）

**目标**：多轮对话有记忆，SSE 流式打字效果，会话管理完整可用

> 对应 API 文档：ChatController（3 端点：非流式对话 + SSE 流式 + 停止）+ SessionController（7 端点：会话 CRUD + 消息历史），共 10 端点。

- 创建 MongoDB Document（ChatMessageDoc/ChatSessionDoc/AiAnalysisDoc）
- 创建 MongoDB Repository
- 实现 `MongoChatMemoryRepository`（自定义 MongoDB 存储）
- 配置 `MessageWindowChatMemory`（滑动窗口 20 条）
- 配置 `MessageChatMemoryAdvisor` 加入 Advisor 链
- 实现会话管理：`SessionController`（会话列表/创建/查询/删除/消息历史/关闭）+ `SessionService`
- 实现非流式对话：`ChatController` 的 `POST /api/chat`（企业标准，用于后端任务调用和测试调试）
- 实现 SSE 流式输出：`ChatController` 的 `GET /api/chat/stream`
- 实现停止生成：`POST /api/chat/stop`
- 实现前端 `chat-stream.js`（EventSource + 停止按钮）
- 验证：多轮对话有记忆；流式输出逐字显示；停止按钮能中断；会话列表正确展示
- **Git commit + push**

### 阶段六：Tool Calling 与业务整合（技术点 3/7/8）

**目标**：AI 能查询真实数据、执行操作

- 实现 `HabitQueryTools`（查询历史记录、最近记录，查 MySQL）
- 实现 `HabitStatTools`（达成率计算、趋势统计）
- 实现 `SuggestionTools`（保存 AI 建议到 MongoDB）
- 将工具注册到 ChatClient
- 实现自定义 `LoggingAdvisor`（记录请求/响应/Token）
- 实现自定义 `SafetyFilterAdvisor`（基础输入过滤）
- 验证：问"我最近一周睡眠怎么样"，AI 调用工具查询并分析
- **Git commit + push**

### 阶段七：RAG 知识库（技术点 9）

**目标**：AI 回答有专业知识支撑

> 对应 API 文档：RagController（5 端点：导入/上传/检索/列表/删除）。

- 编写 RAG 知识库文档（`rag-docs/`：睡眠/运动/饮食指南）
- 配置 MongoDB Atlas VectorStore + DashScope EmbeddingModel
- 实现 `RagService`（文档导入：读取→分块→向量化→存储）
- 实现 `RagController`（5 端点：导入预设/上传文档/语义检索/文档列表/删除文档）
- 配置 `RetrievalAugmentationAdvisor`
- 验证：问"每天应该睡多久"，AI 基于知识库文档回答
- **Git commit + push**

### 阶段八：多智能体路由（技术点 13）

**目标**：不同意图路由到不同子 Agent

- 实现 `IntentRouter`（意图分类）
- 实现 `DataAnalysisAgent`（侧重调用工具查数据）
- 实现 `SuggestionAgent`（侧重 RAG 检索 + 生成建议）
- 实现 `ChatAgent`（日常多轮对话）
- 在 ChatService 中集成路由逻辑
- 验证：不同类型问题路由到对应 Agent
- **Git commit + push**

### 阶段九：趋势分析与图表（前端 Vue 组件）

**目标**：ECharts 可视化数据趋势（前端独立仓库 Vue 组件渲染）

> 对应 API 文档：AnalysisController（4 端点：趋势数据/达成率/概览统计/雷达图）。

- 实现 `AnalysisService`（统计计算：趋势数据/达成率/概览/雷达图）
- 创建 `AnalysisController` 返回 JSON 数据（4 端点）
  - `GET /api/analysis/trends?days=30`：趋势数据
  - `GET /api/analysis/achievement?period=WEEKLY`：达成率
  - `GET /api/analysis/overview?days=7`：概览统计
  - `GET /api/analysis/radar?period=WEEKLY`：雷达图数据
- 前端 `TrendView.vue` 中通过 ECharts 组件渲染图表：睡眠折线图、运动柱状图、饮水折线图、达标率雷达图（数据经 `/api/analysis/*` 获取）
- 验证：图表正确渲染历史数据趋势
- **Git commit + push**

### 阶段十：AI 分析结果、打卡提醒与收尾（技术点 14）

**目标**：技术点全覆盖 + 边界完善 + 交付

> 对应 API 文档：AiAnalysisController（6 端点：列表/详情/触发/删除/每日评价 + 最新）+ ReminderController（5 端点：CRUD + 启用/禁用切换），共 11 端点。

- 实现 `AiAnalysisService` + `AiAnalysisController`（6 端点）
  - `GET /api/ai-analysis?type=WEEKLY`：AI 分析列表
  - `GET /api/ai-analysis/latest`：最新分析结果
  - `GET /api/ai-analysis/{id}`：分析详情
  - `POST /api/ai-analysis/trigger`：触发周期分析
  - `POST /api/ai-analysis/daily`：生成每日评价（PRD 输出数据「每日评价」）
  - `DELETE /api/ai-analysis/{id}`：删除分析结果
- 实现 `ReminderService` + `ReminderController`（5 端点）
  - `GET /api/reminders`：提醒列表
  - `POST /api/reminders`：创建提醒
  - `PUT /api/reminders/{id}`：更新提醒
  - `POST /api/reminders/toggle/{id}`：启用/禁用切换
  - `DELETE /api/reminders/{id}`：删除提醒
- 创建 `Reminder` JPA Entity + `ReminderRepository` + reminder 表 DDL
- 全局异常处理完善（19 个错误码统一映射）
- Token 限制处理、安全处理
- 编写项目启动说明、功能演示说明
- 准备演示数据
- 全流程验证
- **Git commit + push**

---

## 六、边界处理

### 6.1 AI 调用失败处理

- **降级策略**：AI 调用失败返回预设友好提示
- **超时处理**：DashScope 配置 `read-timeout`，超时后返回降级提示
- **限流处理**：前端加节流（同一问题 3 秒内不重复提交）

### 6.2 Token 限制处理

- **输入截断**：`MessageWindowChatMemory` 设置 `maxMessages(20)`
- **长文本处理**：习惯记录描述字段限制 500 字
- **RAG 分块**：`TokenTextSplitter` 控制分块大小（500-800 Token）
- **模型选择**：日常对话用 `qwen-turbo`，深度分析用 `qwen-plus`

### 6.3 安全考虑

| 风险 | 处理方式 |
|---|---|
| API Key 泄露 | 环境变量 `${DASHSCOPE_API_KEY}` 注入，不硬编码 |
| Prompt 注入 | `SafetyFilterAdvisor` 过滤用户输入 |
| XSS | 前端渲染统一转义（Vue 默认文本插值与 v-text 自动转义，v-html 仅用于可信 Markdown） |
| SQL 注入 | JPA 参数化查询 |
| MongoDB 注入 | Spring Data MongoDB 参数化查询 |
| 越权访问 | 所有查询都带 `userId` 条件 |

### 6.4 数据一致性

- **MongoDB 对话记忆**：MongoDB 为主存储，TTL 索引自动过期（7天）
- **AI 分析结果缓存**：MongoDB TTL 索引（1天过期），避免重复调用 AI
- **打卡幂等**：MySQL `habit_record` 表 `uk_user_date` 唯一约束

---

## 七、验证步骤

### 7.1 分阶段验证清单

**阶段一验证（环境）**：
- [ ] `mvn spring-boot:run` 项目正常启动
- [ ] MySQL 连接成功，表结构自动创建
- [ ] MongoDB 连接成功（`mongosh` 可访问）
- [ ] DashScope API Key 配置正确
- [ ] Git 仓库已初始化并关联远程

**阶段二验证（CRUD）**：
- [ ] POST 录入习惯记录 → 200 返回
- [ ] GET 查询历史记录 → 返回列表
- [ ] 同一天重复录入 → 更新而非报错

**阶段三验证（前端页面）**：
- [ ] 前端 `npm run dev` 启动，`http://localhost:5173/` 5 个路由页面均可访问渲染
- [ ] 打卡表单提交后数据入库（经 `/api/habits`）
- [ ] 历史记录页通过 `/api/habits` 正确展示数据

**阶段四验证（AI 基础配置）**：
- [ ] ChatClient Bean 成功创建
- [ ] 通义千问模型连通（`chatClient.prompt().user("你好").call()` 返回回复）
- [ ] System Prompt 生效（AI 以"生活习惯助手"角色回应）

**阶段五验证（记忆+流式+会话）**：
- [ ] `POST /api/chat` 非流式对话能返回完整 JSON 响应
- [ ] 多轮对话记住上文
- [ ] 流式输出逐字显示
- [ ] 停止按钮能中断生成
- [ ] 重启项目后对话历史从 MongoDB 恢复
- [ ] `GET /api/sessions` 返回会话列表
- [ ] `GET /api/sessions/{id}/messages` 返回消息历史

**阶段六验证（Tool Calling）**：
- [ ] 问"我最近7天睡眠怎么样"→ AI 调用工具
- [ ] AI 基于真实数据分析

**阶段七验证（RAG）**：
- [ ] 问"每天应该喝多少水"→ AI 基于知识库回答

**阶段八验证（多智能体）**：
- [ ] 数据分析类问题 → DataAnalysisAgent
- [ ] 建议请求类问题 → SuggestionAgent
- [ ] 闲聊类问题 → ChatAgent

**阶段九验证（趋势分析）**：
- [ ] `GET /api/analysis/trends?days=7` 返回 7 天趋势数据
- [ ] `GET /api/analysis/achievement?period=WEEKLY` 返回达成率
- [ ] `GET /api/analysis/overview?days=7` 返回概览统计
- [ ] `GET /api/analysis/radar?period=WEEKLY` 返回雷达图数据
- [ ] 4 个 ECharts 图表（前端 Vue 组件）正确渲染

**阶段十验证（AI分析+提醒+收尾）**：
- [ ] `POST /api/ai-analysis/trigger` 能触发周期分析
- [ ] `POST /api/ai-analysis/daily` 能生成每日评价（含 score 字段）
- [ ] `GET /api/ai-analysis?type=WEEKLY` 返回分析列表
- [ ] `GET /api/reminders` 返回提醒列表
- [ ] `POST /api/reminders` 能创建提醒
- [ ] `POST /api/reminders/toggle/{id}` 能切换启用状态
- [ ] AI 调用超时返回友好降级提示
- [ ] 全流程演示通过

### 7.2 全流程演示验证

1. 启动后端 `mvn spring-boot:run`，启动前端 `npm run dev`（Vite 代理 `/api`）
2. 打开首页（前端 SPA），显示欢迎信息和今日打卡入口
3. 进入每日打卡页，录入今日数据，提交成功（调 `/api/habits`）
4. 进入历史记录页，查看历史打卡记录（调 `/api/habits`）
5. 进入趋势分析页，查看趋势图表和达成率（ECharts 组件调 `/api/analysis/*`）
6. 进入 AI 建议页，与 AI 流式对话（EventSource 调 `/api/chat/stream`）
7. 查看已保存的 AI 建议历史（调 `/api/sessions`）

---

## 八、实施注意事项

1. **版本一致性**：Spring AI 2.0.0 GA 必须搭配 Spring Boot 4.1.x + JDK 21
2. **双库共存**：MySQL 用 JPA，MongoDB 用 Spring Data MongoDB，两者在 Spring Boot 中可无缝共存
3. **MongoDB Atlas Vector Search**：需要 MongoDB Atlas 账号或本地配置向量索引。本地开发可用 MongoDB Community + 手动向量检索降级
4. **前后端分离 + 流式输出**：后端仅提供 SSE 端点，前端 Vue 工程用 EventSource 接收流式输出；前端独立仓库通过 Vite 代理或生产 CORS 与后端对接
5. **开发顺序**：严格按阶段一→十推进，每阶段验证通过且用户批准后进入下一阶段
6. **Git 管理**：每阶段完成后自动 commit + push 到 `https://github.com/Yu-xingyou/Agent_demo`

---

## 九、接口与开发阶段映射表

> 与《API接口文档设计计划.md》《API接口设计方案.md》保持一致，共 8 个后端 REST 模块、46 个 `/api` 端点（原 PageController 的 5 个页面路由已移除，前端改为 Vue 独立仓库 SPA 路由，不计入后端端点；目标模块含 5 个自定义目标打卡记录扩展接口）。

| 开发阶段 | 模块 | Controller | 端点数 | 累计 |
|---|---|---|---|---|
| 阶段二 | 习惯记录 + 习惯目标 | HabitController + GoalController | 7 + 9 = 16 | 16 |
| 阶段三 | 前端独立仓库（SPA 路由，不占后端端点） | — | 0 | 16 |
| 阶段四 | Spring AI 配置（无对外端点） | — | 0 | 16 |
| 阶段五 | AI 对话 + 会话管理 | ChatController + SessionController | 3 + 7 = 10 | 26 |
| 阶段六 | Tool Calling + Advisor（无对外端点） | — | 0 | 26 |
| 阶段七 | RAG 知识库 | RagController | 5 | 31 |
| 阶段八 | 多智能体路由（无新增端点） | — | 0 | 31 |
| 阶段九 | 趋势分析 | AnalysisController | 4 | 35 |
| 阶段十 | AI 分析结果 + 打卡提醒 + 收尾 | AiAnalysisController + ReminderController | 6 + 5 = 11 | 46 |

**端点分布说明**：

| Controller | 路径前缀 | 端点数 | 开发阶段 |
|---|---|---|---|
| HabitController | `/api/habits` | 7 | 阶段二 |
| GoalController | `/api/goals` + `/api/goal-records/records` | 9 | 阶段二 |
| ChatController | `/api/chat` `/api/chat/stream` `/api/chat/stop` | 3 | 阶段五 |
| SessionController | `/api/sessions` | 7 | 阶段五 |
| RagController | `/api/rag` | 5 | 阶段七 |
| AnalysisController | `/api/analysis` | 4 | 阶段九 |
| AiAnalysisController | `/api/ai-analysis` | 6 | 阶段十 |
| ReminderController | `/api/reminders` | 5 | 阶段十 |
| **合计（后端 REST 端点）** | | **46** | |
