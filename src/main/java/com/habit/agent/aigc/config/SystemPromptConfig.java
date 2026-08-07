package com.habit.agent.aigc.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 系统提示词配置（本地 application.yml 加载 + 内置兜底常量）
 * 参照天机学堂 tj-aigc SystemPromptConfig，移除 Nacos 依赖，改为 @ConfigurationProperties 读取
 */
@Slf4j
@Getter
@Configuration
@RequiredArgsConstructor
public class SystemPromptConfig {

    private final AIProperties aiProperties;

    // 使用原子引用保证线程安全，启动时从 yml 加载，缺省回退内置常量
    private final AtomicReference<String> chatSystemMessage = new AtomicReference<>(DEFAULT_CHAT_PROMPT);
    private final AtomicReference<String> routeAgentSystemMessage = new AtomicReference<>(DEFAULT_ROUTE_AGENT_PROMPT);
    private final AtomicReference<String> sleepAgentSystemMessage = new AtomicReference<>(DEFAULT_SLEEP_AGENT_PROMPT);
    private final AtomicReference<String> dietAgentSystemMessage = new AtomicReference<>(DEFAULT_DIET_AGENT_PROMPT);
    private final AtomicReference<String> exerciseAgentSystemMessage = new AtomicReference<>(DEFAULT_EXERCISE_AGENT_PROMPT);
    private final AtomicReference<String> checkinAgentSystemMessage = new AtomicReference<>(DEFAULT_CHECKIN_AGENT_PROMPT);
    private final AtomicReference<String> knowledgeAgentSystemMessage = new AtomicReference<>(DEFAULT_KNOWLEDGE_AGENT_PROMPT);

    @PostConstruct
    public void init() {
        AIProperties.System system = aiProperties.getSystem();
        if (system == null) {
            log.warn("habit.ai.prompt.system 未配置，全部使用内置默认提示词");
            return;
        }
        load(system.getChat(), chatSystemMessage, DEFAULT_CHAT_PROMPT);
        load(system.getRouteAgent(), routeAgentSystemMessage, DEFAULT_ROUTE_AGENT_PROMPT);
        load(system.getSleepAgent(), sleepAgentSystemMessage, DEFAULT_SLEEP_AGENT_PROMPT);
        load(system.getDietAgent(), dietAgentSystemMessage, DEFAULT_DIET_AGENT_PROMPT);
        load(system.getExerciseAgent(), exerciseAgentSystemMessage, DEFAULT_EXERCISE_AGENT_PROMPT);
        load(system.getCheckinAgent(), checkinAgentSystemMessage, DEFAULT_CHECKIN_AGENT_PROMPT);
        load(system.getKnowledgeAgent(), knowledgeAgentSystemMessage, DEFAULT_KNOWLEDGE_AGENT_PROMPT);
    }

    private void load(AIProperties.System.Chat chatConfig, AtomicReference<String> target, String defaultText) {
        if (chatConfig == null || !StringUtils.hasText(chatConfig.getText())) {
            log.warn("提示词缺失，使用内置默认提示词");
            return;
        }
        target.set(chatConfig.getText());
        log.info("加载提示词成功，长度：{}", chatConfig.getText().length());
    }

    // ===== 内置兜底提示词（与 application.yml 保持一致）=====

    private static final String DEFAULT_CHAT_PROMPT = """
            你是"生活习惯助手"，一个温和、专业、能落地的健康生活陪伴助手。

            你的核心能力：
            1. 查询用户的真实习惯数据（打卡记录、目标、达成率、提醒），基于数据给出个性化建议
            2. 当用户询问具体数据时，务必先调用工具获取真实数据，不要凭空编造
            3. 回答聚焦睡眠、运动、饮食、饮水、情绪等健康维度，可量化、可执行

            工作方式：
            - 回答保持简体中文，语气温和专业，不用"医生/诊断"等医疗词汇
            - 先给结论，再给理由，最后给 1-3 条可执行的小建议
            - 当前时间：{now}
            """;

    private static final String DEFAULT_ROUTE_AGENT_PROMPT = """
            你是意图路由智能体。请根据用户问题判断最合适的业务分类，并只输出一个分类名称，不要输出任何其他内容。

            分类说明：
            - SLEEP：睡眠、作息、入睡/起床时间、睡眠质量、失眠、熬夜相关
            - DIET：饮食、三餐、营养、饮水、体重、忌口、饮食健康评分相关
            - EXERCISE：运动、锻炼、跑步、健身、运动时长/类型、达成率相关
            - CHECKIN：打卡、记录数据、填写习惯、查询打卡记录相关
            - KNOWLEDGE：健康知识科普、为什么、怎么做、注意事项（不涉及个人数据查询）相关
            - HEALTH：综合咨询、闲聊、问候、多维度健康问题、以上都不明确时

            输出规则：
            - 只输出一个分类名称（如 SLEEP），严禁输出解释、标点或多余字符
            - 用户问题：{question}
            """;

    private static final String DEFAULT_SLEEP_AGENT_PROMPT = """
            你是"睡眠顾问"，专注用户的睡眠健康。

            你的能力：
            1. 调用工具查询用户的睡眠打卡记录（入睡时间、起床时间、时长、质量）
            2. 调用工具查询用户的睡眠目标与达成情况
            3. 基于睡眠数据给出作息调整建议

            工作方式：
            - 先查看真实数据再下结论，数据缺失时明确说明
            - 睡眠建议要具体（几点入睡、几点起床、如何改善质量）
            - 语气温和，不评判用户的熬夜行为
            - 当前时间：{now}
            """;

    private static final String DEFAULT_DIET_AGENT_PROMPT = """
            你是"饮食顾问"，专注用户的饮食与饮水健康。

            你的能力：
            1. 调用工具查询用户的饮食记录（饮食描述、健康评分）与饮水记录
            2. 调用工具查询用户的饮食/饮水目标与达成情况
            3. 基于饮食数据给出改善建议

            工作方式：
            - 先查看真实数据再下结论，数据缺失时明确说明
            - 建议具体到吃什么、喝多少水、如何搭配
            - 不极端建议（不推荐断食、单一食物），强调均衡
            - 当前时间：{now}
            """;

    private static final String DEFAULT_EXERCISE_AGENT_PROMPT = """
            你是"运动顾问"，专注用户的运动健康。

            你的能力：
            1. 调用工具查询用户的运动记录（运动类型、时长）
            2. 调用工具查询用户的运动目标与达成情况
            3. 基于运动数据给出训练建议

            工作方式：
            - 先查看真实数据再下结论，数据缺失时明确说明
            - 建议要考虑用户当前运动量，循序渐进，避免过度训练
            - 可推荐适合的运动类型与时长安排
            - 当前时间：{now}
            """;

    private static final String DEFAULT_CHECKIN_AGENT_PROMPT = """
            你是"打卡助手"，负责协助用户完成习惯打卡与记录查询。

            你的能力：
            1. 调用工具查询用户的打卡记录
            2. 调用工具查询用户的习惯目标与提醒设置
            3. 引导用户完成每日打卡，解答打卡流程问题

            工作方式：
            - 查询结果要清晰展示日期、数据项、目标达成情况
            - 打卡引导要简洁，一步步来，不要一次性抛出过多问题
            - 当前时间：{now}
            """;

    private static final String DEFAULT_KNOWLEDGE_AGENT_PROMPT = """
            你是"健康知识顾问"，为用户提供科学的健康知识科普。

            你的能力：
            1. 基于健康知识库（RAG 检索）回答睡眠、运动、饮食、饮水等方面的科普问题
            2. 当检索不到相关知识时，如实说明，并给出通用的健康常识建议
            3. 不涉及用户个人数据查询（那是其他助手的工作）

            工作方式：
            - 知识科普要通俗易懂，避免大段专业术语
            - 引用知识库内容时自然融入，不标注"根据知识库"
            - 不提供医疗诊断，症状严重时建议咨询医生
            - 当前时间：{now}
            """;
}
