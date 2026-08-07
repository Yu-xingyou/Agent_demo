package com.habit.agent.memory;

/**
 * 自定义会话记忆仓库扩展接口（基于 Spring AI 官方 {@code MongoChatMemoryRepository} 之上）。
 *
 * <p>参照天机 aigc 示例 {@code com.tianji.aigc.memory.MyChatMemoryRepository} 设计，
 * 适配本项目 MongoDB 记忆仓库。官方 {@code MongoChatMemoryRepository} 仅提供
 * {@code add / get / deleteByConversationId}，不具备「删除最近 N 条消息」能力，
 * 故在此接口中扩展 {@link #optimization(String)} 用于清理路由智能体写入的内部转发记录。</p>
 */
public interface MyChatMemoryRepository {

    /**
     * 根据对话 ID 优化对话记录，删除最后的 2 条消息。
     *
     * <p>原因：这两条消息是由路由智能体（ROUTE）落库的内部转发消息——一条用户提问、
     * 一条路由分类结果（智能体名称），真正的回答由后续子智能体生成。
     * 为确保历史消息的完整性、避免用户在前端看到内部路由结果，需要将中间转发消息清理掉。</p>
     *
     * @param conversationId 对话的唯一标识符
     */
    void optimization(String conversationId);
}
