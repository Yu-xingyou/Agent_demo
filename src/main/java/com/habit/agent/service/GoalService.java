package com.habit.agent.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.common.exception.BusinessException;
import com.habit.agent.common.vo.HabitGoalVO;
import com.habit.agent.entity.jpa.HabitGoal;
import com.habit.agent.entity.jpa.HabitGoal.GoalType;
import com.habit.agent.repository.jpa.HabitGoalRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 习惯目标业务逻辑
 *
 * 支持内置类型(SLEEP/EXERCISE/WATER/DIET)和自定义类型(CUSTOM)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoalService {

    private final HabitGoalRepository habitGoalRepository;
    private final HabitGoalRecordService goalRecordService;

    /**
     * 查询用户启用的目标
     */
    @Transactional(readOnly = true)
    public List<HabitGoalVO> getActiveGoals(Long userId) {
        if (userId == null) userId = AgentConstants.DEFAULT_USER_ID;
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
        if (userId == null) userId = AgentConstants.DEFAULT_USER_ID;
        return habitGoalRepository
                .findByUserId(userId)
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 查询用户所有启用目标（含内置默认 + 用户自定义）
     */
    @Transactional(readOnly = true)
    public List<HabitGoalVO> getActiveGoalsWithCustom(Long userId) {
        if (userId == null) userId = AgentConstants.DEFAULT_USER_ID;
        return habitGoalRepository
                .findByUserIdAndIsActive(userId, Boolean.TRUE)
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 创建目标
     * 内置类型（SLEEP/EXERCISE/WATER/DIET）：同一类型只允许一个
     * 自定义类型（CUSTOM）：允许多个，通过 customName 区分
     */
    @Transactional
    public HabitGoalVO saveGoal(HabitGoal goal) {
        if (goal.getUserId() == null) {
            goal.setUserId(AgentConstants.DEFAULT_USER_ID);
        }

        // 内置类型检查重复
        if (goal.getGoalType() != null && goal.getGoalType() != GoalType.CUSTOM) {
            Optional<HabitGoal> existing = habitGoalRepository
                    .findByUserId(goal.getUserId())
                    .stream()
                    .filter(g -> g.getGoalType() == goal.getGoalType())
                    .findFirst();
            if (existing.isPresent()) {
                throw new BusinessException(AgentConstants.CODE_DUPLICATE_GOAL,
                        "该目标类型已存在: " + goal.getGoalType());
            }
        }

        HabitGoal saved = habitGoalRepository.save(goal);
        log.info("创建目标: userId={}, type={}, customName={}",
                saved.getUserId(), saved.getGoalType(), saved.getCustomName());
        return toVO(saved);
    }

    /**
     * 按类型查询目标
     */
    @Transactional(readOnly = true)
    public HabitGoalVO getGoalByType(Long userId, GoalType goalType) {
        if (userId == null) userId = AgentConstants.DEFAULT_USER_ID;
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

        if (goalUpdate.getCustomName() != null) {
            db.setCustomName(goalUpdate.getCustomName());
        }
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
        log.info("更新目标: id={}, type={}, customName={}", saved.getId(), saved.getGoalType(), saved.getCustomName());
        return toVO(saved);
    }

    /**
     * 删除目标（同时删除关联的打卡记录）
     */
    @Transactional
    public void deleteGoal(Long id) {
        if (!habitGoalRepository.existsById(id)) {
            throw new BusinessException(AgentConstants.CODE_GOAL_NOT_FOUND,
                    "目标不存在: id=" + id);
        }
        // 删除关联的打卡记录
        goalRecordService.deleteByGoalId(id);
        habitGoalRepository.deleteById(id);
        log.info("删除目标: id={}", id);
    }

    /**
     * Entity → VO 转换（含 displayName 计算）
     */
    private HabitGoalVO toVO(HabitGoal entity) {
        String goalTypeName = entity.getGoalType() != null ? entity.getGoalType().name() : null;
        String displayName;
        if (entity.getGoalType() == GoalType.CUSTOM && entity.getCustomName() != null) {
            displayName = entity.getCustomName();
        } else {
            displayName = goalTypeName;
        }
        return HabitGoalVO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .goalType(goalTypeName)
                .customName(entity.getCustomName())
                .displayName(displayName)
                .targetValue(entity.getTargetValue())
                .unit(entity.getUnit())
                .period(entity.getPeriod() != null ? entity.getPeriod().name() : null)
                .isActive(entity.getIsActive())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }
}
