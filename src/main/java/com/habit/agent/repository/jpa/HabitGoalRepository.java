package com.habit.agent.repository.jpa;

import com.habit.agent.entity.jpa.HabitGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 习惯目标 Repository（子模块 2-1）
 */
@Repository
public interface HabitGoalRepository extends JpaRepository<HabitGoal, Long> {

    /**
     * 查询用户启用的目标
     */
    List<HabitGoal> findByUserIdAndIsActive(Long userId, Boolean isActive);

    /**
     * 查询用户所有目标
     */
    List<HabitGoal> findByUserId(Long userId);
}
