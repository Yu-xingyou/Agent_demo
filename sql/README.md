# 数据库初始化说明

## 数据存储架构

本项目采用 **MySQL + MongoDB** 双数据库方案：

| 数据库 | 存储内容 | 表/集合数 |
|---|---|---|
| MySQL 8.0+ | 关系型业务数据（用户、习惯记录、目标、提醒） | 4 张表 |
| MongoDB 7.0+ | 文档型 AI 数据（对话记忆、会话管理、AI 分析结果、RAG 向量） | 5 个集合 |

## 文件清单

| 文件 | 说明 | 运行方式 |
|---|---|---|
| `schema.sql` | MySQL 建表脚本（4 张表 + 种子数据） | MySQL 客户端直接执行 |
| `mongo-init.js` | MongoDB 初始化脚本（5 个集合 + 11 个索引 + 种子数据） | mongosh 执行 |

## MySQL 初始化

### 方式一：命令行

```bash
mysql -u root -p < schema.sql
```

### 方式二：MySQL 客户端（Navicat / DataGrip / MySQL Workbench）

1. 连接 MySQL
2. 打开 `schema.sql` 文件
3. 全选执行

### 方式三：MySQL 命令行内

```sql
SOURCE d:/javacode/agent_demo/sql/schema.sql;
```

### MySQL 表结构

| 表名 | 说明 | 核心字段 |
|---|---|---|
| `user` | 用户表 | id, username, nickname, create_time |
| `habit_record` | 习惯记录表（每日打卡核心数据） | user_id, record_date, sleep_time, wake_time, sleep_duration, sleep_quality, diet_desc, diet_score, exercise_type, exercise_duration, water_intake, mood, remark |
| `habit_goal` | 习惯目标表（达成率统计基准） | user_id, goal_type, target_value, unit, period, is_active, create_time, update_time |
| `reminder` | 打卡提醒表 | user_id, title, reminder_time, reminder_type, weekdays, is_active |

### 种子数据

- 1 个默认用户（id=1, username=demo）
- 4 个默认习惯目标（SLEEP/EXERCISE/WATER/DIET 各一条）
- 2 个默认打卡提醒（睡眠提醒、饮水提醒）
- 7 天示例打卡数据（供前端页面和图表展示）

## MongoDB 初始化

### 方式一：mongosh 命令行

```bash
mongosh < mongo-init.js
```

### 方式二：mongosh 内执行

```javascript
load("d:/javacode/agent_demo/sql/mongo-init.js")
```

### MongoDB 集合结构

| 集合名 | 说明 | TTL | 对应 Java 实体 |
|---|---|---|---|
| `chatMessage` | 对话消息 | 无 | ChatMessageDoc |
| `chatSession` | 会话管理 | 7 天自动过期 | ChatSessionDoc |
| `aiAnalysis` | AI 分析结果 + 每日评价 | DAILY 1天缓存；WEEKLY/MONTHLY/CUSTOM 永不过期 | AiAnalysisDoc |
| `chatMemory` | Spring AI ChatMemoryRepository 存储 | 无 | 由 MongoChatMemoryRepository 管理 |
| `habit_knowledge` | RAG 知识库向量存储 | 无 | Spring AI VectorStore 管理 |

### 索引说明

| 集合 | 索引名 | 类型 | 用途 |
|---|---|---|---|
| chatMessage | idx_conv_time | 复合索引 | 按会话查询消息 |
| chatMessage | idx_user_time | 复合索引 | 按用户查询消息历史 |
| chatSession | uk_conversation_id | 唯一索引 | 会话 ID 唯一 |
| chatSession | idx_user_status | 复合索引 | 查询用户活跃会话 |
| chatSession | ttl_expire_at | TTL 索引 | 7 天自动过期 |
| aiAnalysis | idx_user_type_time | 复合索引 | 按用户和类型查询 |
| aiAnalysis | idx_user_record_date | 复合索引 | 每日评价按日期查询 |
| aiAnalysis | ttl_expire_at | TTL 索引 (sparse) | DAILY 1天缓存过期，WEEKLY/MONTHLY/CUSTOM 永不过期 |
| chatMemory | idx_conv_time | 复合索引 | 按会话查询/删除消息 |
| habit_knowledge | idx_metadata | 复合索引 | 按文档类型和来源过滤 |
| habit_knowledge | habit_vector_index | 向量索引 | RAG 语义检索（Atlas 配置） |

### MongoDB Atlas Vector Search 索引配置

如果使用 MongoDB Atlas，需要手动创建向量搜索索引：

1. 进入 Atlas Console → Collections → habit_agent → habit_knowledge
2. 点击 "Search Indexes" → "Create Search Index"
3. 选择 "Atlas Vector Search" 类型
4. 索引定义 JSON：

```json
{
  "fields": [
    {
      "type": "vector",
      "path": "embedding",
      "numDimensions": 1024,
      "similarity": "cosine"
    }
  ]
}
```

5. 索引名称设为 `habit_vector_index`

> 注意：DashScope text-embedding-v3 模型输出 1024 维向量。
> 如果使用本地 MongoDB Community（不支持 Atlas Vector Search），可在 Spring AI 中配置降级为手动余弦相似度计算。

## 验证

### MySQL 验证

```sql
USE habit_agent;
SELECT COUNT(*) FROM user;           -- 预期: 1
SELECT COUNT(*) FROM habit_goal;     -- 预期: 4
SELECT COUNT(*) FROM habit_record;   -- 预期: 7
SELECT COUNT(*) FROM reminder;       -- 预期: 2
```

### MongoDB 验证

```javascript
use habit_agent;
db.chatSession.countDocuments();     // 预期: 1
db.chatMessage.countDocuments();     // 预期: 2
db.aiAnalysis.countDocuments();      // 预期: 2
db.chatMemory.countDocuments();      // 预期: 2
db.habit_knowledge.countDocuments(); // 预期: 5
db.getCollectionNames();             // 预期: 5 个集合
```
