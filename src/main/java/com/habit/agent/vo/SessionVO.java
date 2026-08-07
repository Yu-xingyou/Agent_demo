package com.habit.agent.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话视图对象（SessionVO）—— 用于会话列表 / 会话详情 / 首屏配置的接口返回。
 *
 * <p>对应 MongoDB 实体 {@code com.habit.agent.entity.mongo.ChatSession}，
 * 仅暴露对外展示所需的字段，不泄露内部主键 / 用户归属等细节。</p>
 *
 * <p>其中 {@link #title} / {@link #describe} / {@link #examples} 由
 * {@code SessionProperties}（{@code habit.ai.session}）注入，作为前端首屏的
 * 助手介绍与快捷示例；{@link #sessionId} / {@link #createTime} / {@link #updateTime}
 * 由会话实体转换得到，用于会话列表展示。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionVO {

    /**
     * 会话ID，用于唯一标识当前的生活习惯助手会话。
     */
    private String sessionId;

    /**
     * 助手标题，用于显示助手的名称或身份（来自首屏配置）。
     */
    private String title;

    /**
     * 助手描述，简要介绍助手的功能或特点（来自首屏配置）。
     */
    private String describe;

    /**
     * 示例列表，包含一些使用助手的快捷示例（来自首屏配置）。
     */
    private List<Example> examples;

    /**
     * 创建时间（会话列表倒序展示用）。
     */
    private LocalDateTime createTime;

    /**
     * 最后更新时间（会话列表倒序展示用）。
     */
    private LocalDateTime updateTime;

    /**
     * Example 表示首屏每个快捷示例的标题和描述（如「记录习惯 / 帮我记录今天23点睡觉」）。
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Example {

        /**
         * 示例分类标题，描述示例所属的能力类型（记录习惯 / 查询数据 / 设定目标 / 习惯分析）。
         */
        private String title;

        /**
         * 示例的具体提问内容，直接可作为一次对话的初始消息。
         */
        private String describe;
    }

    /**
     * HotQuestion 表示首屏「热门问题」榜单中的单条问题（如「如何改善睡眠质量？」）。
     *
     * <p>与 {@link Example} 的区别：Example 是「能力分类 + 示范提问」的引导式示例，
     * HotQuestion 是「真实可点击的高频问题」，更偏用户视角的热门榜。</p>
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HotQuestion {

        /**
         * 问题标题，热门榜上展示的简短标题。
         */
        private String title;

        /**
         * 问题内容，点击后直接作为一次对话的初始消息。
         */
        private String question;
    }
}
