# 习惯记 · 前端 (habit-agent-web)

生活习惯助手 Agent 的前端工程，**前后端分离架构**，独立 git 仓库。对应后端 `agent_demo`（Spring Boot 4 仅提供 `/api` REST 接口）。

## 技术栈

- Vue 3 + Vite 5
- Pinia 2（状态管理）
- Vue Router 4（5 个路由页面）
- Axios 1（统一封装 `/api`，解析 `Result{code,message,data}`）
- Element Plus 2 + ECharts 5（vue-echarts）
- Tailwind CSS 3.4.17（还原「习惯记」青绿渐变视觉风格）

## 目录结构

```
src/
├── api/           # habit / goal / analysis(预留) / chat(预留) 接口封装
├── layouts/       # MainLayout 渐变导航栏 + 路由出口
├── router/        # 5 路由（/ /checkin /history /trend /ai-chat）
├── stores/        # habit.js Pinia 状态
├── utils/         # request.js Axios 单例
└── views/         # 5 个页面
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
| 趋势 | `/trend` | 预留 ECharts 容器（阶段九统计接口接入） |
| AI 建议 | `/ai-chat` | 预留对话骨架（阶段五 SSE 流式接入） |

> 趋势页与 AI 建议页为本期骨架，对应统计/AI 接口在后续开发阶段实现后自动接入。
