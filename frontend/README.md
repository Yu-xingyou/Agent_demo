# 习惯记 · 前端 (habit-agent-web)

生活习惯助手 Agent 的前端工程，**前后端分离架构**。对应后端 `agent_demo`（Spring Boot 3.5.3 提供 `/api` REST + SSE 流式接口）。

## 技术栈

- Vue 3 + Vite 5
- Pinia 2（状态管理）
- Vue Router 4（9 个路由页面）
- Axios 1（统一封装 `/api`，解析 `Result{code,message,data}`）
- Element Plus 2 + ECharts 5（vue-echarts）
- Tailwind CSS 3.4.17（还原「习惯记」青绿渐变视觉风格）

## 目录结构

```
src/
├── api/           # 接口封装：habit / goal / reminder / analysis / aiAnalysis
│                  #          / chat（对话） / session（会话） / rag（知识库）
├── layouts/       # MainLayout 渐变导航栏 + 路由出口
├── router/        # 9 路由（/ /checkin /history /trend /ai-chat /ai-advice /ai-analysis /reminder /404）
├── stores/        # Pinia 状态
├── utils/         # request.js Axios 单例
└── views/         # 9 个页面
```

## 本地启动

```bash
npm install
npm run dev      # http://localhost:5173
```

开发期 Vite 已配置 `server.proxy['/api'] -> http://localhost:8080`，无需额外处理跨域。
请先启动后端 `agent_demo`（默认 8080 端口）。

## 生产构建

```bash
npm run build    # 产物输出 dist/
npm run preview
```

生产环境跨域由后端 `CorsConfig` 或反向代理处理。

## 页面与接口映射

| 页面 | 路由 | 调用接口 |
| --- | --- | --- |
| 首页 | `/` | `GET /api/habits/today`、`/api/habits/recent/7`、`/api/goals/active-with-custom` |
| 打卡 | `/checkin` | `GET /api/habits/today`、`POST /api/habits` |
| 历史 | `/history` | `GET /api/habits?startDate=&endDate=` |
| 趋势 | `/trend` | `GET /api/analysis/**`（ECharts 统计图表） |
| AI 建议 | `/ai-chat` | `POST /api/chat`（SSE 流式）、`/api/session/**` |
| AI 分析 | `/ai-analysis` | `GET /api/ai-analysis/**`（AI 每日/周期评价） |
| AI 建议页 | `/ai-advice` | `GET /api/rag/**`、`/api/ai-analysis/**` |
| 提醒 | `/reminder` | `GET/POST/PUT/DELETE /api/reminder` |

> 后端 AI 接口（chat / session / rag / ai-analysis）已由 Spring AI 实现并通过 SSE 流式推送，前端 `api/chat.js`、`api/session.js`、`api/rag.js`、`api/aiAnalysis.js` 已封装对应调用。
