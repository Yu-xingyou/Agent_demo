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

    /**
     * 录入或更新打卡记录（同一天重复打卡为更新）。
     *
     * @param record 打卡实体（含 userId/日期/各维度数据）
     * @return 保存后的打卡记录视图
     */
    HabitRecordVO saveOrUpdate(HabitRecord record);

    /**
     * 按日期范围查询记录。
     *
     * @param userId    用户 id，可空（空时使用默认用户）
     * @param startDate 开始日期（含）
     * @param endDate   结束日期（含）
     * @return 该区间打卡记录视图列表
     */
    List<HabitRecordVO> getRecordsByDateRange(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 查询最近 N 天记录。
     *
     * @param userId 用户 id，可空（空时使用默认用户）
     * @param days   天数
     * @return 最近 N 天打卡记录视图列表
     */
    List<HabitRecordVO> getRecentRecords(Long userId, int days);

    /**
     * 按日期查询记录。
     *
     * @param userId     用户 id，可空（空时使用默认用户）
     * @param recordDate 指定日期
     * @return 该日打卡记录视图；无则 null
     */
    HabitRecordVO getRecordByDate(Long userId, LocalDate recordDate);

    /**
     * 查询所有记录。
     *
     * @param userId 用户 id，可空（空时使用默认用户）
     * @return 全部打卡记录视图列表
     */
    List<HabitRecordVO> getAllRecords(Long userId);

    /**
     * 查询今日记录。
     *
     * @param userId 用户 id，可空（空时使用默认用户）
     * @return 今日打卡记录视图；无则 null
     */
    HabitRecordVO getTodayRecord(Long userId);

    /**
     * 按 ID 查询记录。
     *
     * @param id 记录 id
     * @return 打卡记录视图
     */
    HabitRecordVO getRecordById(Long id);

    /**
     * 删除记录。
     *
     * @param id 记录 id
     */
    void deleteRecord(Long id);
}
