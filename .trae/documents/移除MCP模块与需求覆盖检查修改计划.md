# 修改计划：移除 MCP 模块 + 需求覆盖检查 + PRD 文档统一更新

## 一、任务概述

根据用户要求：
1. 从项目中移除 MCP（Model Context Protocol）相关功能
2. 对照原始需求文档 `agent模型.md` 检查项目功能覆盖情况
3. 统一更新 PRD 文件夹中的文档，确保一致性

## 二、MCP 出现位置总览

经全文检索，MCP 相关内容共出现在 **3 个文件中**：

| 文件 | 出现次数 | 涉及内容 |
|---|---|---|
| `pom.xml` | 2 处 | MCP 客户端依赖（第 77-81 行） |
| `PRD/agent_demo开发计划.md` | 8 处 | 技术选型表、技术点映射表、pom 示例、阶段十、验证清单、接口映射表 |
| `PRD/详细模块开发流程.md` | 14 处 | 总览表、子模块 1-1 依赖、子模块 10-3 全文、验收标准 |

`API接口设计方案.md` 和 `API接口文档设计计划.md` 中无 MCP 内容，**无需修改**。

## 三、各文件具体修改点

### 文件 1：pom.xml

**文件路径**：`d:\javacode\agent_demo\pom.xml`

**修改内容**：删除第 77-81 行的 MCP 客户端依赖。

删除内容：
```xml
        <!-- MCP 客户端 (技术点15: 多模态 MCP) -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-mcp-client</artifactId>
        </dependency>
```

**修改后效果**：依赖从 11 个减为 10 个，项目仍可正常编译运行。

---

### 文件 2：PRD/agent_demo开发计划.md

**文件路径**：`d:\javacode\agent_demo\PRD\agent_demo开发计划.md`

共 **8 处修改**：

#### 修改点 2-1：文档副标题
- **原文**：`覆盖课程 15 项技术点。`
- **修改为**：`覆盖课程 14 项技术点。`

#### 修改点 2-2：版本对应关系表（Spring AI 描述）
- **原文**：`| Spring AI | **2.0.0 GA** | 2026-06-12 发布；Tool Calling 一等公民、Advisor Chain 重构、MCP 原生集成 |`
- **修改为**：`| Spring AI | **2.0.0 GA** | 2026-06-12 发布；Tool Calling 一等公民、Advisor Chain 重构、增强 RAG 与记忆管理 |`

#### 修改点 2-3：技术选型清单表
- **操作**：删除 MCP 客户端整行
- **删除行**：`| **MCP 客户端** | spring-ai-starter-mcp-client | 随 Spring AI 2.0 | 原生 MCP 集成；@McpTool 注解支持 |`

#### 修改点 2-4：pom.xml 代码示例中的 MCP 依赖块
- **操作**：删除 MCP 依赖块（第 129-133 行）

#### 修改点 2-5：课程技术点映射表标题
- **原文**：`### 4.1 课程 15 项技术点映射表`
- **修改为**：`### 4.1 课程 14 项技术点映射表`

#### 修改点 2-6：删除技术点 15 整行
- **操作**：删除第 15 行"多模态 MCP"
- **删除行**：`| 15 | 多模态 MCP | \`config/\` MCP 客户端 | spring-ai-starter-mcp-client 集成 |`
- **编号调整**：前 14 项编号保持不变，总数从 15 减为 14

#### 修改点 2-7：阶段十标题与任务列表
- **标题原文**：`### 阶段十：AI 分析结果、打卡提醒与收尾（技术点 14/15）`
- **修改为**：`### 阶段十：AI 分析结果、打卡提醒与收尾（技术点 14）`

- **删除任务行**：`- 配置 MCP Client 连接示例 MCP Server（技术点 15）`

#### 修改点 2-8：验证清单与接口映射表
- **验证清单删除**：`- [ ] MCP Client 能连接 MCP Server`

- **接口映射表原文**：`| 阶段十 | AI 分析结果 + 打卡提醒 + MCP | AiAnalysisController + ReminderController | 6 + 5 = 11 | 48 |`
- **修改为**：`| 阶段十 | AI 分析结果 + 打卡提醒 + 收尾 | AiAnalysisController + ReminderController | 6 + 5 = 11 | 48 |`

> 端点数 48 不受影响（MCP 不涉及对外 API 端点）

---

### 文件 3：PRD/详细模块开发流程.md

**文件路径**：`d:\javacode\agent_demo\PRD\详细模块开发流程.md`

共 **7 处修改**（子模块 10-3 为重写）：

#### 修改点 3-1：总览表阶段十
- **原文**：`| 阶段十 | 3 个子模块 | AiAnalysisController + ReminderController + MCP/收尾 | 14/15 | 11 |`
- **修改为**：`| 阶段十 | 2 个子模块 | AiAnalysisController + ReminderController + 收尾完善 | 14 | 11 |`

#### 修改点 3-2：总览表合计行
- **原文**：`| **合计** | **25 个子模块** | | | **48** |`
- **修改为**：`| **合计** | **24 个子模块** | | | **48** |`

#### 修改点 3-3：子模块 1-1 依赖列表
- **原文**：MCP Client
- **修改为**：去掉 MCP Client，保留其余依赖

#### 修改点 3-4：阶段十标题
- **原文**：`## 阶段十：AI 分析结果、打卡提醒与收尾（技术点 14/15）`
- **修改为**：`## 阶段十：AI 分析结果、打卡提醒与收尾（技术点 14）`

#### 修改点 3-5：子模块 10-3 重命名并重写
- **原标题**：`### 子模块 10-3：MCP 集成、全局异常完善与交付文档`
- **新标题**：`### 子模块 10-3：全局异常完善与交付文档`

**涉及文件列表修改**：
- 删除 `application.yml` 追加 MCP 配置
- 删除 `McpConfig.java` 文件
- 保留 `GlobalExceptionHandler.java`、`ChatService.java`、Prompt 模板、README、demo-guide、bailing-platform

**详细说明修改**：
- 删除 `application.yml` 追加 MCP 配置段落
- 删除 `McpConfig` 相关段落
- 保留全局异常处理完善、ChatService 完善、Prompt 模板、README、demo-guide、bailing-platform 等内容

**验收标准修改**：
- 删除 2 条 MCP 相关验收项
- 保留其余验收标准（AI 超时降级、19 个错误码、README、全流程演示等）

---

### 文件 4 & 5：API 接口文档（无需修改）

- `PRD/API接口设计方案.md` — 无 MCP 内容，无需修改
- `PRD/API接口文档设计计划.md` — 无 MCP 内容，无需修改

## 四、需求覆盖情况检查

### 4.1 必做功能覆盖（5 项，全部覆盖）

| 序号 | 原始需求（必做） | 覆盖状态 | 项目实现位置 |
|---|---|---|---|
| 1 | 每日录入作息数据 | 已覆盖 | HabitController + checkin.html |
| 2 | 每日录入饮食与运动数据 | 已覆盖 | HabitController + checkin.html |
| 3 | 查看历史记录 | 已覆盖 | HabitController + history.html |
| 4 | 根据历史记录生成 AI 分析结果 | 已覆盖 | AiAnalysisController + ChatService + Tool Calling |
| 5 | 输出生活习惯改善建议 | 已覆盖 | AiAnalysisController（suggestion 字段）+ SuggestionAgent + RAG |

**输出数据项覆盖**：
- 每日评价 → AiAnalysisController `POST /api/ai-analysis/daily`
- 周期趋势总结 → AiAnalysisController `triggerAnalysis`（WEEKLY/MONTHLY）
- 生活习惯风险提示 → AiAnalysisDoc.riskWarning 字段
- 改善建议 → AiAnalysisDoc.suggestion 字段 + SuggestionAgent

### 4.2 可选功能覆盖（5 项，全部覆盖）

| 序号 | 原始需求（可选） | 覆盖状态 | 项目实现位置 |
|---|---|---|---|
| 1 | 打卡提醒 | 已覆盖 | ReminderController + Reminder Entity |
| 2 | 周报/月报统计 | 已覆盖 | AnalysisController + AiAnalysisController |
| 3 | 图表展示趋势 | 已覆盖 | AnalysisController + ECharts（trend.html） |
| 4 | 自定义习惯目标 | 已覆盖 | GoalController + HabitGoal Entity |
| 5 | 达成率统计 | 已覆盖 | AnalysisController `GET /api/analysis/achievement` |

### 4.3 页面需求覆盖（5 个页面，全部覆盖）

| 原始需求页面 | 覆盖状态 | 对应模板 |
|---|---|---|
| 首页/欢迎页 | 已覆盖 | index.html |
| 每日打卡页 | 已覆盖 | checkin.html |
| 历史记录页 | 已覆盖 | history.html |
| 趋势分析页 | 已覆盖 | trend.html |
| AI 建议页 | 已覆盖 | ai-chat.html |

## 五、与原始需求的差异分析（超出部分）

以下功能/技术不在原始需求文档中，是后续迭代中用户主动要求添加或课程技术点覆盖需要的增强：

| 序号 | 超出内容 | 类型 | 添加原因 | 是否保留 |
|---|---|---|---|---|
| 1 | MongoDB + MySQL 双库架构 | 技术架构 | 用户主动要求添加 MongoDB 用于 AI 数据存储 | 保留 |
| 2 | 多智能体路由（IntentRouter + 3 个子 Agent） | 功能增强 | 课程技术点 13（路由工作流+多智能体） | 保留 |
| 3 | RAG 知识库 + 向量检索（MongoDB Atlas Vector Search） | 功能增强 | 课程技术点 9（RAG + 向量检索） | 保留 |
| 4 | 会话管理（SessionController，7 端点） | 功能增强 | 对话记忆管理扩展，课程技术点 12 | 保留 |
| 5 | Tool Calling（3 个工具类） | 功能增强 | 课程技术点 3/8（Tool Calling 与业务整合） | 保留 |
| 6 | 自定义 Advisor（Logging/SafetyFilter） | 技术增强 | 课程技术点 7（Advisors 自定义增强） | 保留 |
| 7 | SSE 流式输出 + 停止生成 + 非流式对话 | 功能增强 | 课程技术点 5/11 + 企业标准规范 | 保留 |
| 8 | 全局异常处理（19 个错误码） | 工程增强 | 企业级规范，提升项目完整性 | 保留 |
| 9 | 百炼平台智能体说明文档 | 文档增强 | 课程技术点 14（百炼平台智能体） | 保留 |
| 10 | ~~多模态 MCP~~ | ~~技术增强~~ | ~~课程技术点 15（多模态 MCP）~~ | **移除** |

**结论**：移除 MCP 后，项目仍覆盖 14 个课程技术点，且所有原始需求（必做 + 可选）均完整覆盖。

## 六、技术点调整后映射（14 个）

| # | 课程技术点 | 项目落地位置 | 实现方式 |
|---|---|---|---|
| 1 | AI 大模型核心原理 | 全项目理解基础 | 通过通义千问实践 Token/温度/上下文窗口 |
| 2 | OpenAI/百炼 API 对接 | application.yml + OpenAI 兼容模式 | spring-ai-starter-model-openai + DashScope |
| 3 | Tool Calling | agent/tools/ | @Tool 注解定义业务工具 |
| 4 | Prompt Engineering | resources/prompts/ | System/Analysis/Suggestion 提示词模板 |
| 5 | 流式对话 | ChatController | stream() SSE 流式输出 + POST /api/chat 非流式对话 |
| 6 | System 角色设定 | ChatClientConfig | defaultSystem() 设定助手人格 |
| 7 | Advisors 自定义增强 | agent/advisors/ | 自定义 Logging/Safety/Context Advisor |
| 8 | Tool Calling 与业务整合 | agent/tools/ | 工具调用 JPA/Mongo Repository 查询真实数据 |
| 9 | RAG + 向量检索 | RagService + MongoDB Atlas VectorStore | RetrievalAugmentationAdvisor |
| 10 | Spring AI 集成项目 | 整体 | AI 能力贯穿所有页面 |
| 11 | 流式输出与停止生成 | ChatController + chat-stream.js | SseEmitter + Flux + 前端停止按钮 |
| 12 | 对话记忆管理 | agent/memory/MongoChatMemoryRepository | MongoDB 实现 ChatMemoryRepository |
| 13 | 路由工作流+多智能体 | agent/router/ | ChatClient + 手动路由 |
| 14 | 百炼平台智能体 | 文档说明 | 百炼平台配置说明（可选演示） |

## 七、修改执行顺序

1. **先修改 pom.xml**：移除 MCP 依赖
2. **修改 agent_demo开发计划.md**：主文档，调整技术点数量和阶段描述
3. **修改 详细模块开发流程.md**：子模块文档，重写子模块 10-3
4. **核对 API 接口文档**：确认无需修改（端点数不变）
5. **最终验证**：全文搜索 MCP 关键词，确认无遗漏

## 八、关键数据变化汇总

| 指标 | 修改前 | 修改后 | 变化 |
|---|---|---|---|
| 课程技术点 | 15 个 | 14 个 | -1 |
| 总子模块数 | 25 个 | 24 个 | -1 |
| 阶段十子模块 | 3 个 | 2 个 | -1 |
| API 端点数 | 48 个 | 48 个 | 不变 |
| 阶段数量 | 10 个 | 10 个 | 不变 |
| Controller 数量 | 9 个 | 9 个 | 不变 |
| pom 依赖数 | 11 个 | 10 个 | -1 |
