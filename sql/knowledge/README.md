# 知识库初始化说明

本目录存放 **RAG 健康科普知识库** 的源文档与初始化脚本，供后端向量化后写入 MongoDB 集合 `habit_knowledge`，在对话时通过 `QuestionAnswerAdvisor` 做语义检索增强（见 PRD 第 4、11 节）。

## 目录结构

```
sql/knowledge/
├── init-knowledge.mjs   # 初始化脚本（Node.js，读取 .md 并调用 /api/embedding）
├── README.md            # 本说明
├── sleep-01-健康睡眠时长.md
├── sleep-02-失眠应对.md
├── sleep-03-午睡与作息.md
├── exercise-01-运动推荐量.md
├── exercise-02-久坐与活动.md
├── exercise-03-运动前后饮食.md
├── diet-01-每日饮水.md
├── diet-02-均衡饮食.md
├── diet-03-早餐与三餐规律.md
└── diet-04-情绪与饮食.md
```

## 文档分类（docType）

文件名前缀决定文档类型，与 `KnowledgeTools` / PRD 4.x 对齐：

| 前缀 | docType | 主题 |
|---|---|---|
| `sleep-*` | `sleep` | 睡眠 |
| `exercise-*` | `exercise` | 运动 |
| `diet-*` | `diet` | 饮食 / 饮水 |

> 共 10 篇文档（sleep 3 + exercise 3 + diet 4）。新增知识时，按此命名规范添加 `.md` 即可被脚本自动识别。

## 使用方式

### 前置条件
1. 后端 Spring Boot 已启动（默认 `http://localhost:8080`）。
2. `application.yml` 已配置 MongoDB 向量库连接与 embedding 模型（`text-embedding-v3`）。
3. 已执行 `sql/mongo-init.js`（含 `habit_knowledge` 向量索引）。

### 执行初始化

```bash
# 默认指向 http://localhost:8080
node sql/knowledge/init-knowledge.mjs

# 自定义后端地址
BASE_URL=http://localhost:8080 node sql/knowledge/init-knowledge.mjs
```

脚本会：
1. 扫描目录下所有 `.md`（排除 README 与脚本自身）；
2. 按文件名前缀映射 `docType`；
3. 长文档按 ~400 字分块（保留段落结构）；
4. 调用 `POST /api/embedding?messages=...` 批量写入向量库。

### 验证

```bash
# 检索示例
curl "http://localhost:8080/api/rag/search?query=失眠怎么办&topK=3"

# 文档列表（若已接 RagController）
curl "http://localhost:8080/api/rag/documents"
```

## 幂等说明

`/api/embedding` 为增量写入，重复执行会新增文档片段。首次初始化前建议清空 `habit_knowledge` 集合（`db.habit_knowledge.drop()`），或在接入 `RagController.import` 后使用其去重逻辑。
