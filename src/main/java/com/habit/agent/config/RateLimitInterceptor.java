package com.habit.agent.config;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.common.exception.RateLimitException;

/**
 * 简单令牌桶限流拦截器（无 Redis 依赖，单应用可用）。
 * 仅对高成本的 LLM 入口（对话 / 分析生成）生效，保护后端不被刷爆。
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    /** 每个窗口允许的请求数 */
    private static final int LIMIT = 30;
    /** 窗口时长（毫秒）：每分钟 */
    private static final long WINDOW_MS = 60_000L;
    /** 需要限流的路径（与控制器 @RequestMapping 对齐） */
    private static final Set<String> PATHS = Set.of("/api/chat", "/api/ai-analysis/generate");

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        if (!PATHS.contains(uri)) {
            return true;
        }
        String key = request.getRemoteAddr() + ":" + uri;
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket());
        if (!bucket.tryAcquire()) {
            throw new RateLimitException(AgentConstants.CODE_RATE_LIMITED, "请求过于频繁，请稍后再试");
        }
        return true;
    }

    private static final class Bucket {
        private final AtomicInteger tokens = new AtomicInteger(LIMIT);
        private volatile long lastRefill = System.currentTimeMillis();

        synchronized boolean tryAcquire() {
            long now = System.currentTimeMillis();
            if (now - lastRefill >= WINDOW_MS) {
                tokens.set(LIMIT);
                lastRefill = now;
            }
            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }
    }
}
