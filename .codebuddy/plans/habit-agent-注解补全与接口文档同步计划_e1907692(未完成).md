---
name: habit-agent-注解补全与接口文档同步计划
overview: 为 habit-agent 项目补全参数校验注解（Bean Validation），并同步更新 PRD 接口文档：将超出原设计文档的自定义目标打卡接口整合进目标模块，对尚未实现的接口标注"未实现"备注。
todos:
  - id: add-validation-dep
    content: 在 pom.xml 新增 spring-boot-starter-validation 依赖并编译验证
    status: pending
  - id: add-vo-constraints
    content: 为 HabitRecordVO、HabitGoalVO、HabitGoalRecordVO 必填字段添加校验注解
    status: pending
    dependencies:
      - add-validation-dep
  - id: add-controller-validation
    content: 在 HabitController 与 GoalController 入参加 @Valid/@NotNull，并修复 GoalController.getById Bug
    status: pending
    dependencies:
      - add-vo-constraints
  - id: add-exception-handler
    content: GlobalExceptionHandler 补充 MethodArgumentNotValidException 统一响应处理
    status: pending
    dependencies:
      - add-validation-dep
  - id: update-api-doc
    content: 更新 PRD/API接口设计方案.md：整合 goal-records 接口，标注未实现接口
    status: pending
    dependencies:
      - add-controller-validation
---

## 用户需求

对照 PRD《API接口设计方案.md》为 habit-agent 项目补全参数校验注解，并将超出原接口文档定义的接口同步更新到相关文档中。

## 产品概述

本项目为"生活习惯助手 Agent"Spring Boot 后端。当前仅实现了习惯记录（HabitController）与目标管理（GoalController）两大模块，其中目标模块额外包含 PRD 未定义的"自定义目标打卡记录"接口。本次工作分两部分：代码层补全 Bean Validation 参数校验注解；文档层将超纲接口整合进目标模块并标注未实现接口。

## 核心特性

- 在三个入参载体（HabitRecordVO、HabitGoalVO、HabitGoalRecordVO）的必填字段上添加 @NotNull/@NotBlank/@Size 等校验注解
- 在 Controller 的 @RequestBody 入参前加 @Valid，在 @PathVariable/@RequestParam 必填参数前加 @NotNull
- 引入 spring-boot-starter-validation 依赖（Spring Boot 4 的 web starter 不再传递 Hibernate Validator）
- 全局异常处理器补充 MethodArgumentNotValidException 处理，返回统一 Result 失败响应
- 修复 GoalController.getById 误用 HabitRecordRepository 的 Bug
- PRD 文档中将 /api/goal-records/* 五个接口整合进"目标模块"章节
- PRD 文档中对已设计但未实现的接口（提醒、用户、AI分析/对话等约7个Controller）标注"未实现，待模块化开发"备注

## 技术栈

- 框架：Spring Boot 4.1.0 + Spring Web + Spring Data JPA
- 语言：Java 21
- 校验：Jakarta Bean Validation（spring-boot-starter-validation 提供 Hibernate Validator 实现）
- 文档：Markdown（更新 PRD/API接口设计方案.md）

## 实现方案

### 整体策略

以"请求入口参数校验"为主线，自底向上补全三层注解：依赖引入 → VO 字段约束 → Controller 入口触发 → 异常统一响应。文档同步在 PRD 文件内原地更新，不新建文档。

### 关键技术决策

1. **必须新增 validation 依赖**：Spring Boot 4 中 spring-boot-starter-web 不再传递包含 Hibernate Validator，缺少依赖时 @Valid 在运行期会直接失败（No validator found）。这是前置硬性步骤。
2. **VO 复用为入参载体**：当前 Controller 直接用 JPA 实体（HabitRecord/HabitGoal）作为 @RequestBody 入参。为最小改动与关注点分离，采用 PRD 已定义的 VO 作为入参并加校验注解（HabitRecordVO/HabitGoalVO/HabitGoalRecordVO 已是 record 结构，适合承载前端入参），Controller 改为接收对应 VO。
3. **@PathVariable/@RequestParam 校验**：路径/查询参数无法被 @Valid 覆盖，需在参数前直接加 @NotNull，由 MethodValidationPostProcessor 触发 ConstraintViolationException（全局处理器已能捕获）。
4. **异常处理补全**：GlobalExceptionHandler 已处理 ConstraintViolationException，但缺 MethodArgumentNotValidException（@Valid 触发）处理器；需补充，否则校验失败会返回 Spring 默认 400 而非统一 Result。

### 性能与可靠性

- 校验仅发生在请求入口，开销可忽略（单次反射级约束评估），无 N+1 或热路径风险。
- 校验失败时立即短路返回，不进入 Service/DB 层，降低无效写风险。
- 异常处理器统一格式，避免前端解析歧义。

## 实现注意事项

- 引入依赖后务必编译验证（mvn compile），确认 Hibernate Validator 版本与 SB4 兼容。
- VO 字段校验应仅标注真正必填项（如 HabitRecordVO.userId、habitName、category；HabitGoalVO.userId、title；HabitGoalRecordVO.goalId、recordDate），避免过度约束导致正常请求被拒。
- GoalController.getById 当前用 habitRecordRepository 查目标，属逻辑 Bug，应在同文件修改时一并修正为调用 goalService.getGoalById。
- 文档更新保持与原 PRD 第 6 章"业务接口定义"风格一致（请求方法/路径/参数/示例），不破坏现有 48 端点结构。

## 架构设计

当前为典型分层架构：Controller → Service → Repository(JPA)。参数校验位于 Controller 入口层，异常统一由 @RestControllerAdvice 拦截。本次不引入新架构或新模块，仅在现有层内补齐注解与异常分支。

## 目录结构与文件清单

```
pom.xml                                                       # [MODIFY] 新增 spring-boot-starter-validation 依赖
src/main/java/com/habit/agent/common/vo/HabitRecordVO.java    # [MODIFY] 必填字段加 @NotNull/@NotBlank/@Size
src/main/java/com/habit/agent/common/vo/HabitGoalVO.java      # [MODIFY] 必填字段加校验注解
src/main/java/com/habit/agent/common/vo/HabitGoalRecordVO.java# [MODIFY] 必填字段加校验注解
src/main/java/com/habit/agent/controller/HabitController.java # [MODIFY] @RequestBody 改收 VO 并加 @Valid；@PathVariable/@RequestParam 加 @NotNull
src/main/java/com/habit/agent/controller/GoalController.java  # [MODIFY] 同上；修复 getById 误用 repository 的 Bug
src/main/java/com/habit/agent/common/exception/GlobalExceptionHandler.java # [MODIFY] 新增 MethodArgumentNotValidException 处理器
PRD/API接口设计方案.md                                        # [MODIFY] 整合 /api/goal-records/* 进目标模块；未实现接口标注"未实现"
```

## 关键代码结构（校验注解示例）

VO 字段约束示例（仅列关键字段，实际依必填性补充）：

```java
public record HabitRecordVO(
    @NotNull(message = "用户ID不能为空") Long userId,
    @NotBlank(message = "习惯名称不能为空") String habitName,
    @NotBlank(message = "分类不能为空") String category,
    ...
) {}
```

Controller 入口示例：

```java
@PostMapping
public Result<HabitRecordVO> createHabit(@Valid @RequestBody HabitRecordVO vo) { ... }

@GetMapping("/{id}")
public Result<HabitRecordVO> getById(@NotNull @PathVariable Long id) { ... }
```