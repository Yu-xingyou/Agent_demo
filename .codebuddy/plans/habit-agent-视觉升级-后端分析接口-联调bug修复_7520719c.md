---
name: habit-agent-视觉升级-后端分析接口-联调bug修复
overview: 对 habit-agent 项目前三个阶段页面进行现代审美视觉升级（分块式首页+渐变主题+滑动/饼图/折线+全站粒子+任务目标预留），同时实现后端分析接口打通真实数据，并自主审查修复前后端联调中已发现的所有接口/数据格式/响应码 bug，确保前后端交互正确。
design:
  architecture:
    framework: vue
  styleKeywords:
    - 流动渐变
    - 玻璃拟态
    - 粒子流动
    - 科技通透
    - 大气简约
  fontSystem:
    fontFamily: Noto Sans SC
    heading:
      size: 28px
      weight: 700
    subheading:
      size: 18px
      weight: 600
    body:
      size: 15px
      weight: 400
  colorSystem:
    primary:
      - "#0f766e"
      - "#6366f1"
      - "#a855f7"
    background:
      - "#f8fafc"
      - "#0f172a"
      - "#ffffff"
    text:
      - "#1e293b"
      - "#f8fafc"
      - "#64748b"
    functional:
      - "#22c55e"
      - "#f59e0b"
      - "#ef4444"
      - "#06b6d4"
todos:
  - id: theme-tokens
    content: 建立渐变主题 CSS 变量与 Tailwind 工具类，集中目标配色常量
    status: completed
  - id: particle-canvas
    content: 新建 ParticleCanvas 轻量粒子组件并接入 MainLayout 全局背景
    status: completed
    dependencies:
      - theme-tokens
  - id: backend-analysis
    content: 使用 [subagent:code-explorer] 复用既有仓库，新建 AnalysisController/Service 与 4 个 VO 实现趋势/达成率/雷达接口
    status: completed
  - id: analysis-api
    content: 新建前端 api/analysis.js 封装四个分析端点调用
    status: completed
    dependencies:
      - backend-analysis
  - id: fix-integration-bugs
    content: 使用 [skill:frontend-design] 辅助风格前提下，修复响应码判定、goal.js 端点、目标激活逻辑等联调 bug
    status: completed
  - id: home-redesign
    content: 分块式重构首页：Hero/统计/目标/滑动卡/饼图/折线，接真实接口
    status: completed
    dependencies:
      - particle-canvas
      - analysis-api
      - fix-integration-bugs
  - id: trend-redesign
    content: 充实趋势页折线图与五维雷达图，渲染真实后端数据
    status: completed
    dependencies:
      - analysis-api
      - particle-canvas
  - id: other-pages
    content: 统一打卡/历史/AI页视觉风格并加入粒子与渐变
    status: completed
    dependencies:
      - particle-canvas
  - id: goal-view
    content: 新建任务目标专属视图，预留空间与配色，heavy 粒子
    status: completed
    dependencies:
      - particle-canvas
  - id: integration-verify
    content: 启动前后端实测各页面数据流，修复暴露问题并验证接口
    status: completed
    dependencies:
      - home-redesign
      - trend-redesign
      - other-pages
      - goal-view
  - id: commit-push
    content: 执行 git 提交并推送至远程仓库
    status: completed
    dependencies:
      - integration-verify
---

## 用户需求概述

对 habit-agent（Spring Boot 4 + Vue3 生活习惯助手）项目前三个阶段已完成的前端页面进行现代审美视觉升级，同时实现后端分析接口打通真实数据，并自主审查修复前后端联调中已发现的所有接口不匹配、数据格式不一致、请求参数缺失、响应处理错误等 bug，确保前后端交互逻辑正确且功能正常运行，整体呈现大气、流动式的视觉体验。

## 核心功能

- 首页分块式重构：Hero 轮播区、今日概览统计卡、任务目标进度区（基于现有 HabitGoal 模块预留专属展示空间与配色）、滑动卡片区、心情饼图、近 7 天折线趋势、快捷入口，全部接入真实接口。
- 现代审美渐变主题：以流动渐变（青绿→靛蓝→紫）替换单一主色，统一应用于导航栏、卡片、按钮、图表。
- 全站轻量 Canvas 粒子背景：自建零依赖粒子组件，普通页面轻量粒子，任务目标/趋势视图采用重粒子效果。
- 后端分析接口实现：新增 AnalysisController/AnalysisService 及配套 VO，基于 HabitRecord + HabitGoal 计算趋势、达成率、概览、雷达维度，供前端图表消费。
- 其他页面统一视觉标准：打卡页、历史页、AI 建议页沿用首页风格（渐变、微动效、粒子背景）。
- 任务目标板块：基于现有 HabitGoal 模块，新建专属视图，预留空间与配色，heavy 粒子。
- 联调 Bug 修复：统一前后端响应码判定、修复目标接口不匹配、修正自定义目标激活逻辑、核查日期/数值序列化兼容性，并实测验证数据流。

## 视觉与交互效果

页面采用大圆角玻璃拟态卡片、流动渐变描边、悬浮微动效；粒子在背景缓慢游动并与鼠标轻微交互；首页各区块以渐变分隔、滚动进入动画；图表配色与主题渐变一致，整体观感科技、通透、大气。

## 技术栈选择

- 前端：沿用 Vue3 + Vite5 + Vue Router4 + Pinia2 + Axios + Element Plus2 + ECharts5 + TailwindCSS3.4。新增粒子效果使用原生 Canvas（零新增依赖）。
- 后端：沿用 Spring Boot 4 + Spring Data JPA + H2。新增分析模块遵循既有 Controller→Service→Repository 分层与 Result 统一响应规范。
- 图表：ECharts 已具备，饼图/折线/雷达均用 vue-echarts 或原生 echarts.init。

## 实现方案

### 总体策略

以"视觉系统升级 + 数据通道打通 + 联调 Bug 修复"三线推进：先建立全局设计令牌与粒子基础设施，再逐页重构并接入真实接口；后端独立补齐分析服务；同步将审查发现的响应码、接口路径、目标激活等 bug 一次性修复，最后联调实测。

### 关键技术决策

1. **响应码统一（核心 bug 修复）**：后端 `Result.success` 返回 `code=200`（实际代码确认），而前端 `request.js` 拦截器以 `code !== 0` 判定失败 → 全站成功请求被 reject。统一约定成功 `code=200`，前端拦截器改为 `body.code === 200 || body.code === 0` 为成功，避免误伤。
2. **粒子组件 ParticleCanvas.vue**：基于 requestAnimationFrame 的轻量 Canvas 粒子系统，props 控制 `density`（low/heavy）与 `theme` 配色；挂在 MainLayout 作为固定背景层（pointer-events:none），按路由 meta 决定密度，任务/趋势视图设 heavy。重粒子仅增加粒子数与连线，避免阴影/模糊等昂贵绘制。
3. **渐变主题体系**：在 style.css 用 CSS 自定义属性定义 `--grad-primary`、`--grad-accent` 等多段渐变；Tailwind 扩展 `backgroundImage` 与 `boxShadow` 工具类；所有卡片/导航统一引用，保证一致性且便于整体换色。
4. **后端分析计算**：AnalysisService 复用 HabitRecordRepository 既有 `findByUserIdAndRecordDateBetweenOrderByRecordDateDesc` 与 GoalService 目标数据，纯内存聚合（数据量小，O(n) 可接受），不引入新查询；达成率按目标 targetValue 与近 N 天实际均值计算，雷达图返回五维 0-100 分值。
5. **目标接口修复**：`goal.js` 的 `listRecordsByDate` 调用 `/goal-records/records/date` 后端无此端点（仅 `/records/today` 与 `/records?startDate&endDate`），改为对齐的真实端点；`GoalService.getActiveGoalsWithCustom` 当前实现与 `getActiveGoals` 完全相同，需确保自定义目标（CUSTOM）保存时默认激活，使首页任务目标区能取到自定义目标。
6. **数据格式核查**：HabitRecord 的 `recordDate`(LocalDate) 反序列化与 `sleepDuration`/`value`(BigDecimal) 前端拼接 'h'/'ml' 兼容性在实测中验证，必要时后端补充 Jackson 配置或前端 Number 转换。

### 性能与可靠性

- 粒子使用单 Canvas + rAF，组件卸载取消动画帧；heavy 模式限制粒子数（约 60-80）与连线距离，避免低端机卡顿。
- 首页数据并行请求（Promise.all），加载失败降级为空态，不阻塞渲染。
- 图表容器 autoresize + 路由切换时 dispose，防止内存泄漏；窗口 resize 监听节流。
- 后端分析无 N+1，聚合在单条查询内存完成；userId 强制默认用户，遵循越权防护约定。

### 实现要点（防回归）

- 不改动既有后端业务接口（HabitController/GoalController 路径与既有方法），仅在 `com.habit.agent` 下新增 analysis 包与对 GoalService 逻辑微调（保持既有方法签名）。
- 前端保持 `/api` 代理不变；新增 api/analysis.js 仅调用新增端点。
- 复用现有 moodColors/goal 配色逻辑，新增目标类型配色映射（SLEEP/EXERCISE/WATER/DIET/CUSTOM）集中到 constants/theme.js，避免散落。
- 保留 HomeView 现有饼图（心情分布）逻辑，升级为渐变主题样式并补充真实折线趋势区块。

## 架构设计

前端组件层次：MainLayout（粒子背景 + 渐变导航）→ 各 View（分块卡片 + ECharts）；公共：ParticleCanvas、渐变工具类、目标配色常量。
后端层次：AnalysisController（REST）→ AnalysisService（聚合计算，依赖 HabitRecordRepository + GoalService）→ 复用既有 Entity/Repository，输出新 VO。

```mermaid
graph TD
  A[MainLayout 粒子背景+渐变导航] --> B[HomeView 分块首页]
  A --> C[TrendView 折线+雷达]
  A --> D[CheckinView]
  A --> E[HistoryView]
  A --> F[AiChatView]
  A --> G[GoalView 任务目标板块]
  B --> H[ParticleCanvas]
  C --> H
  G --> H
  B --> I[api/analysis + api/habit + api/goal]
  C --> I
  I --> J[AnalysisController]
  J --> K[AnalysisService]
  K --> L[HabitRecordRepository]
  K --> M[GoalService]
```

## 目录结构

```
frontend/src/
├── style.css                     # [MODIFY] 新增渐变主题 CSS 变量、玻璃拟态、流动动效 keyframes
├── tailwind.config.js            # [MODIFY] 扩展渐变 backgroundImage、shadow、animation 工具类
├── components/
│   └── ParticleCanvas.vue        # [NEW] 轻量 Canvas 粒子背景，props: density/theme，rAF 驱动
├── layouts/
│   └── MainLayout.vue            # [MODIFY] 集成 ParticleCanvas 背景、流动渐变导航、路由 meta 控密度
├── views/
│   ├── HomeView.vue              # [MODIFY] 分块式重构：Hero/统计/目标/滑动卡/饼图/折线/入口，接真实接口
│   ├── TrendView.vue             # [MODIFY] 睡眠折线+运动柱状+饮水折线+雷达图，接 /api/analysis
│   ├── CheckinView.vue           # [MODIFY] 统一渐变风格+粒子+任务目标预留提示区
│   ├── HistoryView.vue           # [MODIFY] 统一渐变表格卡片风格+粒子背景
│   ├── AiChatView.vue            # [MODIFY] 渐变气泡+粒子背景+流动感
│   └── GoalView.vue              # [NEW] 任务目标专属视图：基于 HabitGoal，预留空间+配色，heavy 粒子
├── api/
│   ├── analysis.js               # [NEW] 封装 /api/analysis/trends|achievement|overview|radar
│   └── goal.js                   # [MODIFY] 修复 listRecordsByDate 端点路径与参数对齐
├── utils/
│   └── request.js                # [MODIFY] 修复响应码判定（code===200 或 0 为成功）
└── constants/
    └── theme.js                  # [NEW] 目标类型→配色映射、渐变 token 引用，集中管理

src/main/java/com/habit/agent/
├── controller/AnalysisController.java   # [NEW] GET /api/analysis/* 4 端点，返回 Result<VO>
├── service/AnalysisService.java         # [NEW] 趋势/达成率/概览/雷达聚合计算
├── service/GoalService.java             # [MODIFY] 修正自定义目标激活逻辑，确保 getActiveWithCustom 含 CUSTOM
└── common/vo/
    ├── TrendDataVO.java                 # [NEW] 日期列表+睡眠/运动/饮水数值序列
    ├── AchievementRateVO.java           # [NEW] 各维度达成率+总达成率
    ├── AnalysisOverviewVO.java          # [NEW] 平均睡眠/运动/饮水/心情/打卡天数
    └── RadarDataVO.java                 # [NEW] 五维指标/数值/目标
```

## 联调 Bug 修复清单

- FIX#1（致命）：`request.js` 成功判定 `body.code !== 0` 与后端 `Result.success(code=200)` 冲突，全站数据被拒。改为 `code === 200 || code === 0`。
- FIX#2：`goal.js` 的 `listRecordsByDate` 调 `/goal-records/records/date` 后端无此端点，改为 `/goal-records/records?startDate&endDate` 或 `/records/today`。
- FIX#3：`GoalService.getActiveGoalsWithCustom` 与 `getActiveGoals` 实现相同，需确保自定义目标保存时默认 `isActive=true` 并在查询中合并，使首页任务目标区可取自定义目标。
- FIX#4：实测核查 `HabitRecord.recordDate`(LocalDate) 反序列化与 `sleepDuration`/`value`(BigDecimal) 前端展示兼容性，必要时后端 Jackson 配置或前端 Number 转换。
- FIX#5：启动后端 + 前端 dev，实测各页面数据流，修复暴露的请求/响应问题。

## 设计风格

采用「流动渐变 + 玻璃拟态 + 粒子流动」的现代科技风。以青绿(#0f766e)→靛蓝(#6366f1)→紫(#a855f7)的多段流动渐变为核心视觉语言，卡片为大圆角半透明玻璃质感，悬浮时渐变描边发光。全局 Canvas 粒子缓慢上浮并与鼠标轻微排斥，任务/趋势视图粒子更密并带连线，营造大气流动感。

## 页面规划（6 屏内）

1. 首页（分块式）：顶部流动渐变 Hero 轮播；4 张渐变统计卡；任务目标进度区（按类型配色进度条）；横向滑动卡片（习惯贴士/快捷）；心情饼图 + 近7天折线趋势双图；底部快捷入口。
2. 趋势分析页：睡眠折线 + 运动柱状 + 饮水折线 + 五维雷达，四块玻璃卡片网格，heavy 粒子背景。
3. 每日打卡页：分区表单（睡眠/饮食/运动/饮水心情），左侧渐变色条，悬浮微动效，轻粒子背景。
4. 历史记录页：玻璃表格卡片，行 hover 渐变高亮，轻粒子背景。
5. AI 建议页：渐变对话气泡，打字机光标，轻粒子背景。
6. 任务目标页（新增）：专属 heavy 粒子背景，目标卡按类型渐变配色，进度环 + 预留新增目标入口空间，整体通透大气。

## 区块设计要点

- 顶部导航：固定渐变栏，当前项底部流动光条，icon 渐变着色。
- 统计卡：每卡独立渐变（睡眠靛蓝/饮水青/运动橙/心情粉），数字大号半透明。
- 饼图/折线：ECharts 配色取自主题渐变，tooltip 玻璃风格。
- 粒子层：fixed 全屏 pointer-events:none，z-index 低于内容。

## Agent Extensions

### Skill

- **frontend-design**
- Purpose: 为分块式首页、渐变主题、玻璃拟态与粒子流动效果提供专业视觉设计指导，避免模板化默认风格。
- Expected outcome: 产出具有高级审美、非套路的视觉方向，应用于所有页面统一风格。

### SubAgent

- **code-explorer**
- Purpose: 在实现前后端代码前深入检索后端 HabitRecordRepository/GoalService 既有方法签名与字段，并使用 [skill:frontend-design] 指导视觉方向，确保 AnalysisService 计算逻辑与前端接口封装准确复用，不引入新查询。
- Expected outcome: 确认既有方法签名与返回类型，输出可供 AnalysisService 与前端 api 直接调用的依赖清单。