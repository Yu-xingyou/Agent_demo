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
     *
     * @param userId    用户 id
     * @param startDate 开始日期（含）
     * @param endDate   结束日期（含）
     * @return 该区间打卡记录实体列表（倒序）
     */
    List<HabitRecord> findByUserIdAndRecordDateBetweenOrderByRecordDateDesc(
            Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * 按日期查询用户记录
     *
     * @param userId      用户 id
     * @param recordDate  指定日期
     * @return 命中的打卡记录；不存在时返回空 Optional
     */
    Optional<HabitRecord> findByUserIdAndRecordDate(Long userId, LocalDate recordDate);

    /**
     * 查询用户所有记录（按日期倒序）
     *
     * @param userId 用户 id
     * @return 该用户全部打卡记录实体列表（倒序）
     */
    List<HabitRecord> findByUserIdOrderByRecordDateDesc(Long userId);
}
