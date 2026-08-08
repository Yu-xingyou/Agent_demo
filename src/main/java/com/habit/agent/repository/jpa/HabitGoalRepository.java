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
     *
     * @param userId   用户 id
     * @param isActive 启用状态
     * @return 该用户指定启用状态的目标实体列表
     */
    List<HabitGoal> findByUserIdAndIsActive(Long userId, Boolean isActive);

    /**
     * 查询用户所有目标
     *
     * @param userId 用户 id
     * @return 该用户全部目标实体列表
     */
    List<HabitGoal> findByUserId(Long userId);
}
