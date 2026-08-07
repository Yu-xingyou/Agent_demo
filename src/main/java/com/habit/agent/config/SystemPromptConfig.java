package com.habit.agent.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 系统提示词配置（加载到内存，供流式对话注入）。
 *
 * <p>参照示例 {@code SystemPromptConfig} 的设计：
 * 使用 {@link AtomicReference} 持有提示词以保证线程安全，
 * 在 {@code @PostConstruct} 时从 {@link AIProperties} 加载。
 * 与示例差异：示例从 Nacos 远程拉取并监听热更新；本项目无 Nacos，
 * 改为从 application.yml 读取（修改 yml 后随应用重启生效，
 * 如需运行时热刷可接入 {@code @RefreshScope} 或配置中心）。</p>
 */
@Slf4j
@Getter
@Configuration
@RequiredArgsConstructor
public class SystemPromptConfig {

    private final AIProperties aiProperties;

    /** 线程安全的系统提示词引用 */
    private final AtomicReference<String> chatSystemMessage = new AtomicReference<>();

    @PostConstruct
    public void init() {
        String text = aiProperties.getSystem().getChat().getText();
        if (text == null || text.isBlank()) {
            log.warn("系统提示词 habit.ai.prompt.system.chat.text 为空，将使用内置默认提示词");
            text = DEFAULT_SYSTEM_PROMPT;
        }
        chatSystemMessage.set(text);
        log.info("系统提示词加载完成，长度={} 字符", text.length());
    }

    /** 内置兜底提示词（当 yml 未配置时使用） */
    private static final String DEFAULT_SYSTEM_PROMPT =
            "你是健康习惯管理助手，帮助用户培养并追踪健康生活习惯。";
}
