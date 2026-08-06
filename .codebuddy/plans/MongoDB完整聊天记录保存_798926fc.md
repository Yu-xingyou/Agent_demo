---
name: MongoDB完整聊天记录保存
overview: 将 ChatMemoryConfig 中 MessageWindowChatMemory 的 maxMessages 从 20 放大为极大值，使 Spring AI 记忆库把完整聊天记录持久化到 MongoDB 的 ai_chat_memory 集合，解决对话超过 20 条后早期记录被丢弃导致保存不完整的问题。
todos:
  - id: expand-memory-window
    content: 修改 ChatMemoryConfig 的 MEMORY_WINDOW_SIZE 为 Integer.MAX_VALUE 并同步更新注释
    status: completed
  - id: commit-push
    content: git 提交并推送本次修改到远程仓库
    status: completed
    dependencies:
      - expand-memory-window
---

## 用户需求

MongoDB 中保存的 Conversation（对话聊天记录）不完整，根因是记忆窗口限制了仅保留最近 20 条消息，早期聊天记录被截断丢失。

## 产品概述

调整对话记忆持久化策略，使每一次对话的完整聊天记录都保存到 MongoDB 中，不再被数量窗口截断。

## 核心特性

- 放大 Spring AI `MessageWindowChatMemory` 的记忆窗口限制，让完整的用户与助手消息全部持久化到 MongoDB 集合 `ai_chat_memory`。
- 同步修正 `ChatMemoryConfig` 中关于「保留最近 20 条消息窗口」的注释说明，保持文档与实现一致。
- 历史会话读取（`getMessages`）自动返回完整记录，无需改动上层逻辑。

## 技术栈

- 后端框架：Spring Boot 4 + Spring AI 2.0
- 对话记忆：`MessageWindowChatMemory`（Spring AI）+ `MongoChatMemoryRepository`（持久化到 MongoDB `ai_chat_memory`）
- 语言：Java 17

## 实现方案

### 总体策略

根因已定位：`ChatMemoryConfig.java` 中 `MEMORY_WINDOW_SIZE = 20` 经 `.maxMessages(...)` 注入，导致 `MessageWindowChatMemory` 仅保留最近 20 条消息，超出部分被丢弃，造成 MongoDB 保存不完整。按照用户选择的「放大记忆窗口」方案，将该常量改为极大值（`Integer.MAX_VALUE`），使窗口不再截断，完整聊天记录均落到 `ai_chat_memory`。

### 关键技术决策

- **为何用 `Integer.MAX_VALUE` 而非 0/负数**：Spring AI `MessageWindowChatMemory` 的 `maxMessages` 语义为「保留的最大消息条数」，0 或非法值行为未定义或仍截断；设置为 `Integer.MAX_VALUE` 是成熟且安全的「近似不限制」做法，避免引入不可预期的行为。
- **不改动 `ChatSession` 实体 / `ChatSessionRepository` / `touchSession` 冗余计数**：这些只管理会话元数据，与消息记忆解耦，用户已确认仅放大窗口即可。
- **不改 `getMessages` 与 `ChatServiceImpl` 流程**：`getMessages` 通过 `chatMemory.get(cid)` 读回，窗口放大后天然返回完整记录，零回归风险。

### 性能与可靠性

- 时间/空间复杂度：`MessageWindowChatMemory` 每次读写仍按 conversationId 定位文档，O(1) 级定位；消息总量增长带来线性存储增长，属于用户明确接受的取舍。
- 瓶颈与缓解：长对话会使发给模型的上下文变长，可能挤占 token。但用户明确选择保留完整记录，且 `ContextInjectionAdvisor`/`SafeRetrievalAdvisor` 已对注入上下文做长度约束，不会因窗口放大而失控。
- 历史数据：已落库的 `ai_chat_memory` 文档中此前被丢弃的早期消息无法补回，仅对后续新对话生效，属预期范围。

## 实现说明

- 仅修改 `ChatMemoryConfig.java` 一处常量与对应注释，不触碰其它文件，改动范围最小、blast radius 可控。
- 同步更新类注释与 Bean 注释中关于「默认保留最近 20 条消息窗口」的描述，改为「完整保留全部消息」，避免文档误导。
- 保留 `mongoWindowChatMemory` 方法改名以规避 Spring AI 自动配置名称冲突的设计，本次不改动。

## 架构设计

本次为单点配置调整，沿用现有 `ChatMemoryConfig` 提供的 `ChatMemory` Bean 链路（`MongoChatMemoryRepository` → `mongoWindowChatMemory` → `MessageChatMemoryAdvisor` → `ChatClient`），不涉及架构变更。

## 目录结构

```
src/main/java/com/habit/agent/config/
└── ChatMemoryConfig.java  # [MODIFY] 将 MEMORY_WINDOW_SIZE 由 20 改为 Integer.MAX_VALUE；更新 maxMessages 注入与相关注释（类注释第 17-18 行、Bean 注释第 37-38 行），说明完整保留全部消息。
```