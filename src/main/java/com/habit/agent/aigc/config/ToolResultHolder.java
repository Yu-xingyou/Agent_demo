package com.habit.agent.aigc.config;

import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具结果保持器，用来存储 tools 中得到的结果，请求 id 作为 key，value 为键值对数据
 */
public class ToolResultHolder {

    /** 请求 id 到工具结果键值对的全局存储（线程安全） */
    private static final Map<String, Map<String, Object>> HANDLER_MAP = new ConcurrentHashMap<>();

    /**
     * 工具类，禁止实例化
     */
    private ToolResultHolder() {
    }

    /**
     * 存储某次请求下某个字段的工具执行结果
     *
     * @param key    请求 id（作为本次调用的隔离键）
     * @param field  结果字段名
     * @param result 结果值（允许为 null）
     */
    public static void put(String key, String field, Object result) {
        Assert.notNull(key, "key is not null!");
        Assert.notNull(field, "field is not null!");
        HANDLER_MAP.computeIfAbsent(key, k -> new HashMap<>()).put(field, result);
    }

    /**
     * 获取某次请求下全部工具结果
     *
     * @param key 请求 id
     * @return 该请求对应的结果键值对；key 为 null 或不存在时返回 null
     */
    public static Map<String, Object> get(String key) {
        return key == null ? null : HANDLER_MAP.get(key);
    }

    /**
     * 获取某次请求下指定字段的工具结果
     *
     * @param key    请求 id
     * @param field  结果字段名
     * @return 对应字段的结果值；不存在时返回 null
     */
    public static Object get(String key, String field) {
        Assert.notNull(key, "key is not null!");
        Assert.notNull(field, "field is not null!");
        return Optional.ofNullable(HANDLER_MAP.get(key))
                .map(map -> map.get(field))
                .orElse(null);
    }

    /**
     * 移除某次请求的全部工具结果（请求结束时清理，避免内存泄漏）
     *
     * @param key 请求 id
     */
    public static void remove(String key) {
        Assert.notNull(key, "key is not null!");
        HANDLER_MAP.remove(key);
    }
}
