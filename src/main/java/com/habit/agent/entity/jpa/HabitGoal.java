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
import jakarta.persistence.UniqueConstraint;
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
    uniqueConstraints = @UniqueConstraint(name = "uk_user_type", columnNames = {"user_id", "goal_type"}),
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
        DAILY, WEEKLY, MONTHLY
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", nullable = false, length = 30)
    private GoalType goalType;

    /**
     * 自定义目标的显示名称（仅当 goalType=CUSTOM 时使用）
     * 例如："冥想"、"阅读"、"练字"
     */
    @Column(name = "custom_name", length = 100)
    private String customName;

    @Column(name = "target_value", nullable = false, precision = 8, scale = 2)
    private BigDecimal targetValue;

    @Column(name = "unit", length = 20)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "period", nullable = false, length = 20)
    @Builder.Default
    private Period period = Period.DAILY;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = Boolean.TRUE;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    void onCreate() {
        this.createTime = LocalDateTime.now();
        this.updateTime = this.createTime;
    }

    @PreUpdate
    void onUpdate() {
        this.updateTime = LocalDateTime.now();
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
