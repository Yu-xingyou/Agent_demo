package com.habit.agent.entity.jpa;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 习惯记录实体（子模块 2-1，核心业务表）
 *
 * 每日打卡数据，同一天重复打卡为更新（uk_user_date 唯一约束）。
 * sleep_duration 由 @PrePersist/@PreUpdate 自动计算，无需手动设置。
 * 跨天睡眠处理：如果起床时间早于或等于入睡时间，加 24 小时。
 * 对应 MySQL 表: habit_record
 */
@Entity
@Table(
    name = "habit_record",
    uniqueConstraints = @UniqueConstraint(name = "uk_user_date", columnNames = {"user_id", "record_date"}),
    indexes = {
        @Index(name = "idx_user_date", columnList = "user_id, record_date"),
        @Index(name = "idx_user_recent", columnList = "user_id, record_date DESC")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class HabitRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "sleep_time")
    private LocalTime sleepTime;

    @Column(name = "wake_time")
    private LocalTime wakeTime;

    /**
     * 睡眠时长（小时），由 calculateSleepDuration() 自动计算，不要手动 set
     */
    @Column(name = "sleep_duration", precision = 4, scale = 2)
    private BigDecimal sleepDuration;

    @Column(name = "sleep_quality")
    private Integer sleepQuality;

    @Column(name = "diet_desc", length = 500)
    private String dietDesc;

    @Column(name = "diet_score")
    private Integer dietScore;

    @Column(name = "exercise_type", length = 100)
    private String exerciseType;

    @Column(name = "exercise_duration")
    private Integer exerciseDuration;

    @Column(name = "water_intake")
    private Integer waterIntake;

    @Column(name = "mood")
    private Integer mood;

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
        calculateSleepDuration();
    }

    @PreUpdate
    void onUpdate() {
        this.updateTime = LocalDateTime.now();
        calculateSleepDuration();
    }

    /**
     * 根据入睡/起床时间计算睡眠时长（小时）。
     *
     * 规则：
     * 1) sleepTime 或 wakeTime 任一为 null → sleepDuration 置 null
     * 2) wakeTime ≤ sleepTime（跨午夜）→ 加 24 小时
     * 3) 保留 2 位小数（HALF_UP），匹配 DECIMAL(4,2)
     *
     * 注意：@PrePersist/@PreUpdate 在 save() 返回后才触发，
     * Service 层需在 save() 前显式调用此方法以确保 VO 值正确。
     */
    public void calculateSleepDuration() {
        if (sleepTime == null || wakeTime == null) {
            this.sleepDuration = null;
            return;
        }

        // Duration.between 跨午夜时返回负值，加 24 小时修正
        long minutes = Duration.between(sleepTime, wakeTime).toMinutes();
        if (minutes <= 0) {
            minutes += 24 * 60; // 跨午夜：加 24 小时
        }
        this.sleepDuration = BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }
}
