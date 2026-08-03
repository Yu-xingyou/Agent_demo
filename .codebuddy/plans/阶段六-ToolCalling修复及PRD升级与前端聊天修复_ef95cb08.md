---
name: 阶段六-ToolCalling修复及PRD升级与前端聊天修复
overview: 完成阶段六 ToolCalling 业务整合的收尾工作，全面升级 PRD 文档（除 agent模型.md 外），修复前端 AI 聊天界面输入文字无法发送的 bug。
todos:
  - id: fix-chat-path
    content: 修复 frontend/src/api/chat.js 中 sendMessage 和 stopChat 的双重 /api 前缀路径 Bug
    status: completed
  - id: verify-chat
    content: 验证前端聊天发送功能修复效果，确认流式对话端到端可用
    status: completed
    dependencies:
      - fix-chat-path
  - id: update-module-flow
    content: 升级 PRD/详细模块开发流程.md：将阶段五、六子模块状态从"待开发"更新为"已落地"，同步端点计数为 35
    status: completed
  - id: update-dev-plan
    content: 升级 PRD/agent_demo开发计划.md：更新阶段五/六状态标记、端点统计、架构图
    status: completed
  - id: update-api-docs
    content: 升级 PRD/API接口文档设计计划.md 和 PRD/API接口设计方案.md：同步 ChatController、SessionController 实现状态
    status: completed
  - id: update-vector-plan
    content: 升级 PRD/向量库搭建计划表.md：确认与当前项目状态一致并更新必要内容
    status: completed
  - id: git-commit
    content: 执行 git 提交并推送所有改动至远程仓库
    status: completed
    dependencies:
      - fix-chat-path
      - update-module-flow
      - update-dev-plan
      - update-api-docs
      - update-vector-plan
---

## 用户需求

用户提出 3 个并行需求：

### 1. 完成阶段六 Tool Calling 业务整合的收尾工作

根据 `.codebuddy/plans/模块六-ToolCalling业务整合_364fb5fd.md`，阶段六的 5 个 Todo 均已完成（create-tools、register-tools、fix-chat-api、upgrade-aichat、verify-build），代码已落地。需要做最终收尾验证，确保前后端完整联通。

### 2. 全面升级 PRD 文档（除 agent模型.md 外）

将 5 份 PRD 文档与当前项目实际代码状态同步，包括：

- `agent_demo开发计划.md`：更新阶段五、六状态为已落地，更新端点统计（35 已落地 + 16 待开发），更新架构图
- `详细模块开发流程.md`：将阶段五（记忆与流式、会话管理）、阶段六（Tool Calling）从"待开发"更新为"已落地"
- `API接口文档设计计划.md`：同步 ChatController、SessionController 的实现状态
- `API接口设计方案.md`：更新接口实现状态总览表
- `向量库搭建计划表.md`：确认与当前项目状态的一致性

### 3. 修复前端 AI 聊天界面无法发送消息的 Bug

用户在前端 AI 聊天页面输入文字后点击发送，消息无法正常发送给后端 AI。

**根因**：`frontend/src/utils/request.js` 中 axios 实例已配置 `baseURL: '/api'`，但 `frontend/src/api/chat.js` 中的 `sendMessage()` 和 `stopChat()` 方法使用了 `request.post('/api/chat', ...)` 和 `request.post('/api/chat/stop', ...)`，导致实际请求路径变为 `/api/api/chat`（双重前缀），后端返回 404。

## 技术方案

### 一、前端聊天 Bug 修复

**问题定位**：

- `request.js`：`axios.create({ baseURL: '/api' })` — 所有 axios 请求自动拼接 `/api` 前缀
- `chat.js` 第 7 行：`request.post('/api/chat', ...)` → 实际请求 `/api/api/chat`（错误）
- `chat.js` 第 95 行：`request.post('/api/chat/stop', ...)` → 实际请求 `/api/api/chat/stop`（错误）
- `streamMessage()` 使用原生 `fetch('/api/chat/stream')`，不受 axios baseURL 影响，路径正确

**修复策略**：
将 `chat.js` 中走 axios 的两处路径去掉 `/api` 前缀，使其与 `baseURL` 正确拼接：

- `'/api/chat'` → `'/chat'`
- `'/api/chat/stop'` → `'/chat/stop'`

**为什么只修 chat.js 而不是改 baseURL**：

- 项目中其他 API 模块（habit.js、goal.js、analysis.js）同样依赖 axios baseURL `/api`，改动 baseURL 会影响全局
- `chat.js` 是唯一出现路径双重前缀的文件，最小化修改范围

**涉及文件**：

- `frontend/src/api/chat.js`：[MODIFY] 第 7 行和第 95 行路径去掉 `/api` 前缀

### 二、阶段六收尾验证

阶段六 5 个 Todo 均已完成，收尾工作主要是验证前后端联通性：

1. 确认后端 ChatController 3 端点正常响应
2. 确认 ChatClient 正确注册 4 个工具 Bean
3. 确认前端修复后能正常发送流式对话请求
4. 提交代码并推送

**涉及文件**：

- 无需新增或修改后端代码，仅做验证

### 三、PRD 文档全面升级

5 份文档需要更新的核心内容：

| 文档 | 需更新的关键内容 |
| --- | --- |
| `agent_demo开发计划.md` | 阶段五/六状态标记、端点统计（35/51）、架构图补充 Tool Calling 层 |
| `详细模块开发流程.md` | 阶段五 3 子模块（记忆、流式、会话）→ 已落地，阶段六 2 子模块 → 已落地，端点计数更新 |
| `API接口文档设计计划.md` | ChatController(3) + SessionController(7) → 已实现 |
| `API接口设计方案.md` | 接口实现状态总览表同步，Chat/Session 状态更新 |
| `向量库搭建计划表.md` | 确认与当前项目状态一致，更新 RAG 接入时序中前端相关描述 |


**更新原则**：

- 以真实代码状态为唯一口径
- 保留原有文档结构和风格
- 只修改状态标记、数字统计、架构描述，不改变文档的规划性内容

## Agent Extensions

### SubAgent

- **code-explorer**
- Purpose: 在修改 PRD 文档前，快速确认各文档中需要更新的具体行号和内容，确保修改精准
- Expected outcome: 精确定位 5 份 PRD 文档中需要修改的状态标记、数字统计和架构描述段落

### Skill

- **doc-coauthoring**
- Purpose: 辅助撰写和修订 PRD 文档内容，确保文档结构清晰、表述准确、与代码状态一致
- Expected outcome: 5 份 PRD 文档内容完整升级，状态标记、数字统计、架构描述与当前代码同步