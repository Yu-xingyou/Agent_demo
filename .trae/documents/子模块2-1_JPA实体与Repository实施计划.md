# 子模块 2-1 实施计划：JPA 实体与 Repository

## Context

阶段一（项目骨架 + MySQL/MongoDB/Spring AI 连通）已完成。现在进入阶段二：业务数据层。
子模块 2-1 创建 4 个 JPA Entity + 4 个 Repository 接口，为 2-2 的 Service/Controller 层提供数据访问基础。
不含 Service 和 Controller，不含业务逻辑。

## 设计决策

| 决策点 | 选择 | 理由 |
|---|---|---|
| 枚举字段 (goalType/reminderType) | Java enum + `@Enumerated(EnumType.STRING)` | schema.sql 种子数据以大写字符串存储（`SLEEP`/`WATER`等），枚举常量名一致即可直接读写 |
| period 字段 | 同样用 enum (`DAILY`/`WEEKLY`/`MONTHLY`) | 与 schema.sql 种子值一致，类型安全 |
| isActive 字段 | `Boolean`（包装类） | Hibernate 自动映射 TINYINT(1)↔Boolean，0↔false/1↔true |
| Lombok 注解 | `@Getter @Setter @NoArgsConstructor @Builder @AllArgsConstructor @ToString` | JPA 实体禁用 `@Data`（`@EqualsAndHashCode` 与懒加载代理冲突），`@NoArgsConstructor` 是 JPA 规范要求 |
| @Column 策略 | 所有字段显式标注 name/nullable/length/precision | `ddl-auto: update` 下严格对齐 schema.sql，避免 Hibernate 意外 ALTER |
| sleepDuration 计算 | `private calculateSleepDuration()`，由 `@PrePersist`/`@PreUpdate` 调用 | 跨午夜逻辑：wakeTime ≤ sleepTime 时加 24h；用 BigDecimal 除法保留 2 位小数 |
| `user` 表保留字 | application.yml 追加 `hibernate.auto_quote_keyword: true` | MySQL 中 `user` 是保留字，Hibernate 需自动加引号 |

## 实施步骤（10 步，按依赖排序）

### 步骤 1：application.yml 追加配置
- 文件：`src/main/resources/application.yml`
- 变更：`spring.jpa.properties.hibernate` 下追加 `auto_quote_keyword: true`
- 目的：处理 `user` 保留字表名

### 步骤 2-3：User 实体 + Repository
- `entity/jpa/User.java`：`@Entity @Table(name="user")`，字段 id/username/nickname/createTime，`@PrePersist` 设 createTime
- `repository/jpa/UserRepository.java`：`findByUsername(String) → Optional<User>`

### 步骤 4-5：HabitGoal 实体 + Repository
- `entity/jpa/HabitGoal.java`：含嵌套枚举 `GoalType {SLEEP,EXERCISE,WATER,DIET}` 和 `Period {DAILY,WEEKLY,MONTHLY}`，字段 id/userId/goalType/targetValue/unit/period/isActive/createTime/updateTime
- `repository/jpa/HabitGoalRepository.java`：`findByUserIdAndIsActive(Long,Boolean)`、`findByUserId(Long)`

### 步骤 6-7：Reminder 实体 + Repository
- `entity/jpa/Reminder.java`：含嵌套枚举 `ReminderType {SLEEP,DIET,EXERCISE,WATER,CUSTOM}`，字段 id/userId/title/reminderTime/reminderType/weekdays/isActive/createTime/updateTime
- `repository/jpa/ReminderRepository.java`：`findByUserIdOrderByCreateTimeDesc(Long)`、`findByUserIdAndIsActive(Long,Boolean)`

### 步骤 8-9：HabitRecord 实体 + Repository（最复杂）
- `entity/jpa/HabitRecord.java`：14 个业务字段 + id + createTime + updateTime，`@PrePersist`/`@PreUpdate` 调用 `calculateSleepDuration()`
- 跨午夜算法：sleepTime/wakeTime 任一为 null → sleepDuration=null；wakeTime ≤ sleepTime → 加 24h；`BigDecimal.divide(60, 2, HALF_UP)`
- `repository/jpa/HabitRecordRepository.java`：3 个派生查询方法（findByUserIdAndRecordDateBetweenOrderByRecordDateDesc / findByUserIdAndRecordDate / findByUserIdOrderByRecordDateDesc）

### 步骤 10：验证 + 勾选验收标准 + Git commit + push
- `mvn compile` 无错误
- `mvn spring-boot:run` 启动无报错，show-sql 不产生破坏性 ALTER
- 在详细模块开发流程.md 中勾选 2-1 验收标准
- Git commit + push

## 涉及文件清单（9 个文件，1 改 8 新）

| 操作 | 文件 |
|---|---|
| 修改 | `src/main/resources/application.yml`（追加 auto_quote_keyword） |
| 新建 | `src/main/java/com/habit/agent/entity/jpa/User.java` |
| 新建 | `src/main/java/com/habit/agent/entity/jpa/HabitRecord.java` |
| 新建 | `src/main/java/com/habit/agent/entity/jpa/HabitGoal.java` |
| 新建 | `src/main/java/com/habit/agent/entity/jpa/Reminder.java` |
| 新建 | `src/main/java/com/habit/agent/repository/jpa/UserRepository.java` |
| 新建 | `src/main/java/com/habit/agent/repository/jpa/HabitRecordRepository.java` |
| 新建 | `src/main/java/com/habit/agent/repository/jpa/HabitGoalRepository.java` |
| 新建 | `src/main/java/com/habit/agent/repository/jpa/ReminderRepository.java` |

## 验收标准

- [ ] 项目启动后 MySQL 表结构与 Entity 匹配
- [ ] 无编译错误

## 验证方式

1. `mvn compile` — 编译通过
2. `mvn spring-boot:run` — 启动成功，无 SQL 语法错误（验证 user 保留字处理）
3. 观察 show-sql 日志 — 不产生破坏性 ALTER 语句（验证 @Column 与 schema.sql 对齐）
