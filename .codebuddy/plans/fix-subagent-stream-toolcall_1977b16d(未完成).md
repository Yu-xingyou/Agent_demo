---
name: fix-subagent-stream-toolcall
overview: 修复子 Agent 真流式调用工具时 ChunkMerger 抛 NoSuchElementException 导致整条流崩溃的问题：根因是子 Agent 复用全局带工具 ChatClient，DashScope 工具调用流式分片偶发不规范 delta，触发 OpenAiChatModel 聚合器内部 Optional.get() 崩溃且未被降级捕获。计划在子 Agent 层构建无工具的独立流式 ChatClient、并对聚合异常做兜底捕获。
---

## 用户需求

用户在运行 AI 对话时发现子 Agent 流式调用报错，日志堆栈明确为：

```
java.util.NoSuchElementException: No value present
  at org.springframework.ai.openai.OpenAiChatModel$ChunkMerger.lambda$chunkToChatCompletion$6(OpenAiChatModel.java:1130)
  at ... FluxBufferPredicate$BufferPredicateSubscriber.onComplete
  at org.springframework.ai.chat.model.MessageAggregator : Aggregation Error
```

该异常发生在 `OpenAiChatModel` 内部 `ChunkMerger` 对工具调用（tool_calls）流式分片做聚合（`Optional.get()`）时，绕过了子 Agent `handleStream()` 末端的 `onErrorResume` 降级逻辑，导致整条流无法降级、前端断流报错。

用户要求：定位漏洞并修复，给出实施计划。

## 产品概述

修复后，AI 对话在子 Agent 触发工具调用（打卡查询/统计分析/目标设定等）的流式场景下不再因 DashScope 偶发不规范的工具 delta 而崩流；调用失败时统一降级为可读话术，保证对话连续性。

## 核心功能

### 1. 根因修复：子 Agent 不再复用全局带工具/记忆的 ChatClient

- 子 Agent（数据分析、改善建议、闲聊）当前通过 `AbstractSubAgent` 复用全局 `chatClient`（含 4 个工具 + 记忆 Advisor + RAG Advisor + parallelToolCalls）。
- 当子 Agent 自行发起工具调用时，会重跑工具循环并放大 `ChunkMerger` 聚合脆弱性，且聚合期 `NoSuchElementException` 发生在 SDK 内部、无法被业务层 `onErrorResume` 捕获。
- 改为：为子 Agent 提供**不含工具、不含记忆/工具 Advisor** 的轻量 ChatClient（仅保留安全/日志 Advisor 作为安全网），工具调用统一由主链路 Director 协调。

### 2. 聚合期崩溃兜底

- 在 `OpenAiChatModel` 调用层之外，通过分段缓冲与异常隔离，确保单条工具 delta 聚合失败不影响最终回复下发。
- 若子 Agent 当前必须保留工具能力（如数据分析 Agent 需查数），则对工具轮次采用「先 `.call()` 取完整结果再包装为单条 Flux」的降级路径，规避流式分片聚合。

### 3. 可观测与协议兼容

- 错误事件 `error` 仍由 `ChatServiceImpl.stream` 统一下发，前端解析逻辑不变。
- 路由意图 `meta`、文本增量 `chunk`、工具状态 `tool_call`、结束 `done` 事件协议保持向后兼容。

## 技术栈（沿用现有，不引入新依赖）

| 层次 | 技术 | 说明 |
| --- | --- | --- |
| 框架 | Spring Boot 4.1.0 + Java 21 | 现有 |
| AI | Spring AI 2.0.0 | 现有 |
| 模型 | 通义千问 DashScope（OpenAI 兼容模式，qwen-plus） | 现有 |
| 流式栈 | spring-boot-starter-webflux（Reactor） | 现有 |
| 序列化 | Jackson | 现有 |


不新增 Maven 依赖。

## 实现方案

### 3.1 根因定位（已核实）

`AbstractSubAgent.handleStream()`（第 63-80 行）直接复用注入的全局 `chatClient`。该 `chatClient` 在 `ChatClientConfig.chatClient()` 中装配了 `defaultTools(toolProvider)`（4 个工具类共 14 个 `@Tool`）、`MessageChatMemoryAdvisor`、`SafeRetrievalAdvisor`。

当子 Agent（尤其是 `DataAnalysisAgent`/`SuggestionAgent`，其 system 提示词明确要求"调用工具获取真实数据"）触发工具调用时：

- 流式响应包含 `tool_calls` delta 分片；
- DashScope 在 OpenAI 兼容模式下，偶发下发不规范的工具 delta（如 `function` 字段或 `index` 缺失）；
- `OpenAiChatModel$ChunkMerger.chunkToChatCompletion`（line 1130）对这些分片做 `Optional.get()` 聚合，缺失时抛 `NoSuchElementException`；
- 该异常发生在 SDK 内部 `FluxBufferPredicate.onComplete` → `MessageAggregator` 错误回调，**不经过** `handleStream` 末端 `.onErrorResume(...)`，因此业务降级失效，整条流中断，`MessageAggregator` 仅打印 `Aggregation Error`。

### 3.2 修复策略（两层防御）

**第一层（架构修复，治本）**：子 Agent 角色是"领域专家回复"，工具调用应由主链路 `ChatServiceImpl.stream` → Director 统一协调。子 Agent 不应再自带工具与记忆 Advisor。新增一个**专用子 Agent ChatClient Bean**（`subAgentChatClient`），仅装配：

- `system` 提示词由子 Agent 自己通过 `.system(...)` 提供；
- Advisor：仅保留 `SafetyFilterAdvisor`（+10）、`LoggingAdvisor`（+20）作为安全/可观测兜底；
- **不挂** `MessageChatMemoryAdvisor`、**不挂** `SafeRetrievalAdvisor`、**不挂** `defaultTools`；
- `parallelToolCalls` 无关（无工具）。

`AbstractSubAgent` 改为注入 `subAgentChatClient`，`handleStream()` 与 `handle()` 均使用该 Bean，从而彻底消除子 Agent 侧的工具循环与 `ChunkMerger` 暴露面。

**第二层（降级兜底，治标）**：若后续确实需要在子 Agent 内调用工具，对工具轮次采用「同步 `.call()` 取完整结果 → 包装为 `Flux.just(ChatClientResponse)`」的非流式降级路径，规避流式聚合。本次按"第一层治本"落地，并在 `AbstractSubAgent` 注释中固化这一约束（子 Agent 不得自带工具），保留未来扩展点。

### 3.3 关键改动点

1. `ChatClientConfig`：新增 `subAgentChatClient` Bean（轻量、无工具/无记忆/无 RAG）。
2. `AbstractSubAgent`：构造函数注入 `subAgentChatClient`（替换原 `chatClient` 字段名 `chatClient` → 保留字段但指向轻量 Bean）；`handle()` / `handleStream()` 改为使用该 Bean；`handleStream` 的 `onErrorResume` 保留作为网络层异常的兜底（非聚合崩溃，但仍是良好实践）。
3. `DataAnalysisAgent` / `SuggestionAgent` / `ChatAgent`：继承关系不变，system 提示词微调——移除"调用工具"的措辞（工具统一由 Director 调），改为"基于已提供的上下文作答"，避免模型在无工具时仍尝试调用导致反复失败。
4. `ChatServiceImpl.stream`：主链路调用 `agent.handleStream()` 不变；Director 已通过全局 `chatClient`（带工具）在 `AbstractSubAgent` 之外完成工具协调——**注意**：当前 `ChatServiceImpl` 调用 `agent.handleStream` 时，工具调用实际发生在子 Agent 内部（因子 Agent 复用了带工具的 chatClient）。修复后，子 Agent 不再带工具，工具调用需上移到 `ChatServiceImpl` 或保留全局 chatClient 在 Director 层。

**架构决策（修正）**：为避免重复工具循环与回归，采用「**Director 层统一工具调用**」模式：

- 主链路 `ChatServiceImpl.stream` 使用**全局 `chatClient`**（带工具）发起请求；
- 子 Agent 仅作为"角色 system 提示词 + 流式渲染"的承载（用 `subAgentChatClient` 仅做纯文本/带记忆的流式输出）；
- 若意图命中需工具的 Agent（DATA_ANALYSIS/SUGGESTION），由 `ChatServiceImpl` 直接以全局 `chatClient` + 对应子 Agent 的 `systemPrompt()` 调用 `.stream().chatClientResponse()`，使工具循环发生在框架自动注册的 `ToolCallingAdvisor` 中（单一入口，聚合面收敛）；
- 闲聊（CHAT）走 `subAgentChatClient` 纯流式即可。

此方案将工具循环收敛到**唯一入口**（主链路全局 chatClient），子 Agent 退化为"提示词 + 渲染单元"，既消除 `ChunkMerger` 暴露面，又保留多智能体角色路由语义。

### 3.4 降级与容错

- `AbstractSubAgent.handleStream` 末端 `onErrorResume` 保留：对订阅后异步抛出的网络/超时异常返回兜底 `ChatClientResponse`（已在位，无需改）。
- 聚合期 `NoSuchElementException` 属 SDK 内部错误，业务层无法捕获；通过"子 Agent 无工具"从根上避免触发，辅以全局 chatClient 单一入口收敛。
- 若主链路全局 chatClient 仍偶发该异常，由 `ChatServiceImpl.stream` 的 `.onErrorResume` → `errorEvent` 统一下发 `error` 事件（已在位）。

### 3.5 性能与爆炸半径

- 子 Agent 轻量 Bean 无工具/无 RAG，单次流式延迟与 Token 消耗下降。
- 工具循环收敛到主链路，避免子 Agent 内重复记忆写入与 RAG 检索。
- 不改动 `AiAnalysisServiceImpl`、`RagServiceImpl`、`ChatController` SSE 协议、`ChatStreamEvent` 契约。
- 仅改 chat 链路相关文件，符合项目约束。

## 目录结构

```
d:/javacode/agent_demo/
├── src/main/java/com/habit/agent/
│   ├── config/
│   │   └── ChatClientConfig.java          # [MODIFY] 新增 subAgentChatClient Bean
│   │                                      #   （无 defaultTools / 无记忆 Advisor / 无 RAG，
│   │                                      #   仅 SafetyFilter + Logging）；更新注释说明
│   │                                      #   子 Agent 不得自带工具的架构约束
│   ├── agent/router/
│   │   ├── AbstractSubAgent.java          # [MODIFY] 注入 subAgentChatClient；
│   │   │                                  #   handle()/handleStream() 改用该 Bean；
│   │   │                                  #   注释固化"子 Agent 不挂工具"约束
│   │   ├── DataAnalysisAgent.java         # [MODIFY] system 提示词移除"调用工具"措辞，
│   │   │                                  #   改为基于上下文作答；fallback 不变
│   │   ├── SuggestionAgent.java           # [MODIFY] 同上
│   │   └── ChatAgent.java                 # [MODIFY] 同上（轻量调整）
│   └── service/impl/
│       └── ChatServiceImpl.java           # [MODIFY] stream() 中按意图路由：
│           │                              #   DATA_ANALYSIS/SUGGESTION 用全局 chatClient
│           │                              #   + 子Agent.systemPrompt() 发起流式（工具循环收敛主链路）；
│           │                              #   CHAT 用 subAgentChatClient；复用现有映射与事件逻辑
└── PRD/
    └── 详细模块开发流程.md                 # [MODIFY] 阶段九/5-2 补记：子 Agent 无工具约束、
                                            #   ChunkMerger 聚合崩溃根因与修复结论、Commit 记录
```

## 关键代码结构

```java
// ChatClientConfig.java —— 新增轻量子 Agent ChatClient
@Bean
public ChatClient subAgentChatClient(ChatClient.Builder builder,
                                     SafetyFilterAdvisor safetyFilterAdvisor,
                                     LoggingAdvisor loggingAdvisor) {
    return builder
            .defaultAdvisors(safetyFilterAdvisor, loggingAdvisor)
            // 故意不挂 MessageChatMemoryAdvisor / SafeRetrievalAdvisor / defaultTools：
            // 子 Agent 仅做角色化流式输出，工具调用统一由主链路 Director（全局 chatClient）协调，
            // 避免子 Agent 内重跑工具循环放大 ChunkMerger 聚合脆弱性。
            .build();
}

// AbstractSubAgent.java —— 使用轻量 Bean
protected final ChatClient subAgentChatClient;

protected AbstractSubAgent(ChatClient subAgentChatClient) {
    this.subAgentChatClient = subAgentChatClient;
}
// handle() / handleStream() 内部 .stream()/.call() 均基于 subAgentChatClient
```

# Agent Extensions

<extensions>

- type: SubAgent
name: code-explorer
Purpose: 在落地前精确核对 ChatClientConfig 中 chatClient/reportChatClient 的完整装配、AbstractSubAgent 字段注入来源、ChatServiceImpl.stream 调用 agent.handleStream 的上下文，确认 subAgentChatClient 注入点与系统提示词现状，避免遗漏任何复用全局 chatClient 的子 Agent 调用方。
Expected outcome: 输出改造点清单（文件路径 + 行号 + 现状代码片段），确保"子 Agent 无工具"约束落地且无遗漏调用方，且不误伤 AiAnalysisServiceImpl / RagServiceImpl。