package com.habit.agent.entity.jpa;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 自定义目标打卡记录实体
 *
 * 存储每日自定义目标的打卡数值（CUSTOM 类型目标）
 * 对应 MySQL 表: habit_goal_record
 */
@Entity
@Table(
    name = "habit_goal_record",
    uniqueConstraints = @UniqueConstraint(name = "uk_user_goal_date", columnNames = {"user_id", "goal_id", "record_date"}),
    indexes = {
        @Index(name = "idx_goal_date", columnList = "goal_id, record_date"),
        @Index(name = "idx_user_date", columnList = "user_id, record_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class HabitGoalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "goal_id", nullable = false)
    private Long goalId;

    @Column(name = "goal_type", length = 30)
    private String goalType;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    /** 打卡数值 */
    @Column(name = "value", precision = 10, scale = 2)
    private BigDecimal value;

    @Column(name = "remark", length = 500)
    private String remark;

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
