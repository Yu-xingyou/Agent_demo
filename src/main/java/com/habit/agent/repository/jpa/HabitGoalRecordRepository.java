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
     */
    List<HabitGoalRecord> findByUserIdAndRecordDate(Long userId, LocalDate recordDate);

    /**
     * 按用户和日期范围查询
     */
    List<HabitGoalRecord> findByUserIdAndRecordDateBetweenOrderByRecordDateDesc(
            Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 按目标 ID 查询最近 N 天记录
     */
    List<HabitGoalRecord> findByGoalIdAndRecordDateBetweenOrderByRecordDateDesc(
            Long goalId, LocalDate startDate, LocalDate endDate);

    /**
     * 按用户、目标 ID、日期查询（用于判重）
     */
    Optional<HabitGoalRecord> findByUserIdAndGoalIdAndRecordDate(
            Long userId, Long goalId, LocalDate recordDate);

    /**
     * 按目标 ID 查询所有记录
     */
    List<HabitGoalRecord> findByGoalIdOrderByRecordDateDesc(Long goalId);
}
