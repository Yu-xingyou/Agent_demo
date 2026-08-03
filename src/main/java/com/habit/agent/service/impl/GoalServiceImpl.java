package com.habit.agent.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.common.exception.BusinessException;
import com.habit.agent.common.vo.HabitGoalVO;
import com.habit.agent.entity.jpa.HabitGoal;
import com.habit.agent.entity.jpa.HabitGoal.GoalType;
import com.habit.agent.entity.jpa.HabitRecord;
import com.habit.agent.repository.jpa.HabitGoalRepository;
import com.habit.agent.repository.jpa.HabitRecordRepository;
import com.habit.agent.service.GoalService;
import com.habit.agent.service.HabitGoalRecordService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 习惯目标业务逻辑实现
 *
 * 支持内置类型(SLEEP/EXERCISE/WATER/DIET)和自定义类型(CUSTOM)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoalServiceImpl implements GoalService {

    private final HabitGoalRepository habitGoalRepository;
    private final HabitGoalRecordService goalRecordService;
    private final HabitRecordRepository habitRecordRepository;

    /**
     * 查询用户启用的目标
     */
    @Override
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
    @Override
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
     * 查询用户所有启用目标（内置默认 + 自定义，均默认 isActive=TRUE，故一并返回）。
     * 与 getActiveGoals 语义一致，保留此方法以兼容前端"含自定义目标"的调用约定。
     */
    @Override
    @Transactional(readOnly = true)
    public List<HabitGoalVO> getActiveGoalsWithCustom(Long userId) {
        if (userId == null) userId = AgentConstants.DEFAULT_USER_ID;
        Long uid = userId;
        // 本周（近 7 天，含今天）的打卡记录，用于计算内置目标当前值
        LocalDate weekStart = LocalDate.now().minusDays(6);
        List<HabitRecord> weekRecords = habitRecordRepository
                .findByUserIdAndRecordDateBetweenOrderByRecordDateDesc(uid, weekStart, LocalDate.now());
        return habitGoalRepository
                .findByUserIdAndIsActive(uid, Boolean.TRUE)
                .stream()
                .map(g -> toVO(g, uid, weekRecords))
                .collect(Collectors.toList());
    }

    /**
     * 创建目标
     * 内置类型（SLEEP/EXERCISE/WATER/DIET）：同一类型只允许一个
     * 自定义类型（CUSTOM）：允许多个，通过 customName 区分
     */
    @Override
    @Transactional
    public HabitGoalVO saveGoal(HabitGoalVO vo) {
        if (vo == null) {
            throw new BusinessException(AgentConstants.CODE_PARAM_ERROR, "目标信息不能为空");
        }
        GoalType goalType;
        try {
            goalType = GoalType.valueOf(vo.getGoalType().trim().toUpperCase());
        } catch (Exception e) {
            throw new BusinessException(AgentConstants.CODE_ENUM_ERROR, "不支持的目标类型: " + vo.getGoalType());
        }

        HabitGoal goal = new HabitGoal();
        goal.setUserId(vo.getUserId() != null ? vo.getUserId() : AgentConstants.DEFAULT_USER_ID);
        goal.setGoalType(goalType);
        goal.setCustomName(vo.getCustomName());
        goal.setTargetValue(vo.getTargetValue());
        goal.setUnit(vo.getUnit());
        // VO 周期字符串(DAY/WEEK/MONTH) → 实体枚举；不传则留 null，由 @PrePersist 兜底为 DAILY
        goal.setPeriod(vo.getPeriod() != null ? HabitGoal.Period.fromVo(vo.getPeriod()) : null);
        goal.setIsActive(vo.getIsActive() != null ? vo.getIsActive() : Boolean.TRUE);

        // 内置类型检查重复（同一类型只允许一个）
        if (goalType != GoalType.CUSTOM) {
            boolean duplicate = habitGoalRepository
                    .findByUserId(goal.getUserId())
                    .stream()
                    .anyMatch(g -> g.getGoalType() == goalType);
            if (duplicate) {
                throw new BusinessException(AgentConstants.CODE_DUPLICATE_GOAL,
                        "该目标类型已存在: " + goalType);
            }
        }

        // 自定义类型检查重复（按 goalType + customName 判重，不同 customName 可共存多个）
        if (goalType == GoalType.CUSTOM) {
            String normalizedCustomName = normalizeCustomName(goal.getCustomName());
            boolean duplicate = habitGoalRepository
                    .findByUserId(goal.getUserId())
                    .stream()
                    .filter(g -> g.getGoalType() == GoalType.CUSTOM)
                    .anyMatch(g -> normalizeCustomName(g.getCustomName()).equals(normalizedCustomName));
            if (duplicate) {
                throw new BusinessException(AgentConstants.CODE_DUPLICATE_GOAL,
                        "该自定义目标已存在: " + (normalizedCustomName.isEmpty() ? "默认自定义" : normalizedCustomName));
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
    @Override
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
     * 按 ID 查询目标
     */
    @Override
    @Transactional(readOnly = true)
    public HabitGoalVO getGoalById(Long id) {
        HabitGoal db = habitGoalRepository.findById(id)
                .orElseThrow(() -> new BusinessException(AgentConstants.CODE_GOAL_NOT_FOUND,
                        "目标不存在: id=" + id));
        return toVO(db);
    }

    /**
     * 更新目标
     */
    @Override
    @Transactional
    public HabitGoalVO updateGoal(Long id, HabitGoalVO vo) {
        HabitGoal db = habitGoalRepository.findById(id)
                .orElseThrow(() -> new BusinessException(AgentConstants.CODE_GOAL_NOT_FOUND,
                        "目标不存在: id=" + id));

        if (vo.getCustomName() != null) {
            db.setCustomName(vo.getCustomName());
        }
        if (vo.getTargetValue() != null) {
            db.setTargetValue(vo.getTargetValue());
        }
        if (vo.getUnit() != null) {
            db.setUnit(vo.getUnit());
        }
        if (vo.getPeriod() != null) {
            db.setPeriod(HabitGoal.Period.fromVo(vo.getPeriod()));
        }
        if (vo.getIsActive() != null) {
            db.setIsActive(vo.getIsActive());
        }

        HabitGoal saved = habitGoalRepository.save(db);
        log.info("更新目标: id={}, type={}, customName={}", saved.getId(), saved.getGoalType(), saved.getCustomName());
        return toVO(saved);
    }

    /**
     * 删除目标（同时删除关联的打卡记录）
     */
    @Override
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
     * 归一化自定义目标名称：去空格，null/空串统一为空字符串，便于判重比较。
     */
    private String normalizeCustomName(String customName) {
        return customName == null ? "" : customName.trim();
    }

    private HabitGoalVO toVO(HabitGoal entity) {
        return toVO(entity, entity.getUserId(), null);
    }

    /**
     * Entity → VO 转换（含 displayName、单位计算及本周完成度）。
     *
     * @param weekRecords 本周（近 7 天）打卡记录，为 null 时跳过完成度计算（currentValue/achievement 置 null）
     */
    private HabitGoalVO toVO(HabitGoal entity, Long userId, List<HabitRecord> weekRecords) {
        String goalTypeName = entity.getGoalType() != null ? entity.getGoalType().name() : null;
        String displayName;
        if (entity.getGoalType() == GoalType.CUSTOM && entity.getCustomName() != null) {
            displayName = entity.getCustomName();
        } else {
            displayName = switch (entity.getGoalType()) {
                case SLEEP -> "睡眠目标";
                case EXERCISE -> "运动目标";
                case WATER -> "饮水目标";
                case DIET -> "饮食目标";
                case CUSTOM -> entity.getCustomName() != null ? entity.getCustomName() : "自定义目标";
                default -> goalTypeName;
            };
        }
        String unit = entity.getUnit();
        if (entity.getGoalType() != null && entity.getGoalType() != GoalType.CUSTOM) {
            unit = switch (entity.getGoalType()) {
                case SLEEP -> "h";
                case EXERCISE -> "min";
                case WATER -> "ml";
                case DIET -> "/5";
                default -> unit;
            };
        }

        BigDecimal currentValue = null;
        BigDecimal weeklyAchievement = null;
        if (entity.getTargetValue() != null && entity.getTargetValue().signum() > 0) {
            if (entity.getGoalType() == GoalType.CUSTOM) {
                // 自定义目标：本周打卡记录均值
                BigDecimal avg = goalRecordService.calcAverage(entity.getId(), 7);
                currentValue = avg != null ? BigDecimal.valueOf(round2(avg.doubleValue())) : BigDecimal.ZERO;
            } else if (weekRecords != null) {
                // 内置目标：本周聚合（睡眠累加小时数，运动/饮水累加总量，饮食取平均分）
                currentValue = calcBuiltinWeekly(entity.getGoalType(), weekRecords);
            }
            if (currentValue != null) {
                weeklyAchievement = currentValue
                        .multiply(BigDecimal.valueOf(100))
                        .divide(entity.getTargetValue(), 1, RoundingMode.HALF_UP);
            }
        }

        return HabitGoalVO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .goalType(goalTypeName)
                .customName(entity.getCustomName())
                .displayName(displayName)
                .targetValue(entity.getTargetValue())
                .unit(unit)
                .period(entity.getPeriod() != null ? entity.getPeriod().toVo() : null)
                .isActive(entity.getIsActive())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .currentValue(currentValue)
                .weeklyAchievement(weeklyAchievement)
                .build();
    }

    /**
     * 计算内置目标本周日均。
     * SLEEP/EXERCISE/WATER 为本周求和后除以有效天数；DIET 为本周平均分。
     */
    private BigDecimal calcBuiltinWeekly(GoalType type, List<HabitRecord> weekRecords) {
        if (type == GoalType.DIET) {
            List<Integer> scores = weekRecords.stream()
                    .map(HabitRecord::getDietScore)
                    .filter(s -> s != null)
                    .toList();
            if (scores.isEmpty()) return BigDecimal.ZERO;
            double sum = scores.stream().mapToInt(Integer::intValue).sum();
            return BigDecimal.valueOf(round2(sum / scores.size()));
        }
        BigDecimal sum = BigDecimal.ZERO;
        int days = 0;
        for (HabitRecord r : weekRecords) {
            BigDecimal v = switch (type) {
                case SLEEP -> r.getSleepDuration();
                case EXERCISE -> r.getExerciseDuration() != null ? BigDecimal.valueOf(r.getExerciseDuration()) : null;
                case WATER -> r.getWaterIntake() != null ? BigDecimal.valueOf(r.getWaterIntake()) : null;
                default -> null;
            };
            if (v != null) { sum = sum.add(v); days++; }
        }
        if (days == 0) return BigDecimal.ZERO;
        return round2(sum.divide(BigDecimal.valueOf(days), 4, RoundingMode.HALF_UP));
    }

    private double round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private BigDecimal round2(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
