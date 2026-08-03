package com.habit.agent.service;

import java.util.List;

import com.habit.agent.common.vo.HabitGoalVO;
import com.habit.agent.entity.jpa.HabitGoal.GoalType;

/**
 * 习惯目标业务逻辑接口。
 *
 * 支持内置类型(SLEEP/EXERCISE/WATER/DIET)和自定义类型(CUSTOM)。
 */
public interface GoalService {

    /** 查询用户启用的目标。 */
    List<HabitGoalVO> getActiveGoals(Long userId);

    /** 查询用户所有目标。 */
    List<HabitGoalVO> getAllGoals(Long userId);

    /** 查询用户所有启用目标（内置默认 + 自定义，含本周完成度）。 */
    List<HabitGoalVO> getActiveGoalsWithCustom(Long userId);

    /** 创建目标（内置类型同一类型只允许一个，自定义类型允许多个）。 */
    HabitGoalVO saveGoal(HabitGoalVO vo);

    /** 按类型查询目标。 */
    HabitGoalVO getGoalByType(Long userId, GoalType goalType);

    /** 按 ID 查询目标。 */
    HabitGoalVO getGoalById(Long id);

    /** 更新目标。 */
    HabitGoalVO updateGoal(Long id, HabitGoalVO vo);

    /** 删除目标（同时删除关联的打卡记录）。 */
    void deleteGoal(Long id);
}
