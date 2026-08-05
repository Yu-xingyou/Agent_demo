package com.habit.agent.agent.advisor;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.common.vo.HabitRecordVO;
import com.habit.agent.service.HabitService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 阶段 6-2：业务上下文注入 Advisor。
 *
 * <p>职责：在用户提问前自动补充「当前日期 + 星期 + 今日打卡概况」，
 * 让助手回答具备时间感知与数据感知能力（例如用户问"我今天睡够了吗"时无需再调工具）。
 *
 * <p>设计约束：
 * <ul>
 *   <li>注入文本严格限长 {@link #MAX_CONTEXT_LENGTH} 字符，只放摘要不放明细，避免上下文膨胀。</li>
 *   <li>打卡数据查询失败时<b>跳过注入</b>而非抛出，保证数据库异常不影响对话可用性。</li>
 *   <li>必须位于记忆 Advisor（{@code HIGHEST + 200}）之前执行，故取 {@code HIGHEST + 100}，
 *       使改写后的用户消息能被正确写入对话记忆。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContextInjectionAdvisor implements BaseAdvisor {

    /** 注入上下文的最大字符数。 */
    private static final int MAX_CONTEXT_LENGTH = 200;

    private final HabitService habitService;

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        try {
            String context = buildContext();
            if (context.isBlank()) {
                return request;
            }
            log.debug("[Advisor:Context] 注入业务上下文：{}", context);
            return request.mutate()
                    .prompt(request.prompt().augmentUserMessage(userMessage -> userMessage.mutate()
                            .text("[系统背景信息]\n" + context + "\n\n[用户提问]\n" + userMessage.getText())
                            .build()))
                    .build();
        } catch (Exception e) {
            // 上下文注入属增强能力，失败时静默跳过，不影响对话
            log.warn("[Advisor:Context] 上下文注入失败（已跳过）：{}", e.getMessage());
            return request;
        }
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        return response;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

    // ===== 内部辅助 =====

    /** 组装「日期 + 星期 + 今日打卡概况」摘要，超长时截断。 */
    private String buildContext() {
        LocalDate today = LocalDate.now();
        StringBuilder sb = new StringBuilder();
        sb.append("今天是 ").append(today)
          .append(" ").append(today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.CHINA))
          .append("。");
        sb.append(describeTodayRecord());

        String context = sb.toString();
        return context.length() <= MAX_CONTEXT_LENGTH
                ? context
                : context.substring(0, MAX_CONTEXT_LENGTH);
    }

    /** 描述今日打卡概况；查询异常或无记录时给出中性描述。 */
    private String describeTodayRecord() {
        HabitRecordVO record;
        try {
            record = habitService.getTodayRecord(AgentConstants.DEFAULT_USER_ID);
        } catch (Exception e) {
            log.debug("[Advisor:Context] 今日打卡查询失败（已跳过该项）：{}", e.getMessage());
            return "";
        }
        if (record == null) {
            return "用户今天还没有打卡记录。";
        }

        List<String> parts = new ArrayList<>();
        if (record.getSleepDuration() != null) {
            parts.add("睡眠" + record.getSleepDuration() + "小时");
        }
        if (record.getExerciseDuration() != null) {
            parts.add("运动" + record.getExerciseDuration() + "分钟");
        }
        if (record.getWaterIntake() != null) {
            parts.add("饮水" + record.getWaterIntake() + "毫升");
        }
        if (record.getMood() != null) {
            parts.add("心情" + record.getMood() + "/5");
        }
        return parts.isEmpty()
                ? "用户今天已打卡但未填写具体指标。"
                : "用户今日打卡概况：" + String.join("、", parts) + "。";
    }
}
