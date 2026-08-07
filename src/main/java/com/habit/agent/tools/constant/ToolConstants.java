package com.habit.agent.tools.constant;

/**
 * Agent 工具相关常量。
 *
 * <p>工具描述（Tools）与参数描述（ToolParams）集中在此，
 * 便于 {@code @Tool} / {@code @ToolParam} 复用，并保持与 PRD 第 10 节工具表一致。</p>
 */
public interface ToolConstants {

    interface Tools {
        String RECORD_HABIT = "录入或更新用户某一天的生活习惯打卡记录（睡眠/运动/饮水/饮食/心情），同一天重复调用为覆盖更新";
        String QUERY_HABIT_RECORDS = "查询用户的生活习惯打卡记录，支持按最近N天、日期范围或指定单日查询";
        String DELETE_HABIT_RECORD = "删除用户指定的一条例行打卡记录（按记录id或日期定位）";
        String LIST_ACTIVE_GOALS = "查询用户当前已启用的生活习惯目标（含本周完成度），例如 睡眠8小时、运动30分钟";
        String SET_GOAL = "创建或更新用户的生活习惯目标（睡眠/运动/饮水/饮食/自定义），内置类型同类型唯一、重复调用为更新";
        String DELETE_GOAL = "删除用户指定的生活习惯目标，并级联删除其关联打卡";
        String CREATE_REMINDER = "创建一条打卡提醒（标题、提醒时间、类型、重复星期）";
        String MANAGE_REMINDER = "管理提醒：更新/删除/启用或停用/列出全部提醒";
        String ANALYZE_HABIT_TREND = "分析用户近N天的生活习惯趋势或周报，支持趋势/概览/达成率/雷达/AI摘要";
        String SEARCH_KNOWLEDGE = "检索健康生活知识（睡眠/运动/饮水/饮食），返回相关科普建议";
        String QUERY_SESSION_HISTORY = "manage_session 的查询子能力：查询指定会话的历史对话记录（用户提问与AI回答）";
        String GET_CURRENT_DATE = "获取当前系统日期与星期，用于推断“今天/昨天/本周”等时间表述";
    }

    interface ToolParams {
        String RECORD_DATE = "打卡日期，格式 yyyy-MM-dd，不传则默认今天";
        String SLEEP_TIME = "入睡时间，格式 HH:mm，例如 23:00";
        String WAKE_TIME = "起床时间，格式 HH:mm，例如 07:00";
        String EXERCISE_TYPE = "运动类型描述，例如 跑步/游泳/健身";
        String EXERCISE_DURATION = "运动时长，单位分钟，例如 30";
        String WATER_INTAKE = "饮水量，单位毫升，例如 2000";
        String DIET_DESC = "饮食备注，例如 早餐鸡蛋牛奶、午餐少油";
        String DIET_SCORE = "饮食评分，1-5 整数，5 表示最健康";
        String SLEEP_QUALITY = "睡眠质量评分，1-5 整数，5 表示最好";
        String MOOD = "心情评分，1-5 整数，5 表示最好";
        String REMARK = "其他备注说明";

        String DAYS = "查询最近 N 天的记录（含今天），例如 7 表示近一周";
        String START_DATE = "日期范围起点，格式 yyyy-MM-dd，与 endDate 配合使用";
        String END_DATE = "日期范围终点，格式 yyyy-MM-dd，与 startDate 配合使用";
        String QUERY_DATE = "查询指定单日，格式 yyyy-MM-dd，优先级高于范围与 days";

        String RECORD_ID = "打卡记录id（与 recordDate 二选一用于定位待删除记录）";
        String GOAL_TYPE = "目标类型：SLEEP(睡眠)/EXERCISE(运动)/WATER(饮水)/DIET(饮食)/CUSTOM(自定义)";
        String TARGET_VALUE = "目标数值，例如 8(小时)/30(分钟)/2000(毫升)";
        String UNIT = "目标单位，例如 h/min/ml/步";
        String CUSTOM_NAME = "自定义目标名称，CUSTOM 类型必填，例如 每日阅读";
        String GOAL_ID = "目标id（更新/删除目标时使用）";

        String REMINDER_TITLE = "提醒标题，例如 睡觉提醒/喝水提醒";
        String REMINDER_TIME = "提醒时间，格式 HH:mm，例如 22:00";
        String REMINDER_TYPE = "提醒类型：SLEEP/DIET/EXERCISE/WATER/CUSTOM";
        String WEEKDAYS = "重复星期，1-7 逗号分隔，默认每日(1,2,3,4,5,6,7)";
        String REMINDER_ID = "提醒id（更新/删除/开关时使用）";
        String REMINDER_ACTIVE = "是否启用提醒，true 启用 false 停用";

        String ANALYSIS_TYPE = "分析类型：TREND(趋势)/OVERVIEW(概览)/ACHIEVEMENT(达成率)/RADAR(雷达)/AI_SUMMARY(自然语言周报)";
        String KNOWLEDGE_QUERY = "健康知识检索关键词，例如 失眠怎么办/每天喝多少水";
        String KNOWLEDGE_TYPE = "知识类型：SLEEP/EXERCISE/WATER/DIET，不传则返回综合建议";
        String TOP_K = "返回知识条数，默认 3";
        String SESSION_ID = "会话id，用于查询该会话的历史对话";
    }
}
