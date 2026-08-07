package com.habit.agent.config;

import com.habit.agent.vo.SessionVO;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 生活习惯助手 · 会话首屏配置。
 *
 * <p>映射 {@code application.yml} 中 {@code habit.ai.session} 配置块，
 * 用于前端首屏展示助手名称、简介与快捷示例（记录习惯 / 查询数据 / 设定目标 / 习惯分析）。
 * 与天机课程推荐模板的区别：示例内容已按本项目四大能力做领域适配。</p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "habit.ai.session")
public class SessionProperties {

    /**
     * 助手标题，用于显示助手的名称或身份。
     */
    private String title;

    /**
     * 助手描述，简要介绍助手的功能或特点。
     */
    private String describe;

    /**
     * 示例列表，包含一些使用助手的快捷示例。
     */
    private List<SessionVO.Example> examples;

    /**
     * 热门问题列表，用于前端首屏「热门问题」榜，随机抽取展示。
     */
    private List<SessionVO.HotQuestion> hotQuestions;
}
