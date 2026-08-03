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

    /** 录入或更新单个自定义目标打卡记录。 */
    HabitGoalRecordVO saveOrUpdate(HabitGoalRecord record);

    /** 按用户和日期查询所有自定义目标记录。 */
    List<HabitGoalRecordVO> getByDate(Long userId, LocalDate recordDate);

    /** 按用户和精确日期范围查询自定义目标记录（升序）。 */
    List<HabitGoalRecordVO> getByDateRange(Long userId, LocalDate startDate, LocalDate endDate);

    /** 查询最近 N 天的自定义目标记录。 */
    List<HabitGoalRecordVO> getRecent(Long userId, int days);

    /** 按目标 ID 查询最近 N 天记录。 */
    List<HabitGoalRecordVO> getByGoalIdRecent(Long goalId, int days);

    /** 计算某目标最近 N 天的平均值。 */
    BigDecimal calcAverage(Long goalId, int days);

    /** 删除目标相关的所有记录。 */
    void deleteByGoalId(Long goalId);
}
