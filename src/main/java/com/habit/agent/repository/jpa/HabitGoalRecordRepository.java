package com.habit.agent.repository.jpa;

import com.habit.agent.entity.jpa.HabitGoalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 自定义目标打卡记录 Repository
 */
@Repository
public interface HabitGoalRecordRepository extends JpaRepository<HabitGoalRecord, Long> {

    /**
     * 按用户和日期查询所有自定义目标记录
     *
     * @param userId     用户 id
     * @param recordDate 指定日期
     * @return 该日自定义目标打卡记录实体列表
     */
    List<HabitGoalRecord> findByUserIdAndRecordDate(Long userId, LocalDate recordDate);

    /**
     * 按用户和日期范围查询
     *
     * @param userId    用户 id
     * @param startDate 开始日期（含）
     * @param endDate   结束日期（含）
     * @return 该区间自定义目标打卡记录实体列表（倒序）
     */
    List<HabitGoalRecord> findByUserIdAndRecordDateBetweenOrderByRecordDateDesc(
            Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 按目标 ID 查询指定日期范围记录（倒序，用于趋势聚合）
     *
     * @param goalId    目标 id
     * @param startDate 开始日期（含）
     * @param endDate   结束日期（含）
     * @return 该目标区间打卡记录实体列表（倒序）
     */
    List<HabitGoalRecord> findByGoalIdAndRecordDateBetweenOrderByRecordDateDesc(
            Long goalId, LocalDate startDate, LocalDate endDate);

    /**
     * 按用户、目标 ID、日期查询（用于判重）
     *
     * @param userId     用户 id
     * @param goalId     目标 id
     * @param recordDate 指定日期
     * @return 命中的记录；不存在时返回空 Optional
     */
    Optional<HabitGoalRecord> findByUserIdAndGoalIdAndRecordDate(
            Long userId, Long goalId, LocalDate recordDate);

    /**
     * 按目标 ID 查询所有记录（倒序）
     *
     * @param goalId 目标 id
     * @return 该目标全部打卡记录实体列表（倒序）
     */
    List<HabitGoalRecord> findByGoalIdOrderByRecordDateDesc(Long goalId);
}
