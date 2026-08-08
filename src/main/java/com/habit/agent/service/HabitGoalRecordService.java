package com.habit.agent.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.habit.agent.common.vo.HabitGoalRecordVO;
import com.habit.agent.entity.jpa.HabitGoalRecord;

/**
 * 自定义目标打卡记录业务逻辑接口。
 */
public interface HabitGoalRecordService {

    /**
     * 录入或更新单个自定义目标打卡记录。
     *
     * @param record 自定义目标打卡实体
     * @return 保存后的打卡记录视图
     */
    HabitGoalRecordVO saveOrUpdate(HabitGoalRecord record);

    /**
     * 按用户和日期查询所有自定义目标记录。
     *
     * @param userId     用户 id，可空（空时使用默认用户）
     * @param recordDate 指定日期
     * @return 该日自定义目标打卡记录视图列表
     */
    List<HabitGoalRecordVO> getByDate(Long userId, LocalDate recordDate);

    /**
     * 按用户和精确日期范围查询自定义目标记录（升序）。
     *
     * @param userId    用户 id，可空（空时使用默认用户）
     * @param startDate 开始日期（含）
     * @param endDate   结束日期（含）
     * @return 该区间打卡记录视图列表（升序）
     */
    List<HabitGoalRecordVO> getByDateRange(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 查询最近 N 天的自定义目标记录。
     *
     * @param userId 用户 id，可空（空时使用默认用户）
     * @param days   天数
     * @return 最近 N 天打卡记录视图列表
     */
    List<HabitGoalRecordVO> getRecent(Long userId, int days);

    /**
     * 按目标 ID 查询最近 N 天记录。
     *
     * @param goalId 目标 id
     * @param days   天数
     * @return 该目标最近 N 天打卡记录视图列表
     */
    List<HabitGoalRecordVO> getByGoalIdRecent(Long goalId, int days);

    /**
     * 计算某目标最近 N 天的平均值。
     *
     * @param goalId 目标 id
     * @param days   天数
     * @return 最近 N 天的平均值；样本不足时返回零值
     */
    BigDecimal calcAverage(Long goalId, int days);

    /**
     * 删除目标相关的所有记录。
     *
     * @param goalId 目标 id
     */
    void deleteByGoalId(Long goalId);
}
