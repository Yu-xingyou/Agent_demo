# habit-agent

生活习惯助手 —— 基于 Spring Boot 4 + Vue 3 的前后端分离应用。

> 当前分支 `agent` 已清理掉原有 Agent / AI 相关代码（对话、RAG、智能分析、会话管理等），
> 仅保留后端业务骨架（习惯 / 目标 / 打卡记录 / 提醒 / 统计分析），便于后续重建 Agent 功能。

## 仓库结构（单仓库 Monorepo）

```
agent_demo/
├── src/                 # 后端：Spring Boot 4 应用（仅提供 /api REST）
├── pom.xml              # 后端构建（Maven）
└── frontend/            # 前端：Vue 3 + Vite 工程
```

前端与后端同仓管理，开发期通过 Vite 代理将 `/api` 转发到后端 8080，跨域由后端 `CorsConfig` 允许 `http://localhost:5173`。

## 快速开始

### 1. 启动后端（端口 8080）

```bash
mvn spring-boot:run
```

> 后端仅提供 `http://localhost:8080/api/**` 的 REST 接口，已彻底移除 Thymeleaf/SSR。
> 需要本地 MySQL（默认 `habit_agent` 库，连接串见 `application-local.yml`）。

### 2. 启动前端（端口 5173）

```bash
cd frontend
npm install
npm run dev
```

浏览器打开 `http://localhost:5173` 即可访问「习惯记」界面。

### 3. 生产构建前端

```bash
cd frontend
npm install
npm run build      # 产物输出到 frontend/dist/
```

## 技术栈

- 后端：Spring Boot 4.1 + Java 21，JPA / MySQL，纯 REST API
- 前端：Vue 3 + Vite 5 + Pinia 2 + Axios + Vue Router 4 + Element Plus 2 + ECharts 5 + Tailwind 3.4.17
