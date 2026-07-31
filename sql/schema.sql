-- ============================================================================
-- 生活习惯助手 Agent — MySQL 建表脚本
-- 数据库: habit_agent | 字符集: utf8mb4 | 引擎: InnoDB
-- 技术栈: Spring Boot 4.1 + Spring AI 2.0 + JPA (Hibernate 7)
-- 数据分工: MySQL 存储关系型业务数据，MongoDB 存储文档型 AI 数据
-- 生成日期: 2026-07-30
-- ============================================================================

-- 删除旧数据库（开发环境，生产环境请注释此行）
-- DROP DATABASE IF EXISTS `habit_agent`;

CREATE DATABASE IF NOT EXISTS `habit_agent`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `habit_agent`;

-- ----------------------------------------------------------------------------
-- 表 1: user — 用户表
-- 单用户演示场景默认 userId=1，后续可扩展多用户
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    `username`    VARCHAR(50)  NOT NULL COMMENT '登录用户名',
    `nickname`    VARCHAR(50)  NULL     COMMENT '昵称',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    UNIQUE KEY `uk_username` (`username`) COMMENT '用户名唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='用户表';

-- ----------------------------------------------------------------------------
-- 表 2: habit_record — 习惯记录表（核心业务表）
-- 每日打卡数据，同一天重复打卡为更新（uk_user_date 唯一约束）
-- sleep_duration 由 JPA @PrePersist/@PreUpdate 自动计算，无需手动填入
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `habit_record` (
    `id`                BIGINT       PRIMARY KEY AUTO_INCREMENT,
    `user_id`           BIGINT       NOT NULL                COMMENT '用户ID',
    `record_date`       DATE         NOT NULL                COMMENT '记录日期 yyyy-MM-dd',
    `sleep_time`        TIME         NULL                    COMMENT '入睡时间 HH:mm',
    `wake_time`         TIME         NULL                    COMMENT '起床时间 HH:mm',
    `sleep_duration`    DECIMAL(4,2) NULL                    COMMENT '睡眠时长(小时)，服务端自动计算',
    `sleep_quality`     TINYINT      NULL                    COMMENT '睡眠质量 1(极差)-5(极好)',
    `diet_desc`         VARCHAR(500) NULL                    COMMENT '饮食情况描述',
    `diet_score`        TINYINT      NULL                    COMMENT '饮食健康评分 1(差)-5(好)',
    `exercise_type`     VARCHAR(100) NULL                    COMMENT '运动类型(跑步/游泳/瑜伽等)',
    `exercise_duration` INT          NULL                    COMMENT '运动时长(分钟)',
    `water_intake`      INT          NULL                    COMMENT '饮水量(ml)',
    `mood`              TINYINT      NULL                    COMMENT '心情 1(差)-5(好)',
    `remark`            VARCHAR(500) NULL                    COMMENT '备注',
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_user_date` (`user_id`, `record_date`) COMMENT '同一天重复打卡为更新',
    INDEX `idx_user_date`     (`user_id`, `record_date`)  COMMENT '按用户+日期范围查询',
    INDEX `idx_user_recent`   (`user_id`, `record_date` DESC) COMMENT '查询最近记录'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='习惯记录表(每日打卡核心数据)';

-- ----------------------------------------------------------------------------
-- 表 3: habit_goal — 习惯目标表
-- 四种目标类型: SLEEP(睡眠) / EXERCISE(运动) / WATER(饮水) / DIET(饮食)
-- 用于达成率统计和雷达图展示
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `habit_goal` (
    `id`            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    `user_id`       BIGINT       NOT NULL                COMMENT '用户ID',
    `goal_type`     VARCHAR(30)  NOT NULL                COMMENT '目标类型: SLEEP/EXERCISE/WATER/DIET',
    `target_value`  DECIMAL(8,2) NOT NULL                COMMENT '目标值',
    `unit`          VARCHAR(20)  NULL                    COMMENT '单位: hours/minutes/ml/score',
    `period`        VARCHAR(20)  NOT NULL DEFAULT 'DAILY' COMMENT '周期: DAILY/WEEKLY/MONTHLY',
    `is_active`     TINYINT      NOT NULL DEFAULT 1      COMMENT '是否启用: 0=否 1=是',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_user_type`   (`user_id`, `goal_type`)  COMMENT '同用户同类型目标唯一',
    INDEX `idx_user_active` (`user_id`, `is_active`) COMMENT '查询用户启用的目标'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='习惯目标表(达成率统计基准)';

-- ----------------------------------------------------------------------------
-- 表 4: reminder — 打卡提醒表
-- 对应 PRD 可选功能「打卡提醒」，支持用户自定义提醒时间和方式
-- reminder_type: SLEEP/DIET/EXERCISE/WATER/CUSTOM
-- weekdays: 逗号分隔数字，1-7 对应周一到周日，如 "1,2,3,4,5" 表示工作日
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `reminder` (
    `id`             BIGINT       PRIMARY KEY AUTO_INCREMENT,
    `user_id`        BIGINT       NOT NULL                COMMENT '用户ID',
    `title`          VARCHAR(100) NOT NULL                COMMENT '提醒标题',
    `reminder_time`  TIME         NOT NULL                COMMENT '提醒时间 HH:mm',
    `reminder_type`  VARCHAR(30)  NOT NULL                COMMENT '提醒类型: SLEEP/DIET/EXERCISE/WATER/CUSTOM',
    `weekdays`       VARCHAR(20)  NOT NULL DEFAULT '1,2,3,4,5,6,7' COMMENT '星期几触发，逗号分隔 1-7',
    `is_active`      TINYINT      NOT NULL DEFAULT 1      COMMENT '是否启用: 0=否 1=是',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_user_title` (`user_id`, `title`) COMMENT '同用户同标题提醒唯一',
    INDEX `idx_user_active` (`user_id`, `is_active`) COMMENT '查询用户启用的提醒'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='打卡提醒表(自定义提醒时间)';


-- ============================================================================
-- 种子数据（开发演示用，可直接运行）
-- ============================================================================

-- 默认用户 (userId=1，单用户演示场景)
INSERT INTO `user` (`id`, `username`, `nickname`) VALUES
    (1, 'demo', '演示用户')
ON DUPLICATE KEY UPDATE `nickname` = VALUES(`nickname`);

-- 默认习惯目标 (4 种类型各一条)
INSERT INTO `habit_goal` (`user_id`, `goal_type`, `target_value`, `unit`, `period`, `is_active`) VALUES
    (1, 'SLEEP',    8.00,  'hours',   'DAILY', 1),
    (1, 'EXERCISE', 30.00, 'minutes', 'DAILY', 1),
    (1, 'WATER',    2000.00,'ml',     'DAILY', 1),
    (1, 'DIET',     4.00,  'score',   'DAILY', 1)
ON DUPLICATE KEY UPDATE `is_active` = 1;

-- 默认打卡提醒 (2 条示例)
INSERT INTO `reminder` (`user_id`, `title`, `reminder_time`, `reminder_type`, `weekdays`, `is_active`) VALUES
    (1, '睡眠打卡提醒', '23:00:00', 'SLEEP',    '1,2,3,4,5',     1),
    (1, '饮水提醒',     '10:00:00', 'WATER',    '1,2,3,4,5,6,7', 1)
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);

-- 示例打卡数据 (近 7 天，供前端页面和图表展示)
-- 注意: sleep_duration 的值会由 JPA Entity 自动计算，此处手动填入供 SQL 直接查询
INSERT INTO `habit_record` (`user_id`, `record_date`, `sleep_time`, `wake_time`, `sleep_duration`, `sleep_quality`, `diet_desc`, `diet_score`, `exercise_type`, `exercise_duration`, `water_intake`, `mood`, `remark`) VALUES
    (1, CURDATE() - INTERVAL 6 DAY, '23:30:00', '07:00:00', 7.50, 4, '早餐燕麦，午餐鸡胸肉沙拉，晚餐轻食', 4, '跑步', 45, 1800, 4, '今天感觉精神不错'),
    (1, CURDATE() - INTERVAL 5 DAY, '00:15:00', '07:30:00', 7.25, 3, '午餐外卖，晚餐火锅',                   2, '散步', 20, 1200, 3, ''),
    (1, CURDATE() - INTERVAL 4 DAY, '23:00:00', '06:30:00', 7.50, 5, '三餐规律，多蔬菜少油盐',               5, '游泳', 60, 2000, 5, '精力充沛'),
    (1, CURDATE() - INTERVAL 3 DAY, '01:00:00', '08:00:00', 7.00, 2, '午餐快餐，晚餐烧烤',                   1, NULL,  NULL, 800, 2, '熬夜了'),
    (1, CURDATE() - INTERVAL 2 DAY, '22:45:00', '06:15:00', 7.50, 4, '早餐全麦面包，午餐鱼肉，晚餐蔬菜汤',   4, '瑜伽', 30, 1600, 4, '恢复中'),
    (1, CURDATE() - INTERVAL 1 DAY, '23:15:00', '06:45:00', 7.50, 4, '正常饮食，少油少糖',                   4, '力量训练', 40, 1900, 4, '状态良好'),
    (1, CURDATE(),                   '23:30:00', '07:00:00', 7.50, 4, '早餐燕麦，午餐鸡胸肉沙拉，晚餐轻食',   4, '跑步', 45, 1800, 4, '今天感觉精神不错')
ON DUPLICATE KEY UPDATE
    `sleep_time` = VALUES(`sleep_time`),
    `wake_time` = VALUES(`wake_time`),
    `sleep_duration` = VALUES(`sleep_duration`),
    `update_time` = CURRENT_TIMESTAMP;


-- ============================================================================
-- 验证查询（运行后可执行以下语句确认数据）
-- ============================================================================

-- SELECT * FROM user;
-- SELECT * FROM habit_goal WHERE user_id = 1;
-- SELECT * FROM habit_record WHERE user_id = 1 ORDER BY record_date DESC;
-- SELECT * FROM reminder WHERE user_id = 1 AND is_active = 1;
