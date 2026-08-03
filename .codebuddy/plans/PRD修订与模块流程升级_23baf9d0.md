---
name: PRD修订与模块流程升级
overview: 以 agent模型.md 为原始业务需求基准（保持不动），大幅扩充并升级其余 4 份 PRD（agent_demo开发计划、API接口设计方案、API接口文档设计计划、向量库搭建计划表），重点重写「详细模块开发流程」使其同步真实代码现状，并正式纳入 Goal 自定义目标（CUSTOM + 5 个打卡记录接口）口径。
todos:
  - id: explore-goal-code
    content: 使用 [subagent:code-explorer] 核对 Goal 模块真实字段与端点，输出清单
    status: completed
  - id: rewrite-dev-flow
    content: 大幅重写 PRD/详细模块开发流程.md，同步代码现状并新增 GoalRecords 子模块
    status: completed
    dependencies:
      - explore-goal-code
  - id: upgrade-dev-plan
    content: 扩充 PRD/agent_demo开发计划.md 对齐双库多Agent并纳入 CUSTOM 目标
    status: completed
    dependencies:
      - rewrite-dev-flow
  - id: upgrade-api-design
    content: 修订 PRD/API接口设计方案.md 与 API接口文档设计计划.md 统一枚举与端点口径
    status: completed
    dependencies:
      - rewrite-dev-flow
  - id: expand-vector-doc
    content: 扩充 PRD/向量库搭建计划表.md 细化 RAG 接入与降级方案
    status: completed
    dependencies:
      - rewrite-dev-flow
  - id: cross-check
    content: 交叉校验 4 份文档端点计数与枚举一致性，修正冲突
    status: completed
    dependencies:
      - upgrade-dev-plan
      - upgrade-api-design
      - expand-vector-doc
---

## 用户需求概述

以 `PRD/agent模型.md` 作为原始业务需求文档（本次保持不变），对其余 4 份 PRD 文档进行升级与扩充，使其与项目已落地的现代架构（前后端分离、MySQL+MongoDB 双库、多智能体路由、通义千问 DashScope 接入）保持一致，并正式纳入 Goal 自定义目标（CUSTOM 类型 + 5 个打卡记录接口）口径。其中「详细模块开发流程」需大幅重写并同步真实代码现状。

## 核心修改内容

- 保持 `agent模型.md` 原文不动，作为业务真相源。
- 扩充 `agent_demo开发计划.md`：在每个阶段补充目标、技术点、核心产出、端点明细，对齐真实架构。
- 扩充 `API接口设计方案.md` 与 `API接口文档设计计划.md`：规范化接口文档结构、字段表、错误码、SSE 格式，统一 period 枚举为 DAY/WEEK/MONTH，纳入 Goal 自定义目标与 /api/goal-records 5 接口口径。
- 扩充 `向量库搭建计划表.md`：细化 MongoDB Atlas 向量库搭建步骤与 RAG 接入说明。
- 大幅重写 `详细模块开发流程.md`：阶段仍为 10 阶段、25 子模块，但同步代码实际状态；阶段二 Goal 模块更新为 CUSTOM 类型 + period=DAY/WEEK/MONTH + currentValue/weeklyAchievement；新增 Goal 自定义目标打卡记录子模块；统一总览表端点计数到真实值。

## 约束与边界

- 仅修改 `PRD/` 目录下 5 个文件中的 4 个（agent模型.md 除外）。
- 不修改任何 Java / Vue / 配置文件，仅文档修订。
- 所有接口、字段、枚举需与现有代码（HabitGoalVO、各 Controller）严格一致。

## 技术栈与文档基线

- 文档格式：Markdown（中文），沿用现有 PRD 目录结构与标题层级。
- 参照源码（只读，用于对齐口径）：
- `src/main/java/com/habit/agent/common/vo/HabitGoalVO.java`（goalType 含 CUSTOM、period=DAY/WEEK/MONTH、currentValue、weeklyAchievement）
- `src/main/java/com/habit/agent/controller/` 下 HabitController、GoalController、AnalysisController
- `README.md`（前后端分离、Vite 代理、Spring Boot 4.1 + Vue3 技术栈）
- 不引入新工具/依赖，纯文档编辑。

## 修订策略

1. **基线单一来源**：以 `agent模型.md` 的业务功能范围（必做/可选）为上层依据，将其映射为现代化技术实现描述，写入其余 4 份文档。
2. **口径统一**：所有文档统一使用 `period` 枚举 `DAY/WEEK/MONTH`、`goalType` 枚举 `SLEEP/EXERCISE/WATER/DIET/CUSTOM`、`weekdays` 用整型数组；统一后端 /api 端点计数（Habit 7 + Goal 6 + GoalRecords 5 + Chat 3 + Session 7 + Analysis 4 + AiAnalysis 6 + Rag 5 + Reminder 5 = 48，与详细流程总览一致，修正开发计划九中误写的 35）。
3. **Goal 自定义目标正式化**：在开发计划、API 设计两份文档中将 CUSTOM 类型与 `/api/goal-records/records` 5 接口从「超纲标注」提升为正式功能模块，与详细流程保持一致。
4. **详细流程大幅重写**：

- 顶部架构说明保留并细化（前后端分离 + 双库 + 多 Agent）。
- 总览表端点列修正为真实值（阶段二 16 端点含 GoalRecords 5；合计 48）。
- 阶段二 2-2 子模块：GoalService/GoalController 补充 CUSTOM/customName/displayName/currentValue/weeklyAchievement 字段与 period 枚举；新增 2-3 子模块「自定义目标打卡记录」（GoalRecord entity/repo/service/controller，5 端点）。
- 其余阶段补充「实施状态标记」与「技术点映射」，与 agent模型可选功能（周报月报、图表、提醒、达成率）逐条对应。

5. **扩充单薄处**：开发计划每阶段增加「技术点覆盖」「验收清单」；API 设计方案补充完整 VO 字段表与错误码表；向量库文档补充 RAG 接入时序与本地降级方案。

## 执行注意事项

- 所有文档修订需交叉引用，避免端点数/枚举再次出现不一致。
- 不删除原有有价值内容（如向量库文档的 Atlas 步骤），仅扩充。
- 改动后在 `详细模块开发流程.md` 顶部明确「本文档为后续开发参照标准」以符合工作空间约定。

## 目录结构与文件清单

```
PRD/
├── agent模型.md                  # [不改] 原始业务需求文档，本次保持原样
├── agent_demo开发计划.md         # [MODIFY] 扩充 10 阶段目标/技术点/产出/端点，对齐双库+多Agent，纳入 Goal 自定义
├── API接口设计方案.md            # [MODIFY] 扩充接口规范、VO 字段表、错误码、SSE 格式，统一枚举与端点计数，纳入 GoalRecords
├── API接口文档设计计划.md        # [MODIFY] 扩充文档结构规范与字段表模板，对齐正式口径
├── 向量库搭建计划表.md           # [MODIFY] 扩充 RAG 接入时序、本地降级方案、与开发阶段衔接
└── 详细模块开发流程.md           # [MODIFY] 大幅重写：同步代码现状，Goal CUSTOM+period 枚举，新增 GoalRecords 子模块，统一端点计数
```

## Agent Extensions

### SubAgent

- **code-explorer**
- Purpose: 在执行详细流程重写前，批量核对 `src/main/java/com/habit/agent/` 下 Goal 相关 entity/service/controller 真实字段、端点路径与方法，确保文档与代码严格一致。
- Expected outcome: 产出 Goal 模块（含自定义目标打卡记录）的真实文件清单、字段定义、端点路径，供 PRD 文档精确修订使用。