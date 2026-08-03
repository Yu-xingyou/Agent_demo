package com.habit.agent.agent.tools;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.common.vo.HabitGoalRecordVO;
import com.habit.agent.common.vo.HabitRecordVO;
import com.habit.agent.common.vo.SessionVO;
import com.habit.agent.service.HabitGoalRecordService;
import com.habit.agent.service.HabitService;
import com.habit.agent.service.SessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 习惯数据查询工具（阶段六：Tool Calling 与业务整合）。
 *
 * <p>把打卡记录、会话列表等「只读」业务查询能力暴露给 AI，
 * 让助手在对话中能调取真实业务数据作答。当前 demo 为单用户，
 * 工具内部固定使用 {@link AgentConstants#DEFAULT_USER_ID}，不暴露 userId 入参。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HabitQueryTools {

    private final HabitGoalRecordService habitGoalRecordService;

    private final SessionService sessionService;

    private final HabitService habitService;

    @Tool(description = "查询用户今天的内置习惯打卡记录（睡眠/运动/饮水/饮食/心情）。"
            + "当用户问「我今天打卡了吗」「今天记录了吗」「今日数据」时调用。")
    public String getTodayRecord() {
        try {
            HabitRecordVO record = habitService.getTodayRecord(AgentConstants.DEFAULT_USER_ID);
            if (record == null) {
                return "今天还没有任何打卡记录，鼓励用户现在就去记录一下吧。";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("今日打卡记录：\n");
            if (record.getSleepDuration() != null) {
                sb.append("- 睡眠：").append(record.getSleepDuration()).append(" 小时\n");
            }
            if (record.getExerciseDuration() != null) {
                sb.append("- 运动：").append(record.getExerciseDuration()).append(" 分钟\n");
            }
            if (record.getWaterIntake() != null) {
                sb.append("- 饮水：").append(record.getWaterIntake()).append(" ml\n");
            }
            if (record.getDietScore() != null) {
                sb.append("- 饮食评分：").append(record.getDietScore()).append("/5\n");
            }
            if (record.getMood() != null) {
                sb.append("- 心情：").append(record.getMood()).append("/5\n");
            }
            if (sb.length() == "今日打卡记录：\n".length()) {
                return "今天已创建打卡记录，但各维度数值均为空，鼓励用户补充填写。";
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("[HabitQueryTools] 查询今日记录失败: {}", e.getMessage());
            return "查询今日记录时发生错误：" + e.getMessage();
        }
    }

    @Tool(description = "查询用户最近 N 天的自定义目标打卡记录（含目标名称、数值、单位、打卡日期与备注）。"
            + "当用户问「最近打卡」「这段时间的自定义目标记录」时调用。")
    public String getRecentCheckins(
            @ToolParam(description = "最近天数，例如 7 表示最近一周") int days) {
        try {
            List<HabitGoalRecordVO> records =
                    habitGoalRecordService.getRecent(AgentConstants.DEFAULT_USER_ID, days);
            if (records.isEmpty()) {
                return "最近 " + days + " 天没有自定义目标的打卡记录。";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("最近 ").append(days).append(" 天共 ").append(records.size()).append(" 条打卡记录：\n");
            for (HabitGoalRecordVO r : records) {
                sb.append("- [").append(r.getRecordDate()).append("] 目标ID=").append(r.getGoalId());
                if (r.getValue() != null) {
                    sb.append(" 数值=").append(r.getValue());
                }
                if (r.getRemark() != null && !r.getRemark().isBlank()) {
                    sb.append(" 备注=").append(r.getRemark());
                }
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("[HabitQueryTools] 查询打卡记录失败: {}", e.getMessage());
            return "查询打卡记录时发生错误：" + e.getMessage();
        }
    }

    @Tool(description = "查询用户指定日期范围内的自定义目标打卡记录（升序，含数值与备注）。"
            + "当用户问「某段时间（如7月1日到7月15日）的打卡」时调用。")
    public String getCheckinsByRange(
            @ToolParam(description = "开始日期，格式 YYYY-MM-DD") String startDate,
            @ToolParam(description = "结束日期，格式 YYYY-MM-DD") String endDate) {
        try {
            List<HabitGoalRecordVO> records = habitGoalRecordService.getByDateRange(
                    AgentConstants.DEFAULT_USER_ID,
                    java.time.LocalDate.parse(startDate),
                    java.time.LocalDate.parse(endDate));
            if (records.isEmpty()) {
                return startDate + " 至 " + endDate + " 之间没有任何打卡记录。";
            }
            StringBuilder sb = new StringBuilder();
            sb.append(startDate).append(" 至 ").append(endDate).append(" 共 ")
                    .append(records.size()).append(" 条记录：\n");
            for (HabitGoalRecordVO r : records) {
                sb.append("- [").append(r.getRecordDate()).append("] 目标ID=").append(r.getGoalId());
                if (r.getValue() != null) {
                    sb.append(" 数值=").append(r.getValue());
                }
                if (r.getRemark() != null && !r.getRemark().isBlank()) {
                    sb.append(" 备注=").append(r.getRemark());
                }
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("[HabitQueryTools] 查询范围打卡记录失败: {}", e.getMessage());
            return "查询打卡记录时发生错误：" + e.getMessage();
        }
    }

    @Tool(description = "按目标 ID 查询该目标最近 N 天的打卡记录与平均值。"
            + "当用户问「某个具体目标（先 listActiveGoals 取 ID）最近怎么样」时调用。")
    public String getGoalRecentRecords(
            @ToolParam(description = "目标 ID（可先用 listActiveGoals 查询）") Long goalId,
            @ToolParam(description = "最近天数，例如 7") int days) {
        try {
            List<HabitGoalRecordVO> records =
                    habitGoalRecordService.getByGoalIdRecent(goalId, days);
            if (records.isEmpty()) {
                return "目标 " + goalId + " 最近 " + days + " 天没有打卡记录。";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("目标 ").append(goalId).append(" 最近 ").append(days).append(" 天共 ")
                    .append(records.size()).append(" 条：\n");
            for (HabitGoalRecordVO r : records) {
                sb.append("- [").append(r.getRecordDate()).append("]");
                if (r.getValue() != null) {
                    sb.append(" 数值=").append(r.getValue());
                }
                if (r.getRemark() != null && !r.getRemark().isBlank()) {
                    sb.append(" 备注=").append(r.getRemark());
                }
                sb.append("\n");
            }
            java.math.BigDecimal avg = habitGoalRecordService.calcAverage(goalId, days);
            sb.append("近 ").append(days).append(" 天平均值：")
                    .append(avg == null ? "无数据" : avg.stripTrailingZeros().toPlainString()).append("\n");
            return sb.toString();
        } catch (Exception e) {
            log.warn("[HabitQueryTools] 查询目标记录失败: {}", e.getMessage());
            return "查询目标记录时发生错误：" + e.getMessage();
        }
    }

    @Tool(description = "查询用户的历史对话会话列表（按最后消息时间倒序，含标题、状态与最后消息时间）。"
            + "当用户问「之前的对话」「历史会话」「我们聊过什么」时调用。")
    public String listSessions() {
        try {
            List<SessionVO> sessions = sessionService.listSessions();
            if (sessions.isEmpty()) {
                return "当前用户还没有任何对话会话。";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("共 ").append(sessions.size()).append(" 个会话：\n");
            for (SessionVO s : sessions) {
                sb.append("- conversationId=").append(s.getConversationId())
                        .append(" 标题=").append(s.getTitle())
                        .append(" 状态=").append(s.getStatus())
                        .append(" 最后消息=").append(s.getLastMessageTime())
                        .append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("[HabitQueryTools] 查询会话列表失败: {}", e.getMessage());
            return "查询会话列表时发生错误：" + e.getMessage();
        }
    }
}
