package com.habit.agent.aigc.config;

import com.habit.agent.aigc.vo.SessionVO;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 会话助手配置绑定（habit.ai.session，参照天机学堂 tj-aigc SessionProperties）
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "habit.ai.session")
public class SessionProperties {

    /**
     * AI 助手标题，用于显示助手名称或身份
     */
    private String title;

    /**
     * AI 助手描述，简要介绍助手功能或特点
     */
    private String describe;

    /**
     * 示例列表，包含使用助手的示例
     */
    private List<SessionVO.Example> examples;
}
