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

    /** 线程安全的系统提示词引用（健康习惯助手） */
    private final AtomicReference<String> chatSystemMessage = new AtomicReference<>();

    /** 线程安全的路由智能体系统提示词引用 */
    private final AtomicReference<String> routeAgentSystemMessage = new AtomicReference<>();

    /** 线程安全的睡眠建议智能体系统提示词引用 */
    private final AtomicReference<String> sleepAgentSystemMessage = new AtomicReference<>();

    /** 线程安全的饮食建议智能体系统提示词引用 */
    private final AtomicReference<String> dietAgentSystemMessage = new AtomicReference<>();

    /** 线程安全的运动建议智能体系统提示词引用 */
    private final AtomicReference<String> exerciseAgentSystemMessage = new AtomicReference<>();

    /** 线程安全的习惯打卡智能体系统提示词引用 */
    private final AtomicReference<String> checkinAgentSystemMessage = new AtomicReference<>();

    /** 线程安全的健康知识讲解智能体系统提示词引用 */
    private final AtomicReference<String> knowledgeAgentSystemMessage = new AtomicReference<>();

    @PostConstruct
    public void init() {
        String text = aiProperties.getSystem().getChat().getText();
        if (text == null || text.isBlank()) {
            log.warn("系统提示词 habit.ai.prompt.system.chat.text 为空，将使用内置默认提示词");
            text = DEFAULT_SYSTEM_PROMPT;
        }
        chatSystemMessage.set(text);
        log.info("系统提示词加载完成，长度={} 字符", text.length());

        String routeText = aiProperties.getSystem().getRouteAgent().getText();
        if (routeText == null || routeText.isBlank()) {
            log.warn("路由智能体提示词 habit.ai.prompt.system.route-agent.text 为空，将使用内置默认提示词");
            routeText = DEFAULT_ROUTE_AGENT_PROMPT;
        }
        routeAgentSystemMessage.set(routeText);
        log.info("路由智能体提示词加载完成，长度={} 字符", routeText.length());

        loadAgentPrompt(aiProperties.getSystem().getSleepAgent(),
                sleepAgentSystemMessage, DEFAULT_SLEEP_AGENT_PROMPT, "sleep-agent");
        loadAgentPrompt(aiProperties.getSystem().getDietAgent(),
                dietAgentSystemMessage, DEFAULT_DIET_AGENT_PROMPT, "diet-agent");
        loadAgentPrompt(aiProperties.getSystem().getExerciseAgent(),
                exerciseAgentSystemMessage, DEFAULT_EXERCISE_AGENT_PROMPT, "exercise-agent");
        loadAgentPrompt(aiProperties.getSystem().getCheckinAgent(),
                checkinAgentSystemMessage, DEFAULT_CHECKIN_AGENT_PROMPT, "checkin-agent");
        loadAgentPrompt(aiProperties.getSystem().getKnowledgeAgent(),
                knowledgeAgentSystemMessage, DEFAULT_KNOWLEDGE_AGENT_PROMPT, "knowledge-agent");
    }

    /** 加载单个子 Agent 提示词（空时回退内置默认） */
    private void loadAgentPrompt(AIProperties.System.Chat config,
                                 AtomicReference<String> ref,
                                 String fallback, String name) {
        String text = config == null ? null : config.getText();
        if (text == null || text.isBlank()) {
            log.warn("{} 提示词 habit.ai.prompt.system.{}.text 为空，将使用内置默认提示词", name, name);
            text = fallback;
        }
        ref.set(text);
        log.info("{} 智能体提示词加载完成，长度={} 字符", name, text.length());
    }

    /** 内置兜底提示词（当 yml 未配置时使用） */
    private static final String DEFAULT_SYSTEM_PROMPT =
            "你是健康习惯管理助手，帮助用户培养并追踪健康生活习惯。";

    /** 内置兜底路由智能体提示词（当 yml 未配置时使用） */
    private static final String DEFAULT_ROUTE_AGENT_PROMPT =
            "你是生活习惯助手的路由智能体。请判断用户意图，并将其归类为以下之一：" +
            "SLEEP（睡眠建议）、DIET（饮食建议）、EXERCISE（运动建议）、" +
            "CHECKIN（习惯打卡）、KNOWLEDGE（健康知识讲解）、HEALTH（通用健康习惯）。" +
            "仅输出对应的类型名称，不要输出多余解释。";

    /** 内置兜底睡眠建议智能体提示词 */
    private static final String DEFAULT_SLEEP_AGENT_PROMPT =
            "你是睡眠健康建议助手，专注于帮助用户改善睡眠质量与作息规律。" +
            "你可以查询和记录用户的睡眠相关习惯（如入睡时间、起床时间、睡眠时长），" +
            "并基于数据给出温和、可量化的作息调整建议。请保持专业、不诊断疾病，" +
            "涉及严重睡眠障碍时建议就医。";

    /** 内置兜底饮食建议智能体提示词 */
    private static final String DEFAULT_DIET_AGENT_PROMPT =
            "你是饮食健康建议助手，专注于帮助用户改善饮食结构、饮水量与用餐规律。" +
            "你可以查询和记录用户的饮食、饮水相关习惯，并基于数据给出温和、" +
            "可量化的膳食与补水建议。请保持专业、不诊断疾病，不提供极端节食方案。";

    /** 内置兜底运动建议智能体提示词 */
    private static final String DEFAULT_EXERCISE_AGENT_PROMPT =
            "你是运动健康建议助手，专注于帮助用户建立合适的运动习惯。" +
            "你可以查询和记录用户的运动相关习惯，协助设定运动目标与提醒，" +
            "并基于数据给出温和、可量化的运动强度与频率建议。" +
            "请考虑用户体能基础，避免推荐高强度或高风险动作。";

    /** 内置兜底习惯打卡智能体提示词 */
    private static final String DEFAULT_CHECKIN_AGENT_PROMPT =
            "你是习惯打卡助手，帮助用户完成各类生活习惯的打卡、查询与维护。" +
            "你可以记录、查询、删除习惯打卡，管理目标与提醒，并查看会话历史。" +
            "请聚焦用户的打卡操作请求，准确调用工具，并在操作后给出简洁确认。";

    /** 内置兜底健康知识讲解智能体提示词 */
    private static final String DEFAULT_KNOWLEDGE_AGENT_PROMPT =
            "你是健康知识讲解助手，专注于以通俗易懂、科学严谨的方式解答用户的" +
            "健康科普问题（睡眠、运动、饮水、饮食等）。你只做知识讲解，不执行" +
            "任何写操作（不打卡、不设定目标）。涉及医疗诊断或用药时，建议用户咨询专业医生。";
}
