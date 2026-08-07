-- =============================================================
-- 黑马程序员 · 天机学堂风格 — 生活习惯助手 Agent 数据库脚本
-- 项目名称 : habit_agent
-- 数据库   : MySQL 8.0
-- 字符集   : utf8mb4 / utf8mb4_general_ci
-- 存储引擎 : InnoDB
-- 说明     : 关系型业务数据存放 MySQL；AI 对话/向量数据存放 MongoDB
-- 创建人   : developer
-- 创建时间 : 2026-07-30
-- =============================================================

-- 1. 建库（如已存在先删除，仅开发/初始化使用）
DROP DATABASE IF EXISTS `habit_agent`;
CREATE DATABASE `habit_agent` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `habit_agent`;

-- 2. 用户表
CREATE TABLE `user`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username`    VARCHAR(50)  NOT NULL COMMENT '登录用户名',
    `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT '用户昵称',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除（0-未删除，1-已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户表';

-- 3. 习惯记录表
CREATE TABLE `habit_record`
(
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`         BIGINT      NOT NULL COMMENT '用户ID',
    `record_date`     DATE        NOT NULL COMMENT '记录日期',
    `sleep_minutes`   INT         DEFAULT NULL COMMENT '睡眠时长（分钟）',
    `water_ml`        INT         DEFAULT NULL COMMENT '饮水（毫升）',
    `exercise_minutes` INT        DEFAULT NULL COMMENT '运动（分钟）',
    `mood`            TINYINT     DEFAULT NULL COMMENT '心情（1-5）',
    `note`            VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '逻辑删除（0-未删除，1-已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_date` (`user_id`, `record_date`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '习惯记录表';

-- 4. 习惯目标表
CREATE TABLE `habit_goal`
(
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`       BIGINT        NOT NULL COMMENT '用户ID',
    `type`          VARCHAR(20)   NOT NULL COMMENT '目标类型（SLEEP/WATER/EXERCISE/DIET/MOOD）',
    `target_value`  DECIMAL(10, 2) NOT NULL COMMENT '目标值',
    `current_value` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '当前值',
    `unit`          VARCHAR(10)   DEFAULT NULL COMMENT '单位',
    `status`        TINYINT       NOT NULL DEFAULT 1 COMMENT '状态（1-激活，0-停用）',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`    TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除（0-未删除，1-已删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_type` (`user_id`, `type`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '习惯目标表';

-- 5. 提醒表
CREATE TABLE `reminder`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`      BIGINT       NOT NULL COMMENT '用户ID',
    `title`        VARCHAR(100) NOT NULL COMMENT '提醒标题',
    `remind_time`  TIME         NOT NULL COMMENT '提醒时间',
    `repeat_type`  VARCHAR(20)  NOT NULL DEFAULT 'DAILY' COMMENT '重复类型（DAILY/WEEKLY/ONCE）',
    `enabled`      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用（1-启用，0-停用）',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除（0-未删除，1-已删除）',
    PRIMARY KEY (`id`),
    KEY `idx_user_enabled` (`user_id`, `enabled`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '提醒表';

-- 6. 初始化数据（幂等：依赖各表唯一键，重复执行不会报错）
INSERT INTO `user` (`id`, `username`, `nickname`)
VALUES (1, 'testuser', '测试用户')
ON DUPLICATE KEY UPDATE `nickname` = VALUES(`nickname`);

INSERT INTO `habit_record` (`id`, `user_id`, `record_date`, `sleep_minutes`, `water_ml`, `exercise_minutes`, `mood`, `note`)
VALUES (1, 1, '2026-08-01', 450, 2000, 30, 4, '早起')
ON DUPLICATE KEY UPDATE `sleep_minutes` = VALUES(`sleep_minutes`);

INSERT INTO `habit_goal` (`id`, `user_id`, `type`, `target_value`, `current_value`, `unit`)
VALUES (1, 1, 'SLEEP', 480.00, 450.00, 'min'),
       (2, 1, 'WATER', 2000.00, 1500.00, 'ml'),
       (3, 1, 'EXERCISE', 30.00, 20.00, 'min')
ON DUPLICATE KEY UPDATE `current_value` = VALUES(`current_value`);

INSERT INTO `reminder` (`id`, `user_id`, `title`, `remind_time`, `repeat_type`, `enabled`)
VALUES (1, 1, '喝水提醒', '09:00:00', 'DAILY', 1),
       (2, 1, '运动提醒', '18:00:00', 'DAILY', 1)
ON DUPLICATE KEY UPDATE `enabled` = VALUES(`enabled`);
