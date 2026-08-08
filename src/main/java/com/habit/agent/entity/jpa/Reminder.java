package com.habit.agent.entity.jpa;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 打卡提醒实体（子模块 2-1）
 *
 * 支持用户自定义提醒时间和方式。
 * reminder_type: SLEEP/DIET/EXERCISE/WATER/CUSTOM
 * weekdays: 逗号分隔数字，1-7 对应周一到周日，如 "1,2,3,4,5" 表示工作日
 * 对应 MySQL 表: reminder
 */
@Entity
@Table(
    name = "reminder",
    uniqueConstraints = @UniqueConstraint(name = "uk_user_title", columnNames = {"user_id", "title"}),
    indexes = @Index(name = "idx_user_active", columnList = "user_id, is_active")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Reminder {

    /**
     * 提醒类型枚举
     */
    public enum ReminderType {
        SLEEP, DIET, EXERCISE, WATER, CUSTOM
    }

    /** 主键 id */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 所属用户 id（不可为空） */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 提醒标题（不可为空，最长 100 字符，与 user_id 构成唯一约束） */
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    /** 提醒触发时间（不可为空） */
    @Column(name = "reminder_time", nullable = false)
    private LocalTime reminderTime;

    /** 提醒类型（SLEEP/DIET/EXERCISE/WATER/CUSTOM） */
    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", nullable = false, length = 30)
    private ReminderType reminderType;

    /** 每周重复日，逗号分隔的 1-7（周一到周日），默认每天 */
    @Column(name = "weekdays", nullable = false, length = 20)
    @Builder.Default
    private String weekdays = "1,2,3,4,5,6,7";

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
    }

    @PreUpdate
    void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
