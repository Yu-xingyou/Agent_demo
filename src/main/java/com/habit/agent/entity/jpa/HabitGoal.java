package com.habit.agent.entity.jpa;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 习惯目标实体（子模块 2-1）
 *
 * 四种目标类型: SLEEP(睡眠) / EXERCISE(运动) / WATER(饮水) / DIET(饮食)
 * 用于达成率统计和雷达图展示。
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
     * 目标类型枚举
     */
    public enum GoalType {
        SLEEP, EXERCISE, WATER, DIET
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
}
