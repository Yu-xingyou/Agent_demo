package com.habit.agent.service;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.common.exception.BusinessException;
import com.habit.agent.common.vo.HabitGoalVO;
import com.habit.agent.entity.jpa.HabitGoal;
import com.habit.agent.repository.jpa.HabitGoalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 习惯目标业务逻辑（子模块 2-2）
 *
 * 每种目标类型（SLEEP/EXERCISE/WATER/DIET）同一用户仅能有一个活跃目标。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoalService {

    private final HabitGoalRepository habitGoalRepository;

    /**
     * 查询用户启用的目标
     */
    @Transactional(readOnly = true)
    public List<HabitGoalVO> getActiveGoals(Long userId) {
        if (userId == null) {
            userId = AgentConstants.DEFAULT_USER_ID;
        }
        return habitGoalRepository
                .findByUserIdAndIsActive(userId, Boolean.TRUE)
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 查询用户所有目标
     */
    @Transactional(readOnly = true)
    public List<HabitGoalVO> getAllGoals(Long userId) {
        if (userId == null) {
            userId = AgentConstants.DEFAULT_USER_ID;
        }
        return habitGoalRepository
                .findByUserId(userId)
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 创建目标（同类型重复则报错）
     */
    @Transactional
    public HabitGoalVO saveGoal(HabitGoal goal) {
        if (goal.getUserId() == null) {
            goal.setUserId(AgentConstants.DEFAULT_USER_ID);
        }
        // 检查同类型是否已有目标
        Optional<HabitGoal> existing = habitGoalRepository
                .findByUserId(goal.getUserId())
                .stream()
                .filter(g -> g.getGoalType() == goal.getGoalType())
                .findFirst();
        if (existing.isPresent()) {
            throw new BusinessException(AgentConstants.CODE_DUPLICATE_GOAL,
                    "该目标类型已存在: " + goal.getGoalType());
        }

        HabitGoal saved = habitGoalRepository.save(goal);
        log.info("创建目标: userId={}, type={}", saved.getUserId(), saved.getGoalType());
        return toVO(saved);
    }

    /**
     * 按类型查询目标
     */
    @Transactional(readOnly = true)
    public HabitGoalVO getGoalByType(Long userId, HabitGoal.GoalType goalType) {
        if (userId == null) {
            userId = AgentConstants.DEFAULT_USER_ID;
        }
        return habitGoalRepository
                .findByUserId(userId)
                .stream()
                .filter(g -> g.getGoalType() == goalType)
                .findFirst()
                .map(this::toVO)
                .orElseThrow(() -> new BusinessException(AgentConstants.CODE_GOAL_NOT_FOUND,
                        "目标不存在: type=" + goalType));
    }

    /**
     * 更新目标
     */
    @Transactional
    public HabitGoalVO updateGoal(Long id, HabitGoal goalUpdate) {
        HabitGoal db = habitGoalRepository.findById(id)
                .orElseThrow(() -> new BusinessException(AgentConstants.CODE_GOAL_NOT_FOUND,
                        "目标不存在: id=" + id));

        if (goalUpdate.getTargetValue() != null) {
            db.setTargetValue(goalUpdate.getTargetValue());
        }
        if (goalUpdate.getUnit() != null) {
            db.setUnit(goalUpdate.getUnit());
        }
        if (goalUpdate.getPeriod() != null) {
            db.setPeriod(goalUpdate.getPeriod());
        }
        if (goalUpdate.getIsActive() != null) {
            db.setIsActive(goalUpdate.getIsActive());
        }

        HabitGoal saved = habitGoalRepository.save(db);
        log.info("更新目标: id={}, type={}", saved.getId(), saved.getGoalType());
        return toVO(saved);
    }

    /**
     * 删除目标
     */
    @Transactional
    public void deleteGoal(Long id) {
        if (!habitGoalRepository.existsById(id)) {
            throw new BusinessException(AgentConstants.CODE_GOAL_NOT_FOUND,
                    "目标不存在: id=" + id);
        }
        habitGoalRepository.deleteById(id);
        log.info("删除目标: id={}", id);
    }

    /**
     * Entity → VO 转换
     */
    private HabitGoalVO toVO(HabitGoal entity) {
        return HabitGoalVO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .goalType(entity.getGoalType() != null ? entity.getGoalType().name() : null)
                .targetValue(entity.getTargetValue())
                .unit(entity.getUnit())
                .period(entity.getPeriod() != null ? entity.getPeriod().name() : null)
                .isActive(entity.getIsActive())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}
