-- ============================================
-- 生活习惯助手 Agent - 数据库建表脚本
-- 数据库: habit_agent
-- 字符集: utf8mb4
-- ============================================

CREATE DATABASE IF NOT EXISTS `habit_agent` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `habit_agent`;

-- 用户表（简化，无复杂权限，支持单用户演示或多用户隔离）
CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT PRIMARY KEY AUTO_INCREMENT,
    `username`    VARCHAR(50)  NOT NULL UNIQUE,
    `nickname`    VARCHAR(50),
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 习惯记录表（核心业务表，每日打卡数据）
CREATE TABLE IF NOT EXISTS `habit_record` (
    `id`                BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id`           BIGINT       NOT NULL,
    `record_date`       DATE         NOT NULL COMMENT '记录日期',
    `sleep_time`        TIME         NULL     COMMENT '入睡时间',
    `wake_time`         TIME         NULL     COMMENT '起床时间',
    `sleep_duration`    DECIMAL(4,2) NULL     COMMENT '睡眠时长(小时)',
    `sleep_quality`     TINYINT      NULL     COMMENT '睡眠质量 1-5',
    `diet_desc`         VARCHAR(500) NULL     COMMENT '饮食情况描述',
    `diet_score`        TINYINT      NULL     COMMENT '饮食健康评分 1-5',
    `exercise_type`     VARCHAR(100) NULL     COMMENT '运动类型',
    `exercise_duration` INT          NULL     COMMENT '运动时长(分钟)',
    `water_intake`      INT          NULL     COMMENT '饮水量(ml)',
    `mood`              TINYINT      NULL     COMMENT '心情 1-5',
    `remark`            VARCHAR(500) NULL     COMMENT '备注',
    `create_time`       DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_date` (`user_id`, `record_date`),
    INDEX `idx_user_date` (`user_id`, `record_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 习惯目标表（自定义目标 + 达成率统计）
CREATE TABLE IF NOT EXISTS `habit_goal` (
    `id`            BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id`       BIGINT      NOT NULL,
    `goal_type`     VARCHAR(30) NOT NULL COMMENT 'SLEEP/EXERCISE/WATER/DIET',
    `target_value`  DECIMAL(8,2) NOT NULL COMMENT '目标值',
    `unit`          VARCHAR(20) NULL     COMMENT '单位(小时/分钟/ml)',
    `period`        VARCHAR(20) DEFAULT 'DAILY' COMMENT 'DAILY/WEEKLY/MONTHLY',
    `is_active`     TINYINT DEFAULT 1,
    `create_time`   DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- AI 分析结果表（缓存 AI 输出，避免重复调用）
CREATE TABLE IF NOT EXISTS `ai_analysis` (
    `id`            BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id`       BIGINT      NOT NULL,
    `analysis_type` VARCHAR(20) NOT NULL COMMENT 'DAILY/WEEKLY/MONTHLY/CUSTOM',
    `period_start`  DATE        NOT NULL,
    `period_end`    DATE        NOT NULL,
    `content`       TEXT        NOT NULL COMMENT 'AI 生成的分析内容',
    `suggestion`    TEXT        NULL     COMMENT '改善建议',
    `risk_warning`  TEXT        NULL     COMMENT '风险提示',
    `create_time`   DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_type_period` (`user_id`, `analysis_type`, `period_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 对话消息表（对话历史持久化备份，Redis 为主存储）
CREATE TABLE IF NOT EXISTS `chat_message` (
    `id`              BIGINT PRIMARY KEY AUTO_INCREMENT,
    `conversation_id` VARCHAR(64) NOT NULL COMMENT '会话ID (对应 Redis ChatMemory)',
    `user_id`         BIGINT      NOT NULL,
    `role`            VARCHAR(20) NOT NULL COMMENT 'USER/ASSISTANT/SYSTEM/TOOL',
    `content`         TEXT        NOT NULL,
    `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_conversation` (`conversation_id`),
    INDEX `idx_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 初始化默认用户和示例数据（供演示用）
-- ============================================

-- 默认用户
INSERT INTO `user` (`id`, `username`, `nickname`) VALUES (1, 'demo', '演示用户')
ON DUPLICATE KEY UPDATE `nickname` = '演示用户';

-- 默认习惯目标
INSERT INTO `habit_goal` (`user_id`, `goal_type`, `target_value`, `unit`, `period`) VALUES
(1, 'SLEEP', 8.00, '小时', 'DAILY'),
(1, 'EXERCISE', 30.00, '分钟', 'DAILY'),
(1, 'WATER', 2000.00, 'ml', 'DAILY'),
(1, 'DIET', 4.00, '分', 'DAILY')
ON DUPLICATE KEY UPDATE `is_active` = 1;
