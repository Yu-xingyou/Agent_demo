package com.habit.agent.entity.jpa;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 习惯目标实体（支持内置类型 + 自定义类型）
 *
 * 内置类型: SLEEP(睡眠) / EXERCISE(运动) / WATER(饮水) / DIET(饮食)
 * 自定义类型: CUSTOM — 通过 custom_name 字段存储自定义目标的显示名
 * 对应 MySQL 表: habit_goal
 */
@Entity
@Table(
    name = "habit_goal",
    indexes = @Index(name = "idx_user_active", columnList = "user_id, is_active")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class HabitGoal {

    /**
     * 目标类型枚举（含 CUSTOM 自定义类型）
     */
    public enum GoalType {
        SLEEP, EXERCISE, WATER, DIET, CUSTOM
    }

    /**
     * 目标周期枚举
     */
    public enum Period {
        DAILY, WEEKLY, MONTHLY;

        /**
         * VO 字符串(DAY/WEEK/MONTH) → 枚举。前端用简写，库内用完整名。
         */
        public static Period fromVo(String vo) {
            if (vo == null) return null;
            return switch (vo.trim().toUpperCase()) {
                case "DAY", "DAILY" -> DAILY;
                case "WEEK", "WEEKLY" -> WEEKLY;
                case "MONTH", "MONTHLY" -> MONTHLY;
                default -> throw new IllegalArgumentException("不支持的目标周期: " + vo);
            };
        }

        /**
         * 枚举 → VO 字符串(返回简写 DAY/WEEK/MONTH)
         */
        public String toVo() {
            return switch (this) {
                case DAILY -> "DAY";
                case WEEKLY -> "WEEK";
                case MONTHLY -> "MONTH";
            };
        }
    }

    /** 主键 id */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 所属用户 id */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 目标类型（SLEEP/EXERCISE/WATER/DIET/CUSTOM） */
    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", nullable = false, length = 30)
    private GoalType goalType;

    /**
     * 自定义目标的显示名称（仅当 goalType=CUSTOM 时使用）
     * 例如："冥想"、"阅读"、"练字"
     */
    @Column(name = "custom_name", length = 100)
    private String customName;

    /** 目标值（不可为空，如睡眠 8 小时、饮水 2000 毫升） */
    @Column(name = "target_value", nullable = false, precision = 8, scale = 2)
    private BigDecimal targetValue;

    /** 目标计量单位（如 小时/毫升/次） */
    @Column(name = "unit", length = 20)
    private String unit;

    /** 目标周期（DAILY/WEEKLY/MONTHLY，默认 DAILY） */
    @Enumerated(EnumType.STRING)
    @Column(name = "period", nullable = false, length = 20)
    @Builder.Default
    private Period period = Period.DAILY;

    /** 是否启用（默认 true） */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = Boolean.TRUE;

    /** 创建时间（插入时自动写入，不可更新） */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间（每次更新自动刷新） */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    void onCreate() {
        this.createTime = LocalDateTime.now();
        this.updateTime = this.createTime;
        // 兜底默认值：JSON 反序列化（无 builder）不会触发 @Builder.Default，
        // 若不补默认值，period/isActive 可能为 null 导致非空列插入失败。
        if (this.period == null) {
            this.period = Period.DAILY;
        }
        if (this.isActive == null) {
            this.isActive = Boolean.TRUE;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updateTime = LocalDateTime.now();
        if (this.period == null) {
            this.period = Period.DAILY;
        }
        if (this.isActive == null) {
            this.isActive = Boolean.TRUE;
        }
    }

    /**
     * 获取目标显示名（CUSTOM 类型使用 customName，其他使用枚举名）
     */
    @Transient
    public String getDisplayName() {
        if (goalType == GoalType.CUSTOM && customName != null) {
            return customName;
        }
        return goalType != null ? goalType.name() : "";
    }
}
