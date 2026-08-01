package com.habit.agent.repository.jpa;

import com.habit.agent.entity.jpa.HabitRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 习惯记录 Repository（子模块 2-1）
 */
@Repository
public interface HabitRecordRepository extends JpaRepository<HabitRecord, Long> {

    /**
     * 按日期范围查询用户记录（按日期倒序）
     */
    List<HabitRecord> findByUserIdAndRecordDateBetweenOrderByRecordDateDesc(
            Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 按日期查询用户记录
     */
    Optional<HabitRecord> findByUserIdAndRecordDate(Long userId, LocalDate recordDate);

    /**
     * 查询用户所有记录（按日期倒序）
     */
    List<HabitRecord> findByUserIdOrderByRecordDateDesc(Long userId);
}
