---
name: fix-title-generation-conversationId
overview: 修复 ChatServiceImpl.generateTitle 因复用带 MessageChatMemoryAdvisor 的全局 chatClient、未传 conversationId 而抛 IllegalArgumentException 的问题，新增专用 titleChatClient（不挂记忆 Advisor），并清理异常堆栈噪音。
todos:
  - id: add-title-chatclient
    content: 在 ChatClientConfig 新增 titleChatClient Bean（不挂记忆顾问）
    status: pending
  - id: wire-title-client
    content: ChatServiceImpl 注入 titleChatClient 并改 generateTitle 使用它
    status: pending
    dependencies:
      - add-title-chatclient
  - id: tune-log-level
    content: 将 generateTitle 降级日志由 warn 降为 debug
    status: pending
    dependencies:
      - wire-title-client
  - id: compile-verify
    content: 执行 mvn 编译验证 BUILD SUCCESS
    status: pending
    dependencies:
      - tune-log-level
  - id: commit-push
    content: 提交并推送修复至 origin/main
    status: pending
    dependencies:
      - compile-verify
---

## 用户需求

修复后端日志中每次调用会话标题生成接口（POST /api/chat/title）都抛出 `IllegalArgumentException: conversationId cannot be null` 异常堆栈的问题。

## 产品概述

标题生成接口当前复用注入了 `MessageChatMemoryAdvisor` 的全局 `ChatClient`，该顾问在 `before` 阶段强制要求 `conversationId` 参数；而标题生成是无状态、一次性任务，未传入会话 ID，触发断言异常后被 catch 降级（功能未崩，但每次生成都打一整页错误堆栈，属错误用法）。

## 核心功能

- 为标题生成新增强隔离的专用 `ChatClient`（不挂记忆顾问），消除异常噪音。
- 复用现有 `reportChatClient`/`directorChatClient` 同款「无记忆专用客户端」模式，保持架构一致。
- 保持现有降级兜底逻辑与返回格式不变，仅切换底层客户端，对外行为无感。
- 关联现象（MongoDB 本地不支持 `$search` 的 WARN）属环境限制、已降级，本次不改动，修复后日志更易聚焦。

## 技术栈

- 语言/框架：Java 17 + Spring Boot 4 + Spring AI 2.0.0
- 现有装配模式：`ChatClient.Builder` + 多个 `@Bean` 专用客户端（已存在 `directorChatClient`、`reportChatClient` 不挂 `MessageChatMemoryAdvisor`）

## 实现方案

### 策略

沿用既有「无状态专用 ChatClient」模式（与 `reportChatClient` 完全一致），新增 `titleChatClient` Bean：复用 `ChatClient.Builder`（自动读取 model/温度/API Key），仅装配 `safetyFilterAdvisor`、`loggingAdvisor`、`safeRetrievalAdvisor`（这三个在 missing conversationId 时均安全），**不挂** `MessageChatMemoryAdvisor`，并以低 `temperature(0.3)` 保证标题简洁。随后在 `ChatServiceImpl` 注入 `titleChatClient`，将 `generateTitle` 内的 `chatClient.prompt()` 改为 `titleChatClient.prompt()`。

### 关键技术决策

- **为何新增 Bean 而非传入 conversationId**：标题生成属一次性无状态任务，不应污染会话记忆窗口，且与 `reportChatClient`/`directorChatClient` 的设计意图一致（其类注释已明确说明未传会话 ID 会触发该异常）。新增专用 Bean 是最小改动、零行为污染的方案。
- **复用而非改造全局 chatClient**：全局 `chatClient` 承载多轮对话与工具循环，强依赖 `MessageChatMemoryAdvisor`，不能移除；分离专用 Bean 避免回归。
- **降级日志降级**：将 `generateTitle` 的 `log.warn(... e)` 改为 `log.debug(... e)`，因降级属预期路径，避免生产环境噪音（异常堆栈消失后更需精简日志等级）。

### 性能与可靠性

- 新增 Bean 为单例、构造期零 IO，无启动开销；标题生成调用路径与原来一致（一次 `.call().content()`）。
- 异常兜底保留：标题生成失败时仍截断首条消息返回，保证接口 200。

### 执行注意（防回归）

- `ChatServiceImpl` 使用 `@RequiredArgsConstructor`，新增 `private final ChatClient titleChatClient;` 字段即可自动注入，无需改构造器。
- `temperature` 设 0.3 与全局默认区分，使标题稳定简短（≤15 字约束由 prompt 保障）。
- 不触碰 `SafeRetrievalAdvisor`、MongoDB 记忆仓库等无关逻辑，控制爆炸半径。

## 架构设计

无新增架构模式，遵循现有「多专用 ChatClient 按场景隔离」实践：

- 全局 `chatClient`（带记忆+工具） → 正常对话/流式
- `directorChatClient` → 意图路由
- `reportChatClient` → 分析报告
- **`titleChatClient`（新增）** → 标题生成

## 目录结构

```
src/main/java/com/habit/agent/
├── config/
│   └── ChatClientConfig.java          # [MODIFY] 新增 titleChatClient Bean，复用 Builder，不挂 MessageChatMemoryAdvisor，temperature=0.3，装配 safety/logging/rag 三顾问；补充类注释说明用途。
└── service/impl/
    └── ChatServiceImpl.java           # [MODIFY] 注入 titleChatClient 字段（final，RequiredArgsConstructor 自动装配）；generateTitle 改用 titleChatClient.prompt()；降级日志由 warn 降为 debug。
```

## 关键代码结构

新增 Bean 形态（参考已存在的 `reportChatClient`）：

```java
@Bean
public ChatClient titleChatClient(ChatClient.Builder builder,
        SafetyFilterAdvisor safetyFilterAdvisor,
        LoggingAdvisor loggingAdvisor,
        SafeRetrievalAdvisor safeRetrievalAdvisor) {
    return builder
            .defaultSystem(systemPromptResource)
            .defaultAdvisors(safetyFilterAdvisor, loggingAdvisor, safeRetrievalAdvisor)
            .defaultOptions(OpenAiChatOptions.builder().temperature(0.3).parallelToolCalls(false))
            .build();
}
```

## Agent Extensions

### SubAgent

- **code-explorer**
- Purpose: 在生成最终计划前，对 `ChatClientConfig` 与 `ChatServiceImpl` 当前装配/字段做最终核对，确认 `@RequiredArgsConstructor` 注入路径与现有 `reportChatClient` 模式完全一致，避免计划落地时出现字段未注入或 Bean 冲突。
- Expected outcome: 确认修改点精确无误，新增 `titleChatClient` 与注入字段不会导致编译或运行时 Bean 解析问题。