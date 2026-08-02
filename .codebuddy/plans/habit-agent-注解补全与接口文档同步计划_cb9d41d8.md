---
name: habit-agent-注解补全与接口文档同步计划
overview: 为 habit-agent 项目在各层补全参数校验注解与 springdoc-openapi 文档注解（@Tag/@Operation/@Parameter/@Schema），引入 validation 与 springdoc 依赖，并同步更新 PRD 接口文档：整合超纲的自定义目标打卡接口，标注未实现接口。
todos:
  - id: add-deps
    content: pom.xml 新增 validation 与 springdoc-openapi:3.1.0 依赖并 mvn compile 验证
    status: completed
  - id: add-vo-annotations
    content: 为三个 VO 必填字段加校验注解与 @Schema 文档注解
    status: completed
    dependencies:
      - add-deps
  - id: add-controller-annotations
    content: Controller 加 @Validated/@Tag/@Operation/@Parameter，@RequestBody 加 @Valid，修复 GoalController.getById Bug
    status: completed
    dependencies:
      - add-vo-annotations
  - id: add-exception-handler
    content: GlobalExceptionHandler 补充 MethodArgumentNotValidException 统一响应处理
    status: completed
    dependencies:
      - add-deps
  - id: update-api-doc
    content: 更新 PRD/API接口设计方案.md：整合 goal-records 接口，标注未实现接口
    status: completed
    dependencies:
      - add-controller-annotations
  - id: commit-push
    content: git 提交并推送到远程仓库
    status: completed
    dependencies:
      - update-api-doc
---

## 用户需求

对照 PRD《API接口设计方案.md》，为 habit-agent 项目在实际开发中需要加注解的地方补全注解，并将代码中超出原接口文档定义的接口同步更新到文档中。

## 产品概述

本项目为"生活习惯助手 Agent"Spring Boot 后端，当前已实现习惯记录（HabitController）与目标管理（GoalController）两大模块，其中目标模块额外包含 PRD 未定义的"自定义目标打卡记录"接口。本次工作分两部分：代码层在各层补全参数校验注解与 springdoc-openapi 文档注解；文档层将超纲接口整合进目标模块，并对尚未实现的接口标注"未实现"备注。修改完成后需提交并推送到远程仓库，模块化开发流程作为后续开发参照。

## 核心特性

- 引入 spring-boot-starter-validation 与 springdoc-openapi-starter-webmvc-ui:3.1.0 依赖（Spring Boot 4 兼容）
- 在三个 VO（HabitRecordVO、HabitGoalVO、HabitGoalRecordVO）必填字段加 @NotNull/@NotBlank/@Size 校验注解，并加 @Schema 文档注解
- 在两个 Controller 类上加 @Validated（启用方法参数校验）与 @Tag；方法加 @Operation，路径/请求参数加 @NotNull/@Parameter，@RequestBody 加 @Valid
- 全局异常处理器补充 MethodArgumentNotValidException 处理，返回统一 Result 失败响应
- 修复 GoalController.getById 误用 HabitRecordRepository 查询目标的 Bug
- PRD 文档中将 /api/goal-records/* 五个接口整合进"目标模块"章节
- PRD 文档中对已设计但未实现的接口（提醒、用户、AI 分析/对话等约 7 个 Controller）标注"未实现，待模块化开发"备注
- 所有修改完成后 git 提交并推送到远程仓库

## 技术栈

- 框架：Spring Boot 4.1.0 + Spring Web + Spring Data JPA
- 语言：Java 21
- 参数校验：Jakarta Bean Validation（spring-boot-starter-validation 提供 Hibernate Validator）
- 接口文档：springdoc-openapi v3.1.0（springdoc-openapi-starter-webmvc-ui，Swagger UI 访问 /swagger-ui.html，JSON 在 /v3/api-docs）
- 文档：Markdown（更新 PRD/API接口设计方案.md）

## 实现方案

### 整体策略

以"请求入口参数校验 + 全层文档注解"为主线，自底向上补齐：依赖引入 → VO 字段约束与文档描述 → Controller 入口触发与文档标注 → 异常统一响应 → 文档同步 → 提交推送。文档同步在 PRD 文件内原地更新，不新建文档。

### 关键技术决策

1. **必须新增 validation 依赖**：Spring Boot 4 中 spring-boot-starter-web 不再传递 Hibernate Validator，缺少依赖时 @Valid 在运行期会直接失败（No validator found）。这是前置硬性步骤。
2. **引入 springdoc-openapi v3.1.0**：经官方文档核实，Spring Boot 4.x 对应 springdoc v3.1.0，使用 springdoc-openapi-starter-webmvc-ui:3.1.0（含 Swagger UI）。在 Controller 用 @Tag/@Operation/@Parameter、VO 字段用 @Schema 生成在线文档，与 PRD 接口约定保持一致。
3. **VO 复用为入参载体**：当前 Controller 直接用 JPA 实体作为 @RequestBody 入参。采用 PRD 已定义的 VO 作为入参并加校验与文档注解，Controller 改为接收对应 VO，符合关注点分离。
4. **@Validated 类级注解**：@PathVariable/@RequestParam 的 @NotNull 校验需由类级 @Validated 触发 MethodValidationPostProcessor，否则不生效。这是让参数级校验真正运行的关键。
5. **异常处理补全**：GlobalExceptionHandler 已处理 ConstraintViolationException，但缺 MethodArgumentNotValidException（@Valid 触发）处理器；需补充，否则校验失败会返回 Spring 默认 400 而非统一 Result。
6. **Service/Repository 不改**：经核查 @Service/@Repository/@Transactional/@RequiredArgsConstructor/@Slf4j 已齐全规范，无需补充 Spring 注解，避免无意义改动。

### 性能与可靠性

- 校验仅发生在请求入口，开销为单次反射级约束评估，无 N+1 或热路径风险。
- 校验失败时立即短路返回，不进入 Service/DB 层，降低无效写风险。
- 异常处理器统一格式，避免前端解析歧义。
- springdoc 在启动时扫描一次，运行时无额外开销。

## 实现注意事项

- 引入依赖后务必执行 mvn compile，确认 Hibernate Validator 与 springdoc 3.1.0 与 SB4 兼容。
- VO 字段校验仅标注真正必填项：HabitRecordVO.userId/habitName/category；HabitGoalVO.userId/title；HabitGoalRecordVO.goalId/recordDate，避免过度约束导致正常请求被拒。
- GoalController.getById 当前用 habitRecordRepository 查目标，属逻辑 Bug，应在同文件修改时一并修正为调用 goalService 对应方法。
- 文档更新保持与原 PRD 第 6 章"业务接口定义"风格一致（请求方法/路径/参数/示例），不破坏现有 48 端点结构。
- 仅在 PRD 中标注未实现接口，不为其编写代码。

## 架构设计

当前为典型分层架构：Controller → Service → Repository(JPA)。参数校验位于 Controller 入口层，文档注解分布于 Controller（@Tag/@Operation）与 VO（@Schema），异常统一由 @RestControllerAdvice 拦截。本次不引入新架构或新模块，仅在现有层内补齐注解与异常分支、同步文档。

## 目录结构与文件清单

```
pom.xml                                                       # [MODIFY] 新增 spring-boot-starter-validation 与 springdoc-openapi-starter-webmvc-ui:3.1.0 依赖
src/main/java/com/habit/agent/common/vo/HabitRecordVO.java    # [MODIFY] 必填字段加 @NotNull/@NotBlank/@Size + @Schema
src/main/java/com/habit/agent/common/vo/HabitGoalVO.java      # [MODIFY] 必填字段加校验注解 + @Schema
src/main/java/com/habit/agent/common/vo/HabitGoalRecordVO.java# [MODIFY] 必填字段加校验注解 + @Schema
src/main/java/com/habit/agent/controller/HabitController.java # [MODIFY] 加 @Validated/@Tag；方法加 @Operation/@Parameter；@RequestBody 加 @Valid；参数加 @NotNull
src/main/java/com/habit/agent/controller/GoalController.java  # [MODIFY] 同上；修复 getById 误用 repository 的 Bug
src/main/java/com/habit/agent/common/exception/GlobalExceptionHandler.java # [MODIFY] 新增 MethodArgumentNotValidException 处理器
PRD/API接口设计方案.md                                        # [MODIFY] 整合 /api/goal-records/* 进目标模块；未实现接口标注"未实现，待模块化开发"
```

## 关键代码结构（示例）

VO 字段约束与文档示例（仅列关键字段）：

```java
public record HabitRecordVO(
    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID", example = "1")
    Long userId,
    @NotBlank(message = "习惯名称不能为空")
    @Schema(description = "习惯名称", example = "晨跑")
    String habitName,
    @NotBlank(message = "分类不能为空")
    @Schema(description = "分类", example = "EXERCISE")
    String category
) {}
```

Controller 入口示例：

```java
@Validated
@Tag(name = "习惯记录", description = "习惯打卡记录管理")
@RestController
@RequestMapping("/api/habits")
public class HabitController {
    @Operation(summary = "录入/更新打卡")
    @PostMapping
    public Result<HabitRecordVO> createHabit(@Valid @RequestBody HabitRecordVO vo) { ... }

    @Operation(summary = "按ID查询")
    @GetMapping("/{id}")
    public Result<HabitRecordVO> getById(@NotNull @Parameter(description="记录ID") @PathVariable Long id) { ... }
}
```

## Agent Extensions

### SubAgent

- **code-explorer**
- Purpose: 在补充注解与更新文档前，跨文件核查所有 Controller/Service/VO/Repository 的现有注解与超纲接口清单，确保计划覆盖完整且无遗漏
- Expected outcome: 输出准确的注解缺口清单与超纲接口列表，为注解补全和文档整合提供完整依据