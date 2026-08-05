package com.habit.agent.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 异步任务执行器配置（阶段十 AI 分析异步生成）。
 *
 * <p>为 {@code @Async("agentTaskExecutor")} 提供独立线程池，避免阻塞 Web 请求线程；
 * 核心线程常驻，队列缓冲突发分析请求，超出容量时由 CallerRunsPolicy 在调用线程兜底，防止雪崩。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("agentTaskExecutor")
    public Executor agentTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(16);
        executor.setThreadNamePrefix("agent-async-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
}
