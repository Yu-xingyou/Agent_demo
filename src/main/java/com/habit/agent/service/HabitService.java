package com.habit.agent.service;

import java.time.LocalDate;
import java.util.List;

import com.habit.agent.common.vo.HabitRecordVO;
import com.habit.agent.entity.jpa.HabitRecord;

/**
 * 习惯打卡记录业务逻辑接口（子模块 2-2）。
 *
 * saveOrUpdate 实现同一天重复打卡为更新（依赖 uk_user_date 唯一约束）。
 */
public interface HabitService {

    /** 录入或更新打卡记录（同一天重复打卡为更新）。 */
    HabitRecordVO saveOrUpdate(HabitRecord record);

    /** 按日期范围查询记录。 */
    List<HabitRecordVO> getRecordsByDateRange(Long userId, LocalDate startDate, LocalDate endDate);

    /** 查询最近 N 天记录。 */
    List<HabitRecordVO> getRecentRecords(Long userId, int days);

    /** 按日期查询记录。 */
    HabitRecordVO getRecordByDate(Long userId, LocalDate recordDate);

    /** 查询所有记录。 */
    List<HabitRecordVO> getAllRecords(Long userId);

    /** 查询今日记录。 */
    HabitRecordVO getTodayRecord(Long userId);

    /** 按 ID 查询记录。 */
    HabitRecordVO getRecordById(Long id);

    /** 删除记录。 */
    void deleteRecord(Long id);
}
