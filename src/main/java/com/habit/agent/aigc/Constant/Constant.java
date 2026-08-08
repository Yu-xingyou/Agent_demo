package com.habit.agent.aigc.Constant;

/**
 * AIGC 模块通用常量
 */
public interface Constant {

    /** 请求 id，用于向工具传递参数（幂等/追踪） */
    String REQUEST_ID = "requestId";

    /** 工具名称与参数描述 */
    interface Tools {
        String QUERY_RECENT_RECORDS = "查询用户最近 N 天的习惯打卡记录（睡眠/饮食/运动/饮水/心情）";
        String QUERY_ACTIVE_GOALS = "查询用户当前启用的习惯目标（睡眠/运动/饮水/饮食）";
        String QUERY_ACHIEVEMENT_RATE = "查询用户最近 N 天各类目标的达成率统计";
        String QUERY_REMINDERS = "查询用户当前的打卡提醒列表";
        String QUERY_TRENDS = "查询用户最近 N 天的习惯数据趋势（睡眠时长/运动/饮水/心情）";
        String QUERY_OVERVIEW = "查询用户最近 N 天的习惯数据概览统计";
    }

    interface ToolParams {
        String DAYS = "查询天数，最近 N 天，默认 7";
        String GOAL_TYPE = "目标类型：SLEEP/EXERCISE/WATER/DIET";
        String TITLE = "提醒标题";
    }
}
