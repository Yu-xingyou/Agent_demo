// ============================================================================
// 生活习惯助手 Agent — MongoDB 初始化脚本
// 数据库: habit_agent | 引擎: MongoDB 7.0+
// 技术栈: Spring Boot 4.1 + Spring AI 2.0 + Spring Data MongoDB
// 数据分工: MongoDB 存储文档型 AI 数据，MySQL 存储关系型业务数据
// 生成日期: 2026-07-30
//
// 使用方式:
//   mongosh < mongo-init.js
//   或在 mongosh 中: load("mongo-init.js")
// ============================================================================

use habit_agent;

// ============================================================================
// 集合 1: chatMessage — 对话消息集合
// 存储每一轮对话的完整消息（USER/ASSISTANT/SYSTEM/TOOL）
// 对应 Java 实体: ChatMessageDoc (@Document(collection = "chatMessage"))
// ============================================================================

// 创建集合（如果不存在）
db.createCollection("chatMessage");

// 索引
db.chatMessage.createIndex(
    { "conversationId": 1, "createTime": 1 },
    { name: "idx_conv_time", background: true }
);

db.chatMessage.createIndex(
    { "userId": 1, "createTime": -1 },
    { name: "idx_user_time", background: true }
);

// ============================================================================
// 集合 2: chatSession — 会话管理集合
// 会话元数据 + 状态管理，TTL 索引实现 7 天自动过期
// 对应 Java 实体: ChatSessionDoc (@Document(collection = "chatSession"))
// ============================================================================

db.createCollection("chatSession");

// conversationId 唯一索引
db.chatSession.createIndex(
    { "conversationId": 1 },
    { name: "uk_conversation_id", unique: true, background: true }
);

// userId + status 复合索引（查询用户活跃会话）
db.chatSession.createIndex(
    { "userId": 1, "status": 1 },
    { name: "idx_user_status", background: true }
);

// TTL 索引 — 7 天自动过期（expireAt 字段，expireAfterSeconds=0 表示按文档内时间过期）
db.chatSession.createIndex(
    { "expireAt": 1 },
    { name: "ttl_expire_at", expireAfterSeconds: 0, background: true }
);

// ============================================================================
// 集合 3: aiAnalysis — AI 分析结果集合
// 存储周报/月报/自定义分析 + 每日评价（DAILY 类型含 score 字段）
// TTL 索引实现缓存自动过期（1 天），避免重复调用 AI
// 对应 Java 实体: AiAnalysisDoc (@Document(collection = "aiAnalysis"))
//
// analysisType: WEEKLY / MONTHLY / CUSTOM / DAILY
// WEEKLY/MONTHLY/CUSTOM 使用 periodStart + periodEnd
// DAILY 使用 recordDate + score
// ============================================================================

db.createCollection("aiAnalysis");

// userId + analysisType + createTime 复合索引（按用户和类型查询）
db.aiAnalysis.createIndex(
    { "userId": 1, "analysisType": 1, "createTime": -1 },
    { name: "idx_user_type_time", background: true }
);

// userId + recordDate 索引（每日评价按日期查询）
db.aiAnalysis.createIndex(
    { "userId": 1, "recordDate": -1 },
    { name: "idx_user_record_date", background: true, sparse: true }
);

// TTL 索引 — 缓存 1 天自动过期
db.aiAnalysis.createIndex(
    { "expireAt": 1 },
    { name: "ttl_expire_at", expireAfterSeconds: 0, background: true }
);

// ============================================================================
// 集合 4: chatMemory — Spring AI ChatMemoryRepository 存储
// 自定义 MongoChatMemoryRepository 实现 Spring AI 的 ChatMemoryRepository 接口
// 滑动窗口策略：保留最近 20 条消息，全量替换
// ============================================================================

db.createCollection("chatMemory");

// conversationId 索引（按会话查询/删除消息）
db.chatMemory.createIndex(
    { "conversationId": 1, "createTime": 1 },
    { name: "idx_conv_time", background: true }
);

// ============================================================================
// 集合 5: habit_knowledge — RAG 知识库向量存储
// MongoDB Atlas Vector Search 集合，Spring AI 原生支持
// 文档来源: src/main/resources/rag-docs/ 下的 Markdown 文档
// 处理流程: DocumentReader 读取 → TokenTextSplitter 分块 → Embedding 向量化 → 存储
// ============================================================================

db.createCollection("habit_knowledge");

// metadata 字段索引（按文档类型和来源过滤）
db.habit_knowledge.createIndex(
    { "metadata.doc_type": 1, "metadata.source": 1 },
    { name: "idx_metadata", background: true }
);

// ============================================================================
// 种子数据（开发演示用）
// ============================================================================

// ---- 示例会话 ----
db.chatSession.insertOne({
    conversationId: "demo-session-001",
    userId: NumberLong(1),
    title: "睡眠分析对话",
    status: "ACTIVE",
    messageCount: 2,
    lastMessageTime: new Date(),
    createTime: new Date(Date.now() - 3600000),
    expireAt: new Date(Date.now() + 7 * 24 * 3600000)
});

// ---- 示例对话消息 ----
db.chatMessage.insertMany([
    {
        conversationId: "demo-session-001",
        userId: NumberLong(1),
        role: "USER",
        content: "分析一下我最近一周的睡眠",
        metadata: null,
        createTime: new Date(Date.now() - 3600000)
    },
    {
        conversationId: "demo-session-001",
        userId: NumberLong(1),
        role: "ASSISTANT",
        content: "根据您最近一周的睡眠记录，平均睡眠时长为7.25小时，达标率85.7%。建议每天提前30分钟入睡，保持规律的作息时间。",
        metadata: {
            tokensUsed: 356,
            model: "qwen-plus"
        },
        createTime: new Date()
    }
]);

// ---- 示例 ChatMemory（Spring AI 格式）----
db.chatMemory.insertMany([
    {
        conversationId: "demo-session-001",
        role: "USER",
        content: "分析一下我最近一周的睡眠",
        type: "USER",
        createTime: new Date(Date.now() - 3600000)
    },
    {
        conversationId: "demo-session-001",
        role: "ASSISTANT",
        content: "根据您最近一周的睡眠记录，平均睡眠时长为7.25小时，达标率85.7%。建议每天提前30分钟入睡，保持规律的作息时间。",
        type: "ASSISTANT",
        createTime: new Date()
    }
]);

// ---- 示例 AI 分析结果（周报）----
db.aiAnalysis.insertOne({
    userId: NumberLong(1),
    analysisType: "WEEKLY",
    periodStart: new Date(Date.now() - 7 * 24 * 3600000),
    periodEnd: new Date(),
    recordDate: null,
    content: "## 本周习惯分析\n\n### 睡眠\n平均睡眠时长7.25小时，达标率85.7%。\n### 运动\n本周运动5天，平均运动时长35分钟。\n### 饮水\n平均饮水量1671ml，达标率71.4%。\n### 饮食\n平均饮食评分3.4分，达标率62.9%。",
    score: null,
    suggestion: "1. 建议每天提前30分钟入睡\n2. 增加运动频率至每周5次\n3. 保持每日饮水量2000ml以上\n4. 减少外卖和重油重盐饮食",
    riskWarning: "运动量在周中有2天不足，建议增加日常活动量。饮水在部分天数未达标，建议设置饮水提醒。",
    metadata: {
        tokensUsed: 800,
        model: "qwen-plus"
    },
    createTime: new Date(),
    expireAt: new Date(Date.now() + 24 * 3600000)
});

// ---- 示例每日评价 ----
db.aiAnalysis.insertOne({
    userId: NumberLong(1),
    analysisType: "DAILY",
    periodStart: null,
    periodEnd: null,
    recordDate: new Date(),
    content: "## 今日评价\n\n睡眠时长7.5小时，质量良好。饮食结构均衡，运动量达标。饮水量充足，整体状态优秀。",
    score: 85,
    suggestion: "建议明天保持当前作息节奏，可尝试增加10分钟拉伸运动以进一步提升恢复质量",
    riskWarning: null,
    metadata: {
        tokensUsed: 320,
        model: "qwen-plus"
    },
    createTime: new Date(),
    expireAt: new Date(Date.now() + 24 * 3600000)
});

// ---- 示例 RAG 知识库文档（不含向量，实际向量由 Spring AI EmbeddingModel 生成）----
db.habit_knowledge.insertMany([
    {
        content: "成人每天建议睡眠7-9小时，最佳入睡时间为22:00-23:00。睡眠不足会影响免疫力、记忆力和情绪稳定性。长期睡眠不足增加心血管疾病和代谢综合征风险。",
        metadata: {
            doc_type: "sleep_guide",
            source: "sleep-guide.md",
            chunk_index: 0
        }
        // embedding 字段由 Spring AI 自动生成，此处省略
    },
    {
        content: "睡眠质量评估标准：入睡时间小于30分钟为优秀，夜间醒来不超过1次为良好。深睡眠应占总睡眠的20-25%。改善睡眠建议：保持卧室温度18-22度，睡前1小时避免使用电子设备。",
        metadata: {
            doc_type: "sleep_guide",
            source: "sleep-guide.md",
            chunk_index: 1
        }
    },
    {
        content: "每周推荐中等强度运动150分钟或高强度运动75分钟。运动类型包括有氧运动（跑步、游泳、骑行）和力量训练。建议每周力量训练2-3次，覆盖主要肌群。运动过量警告信号：持续疲劳、睡眠质量下降、静息心率升高。",
        metadata: {
            doc_type: "exercise_guide",
            source: "exercise-guide.md",
            chunk_index: 0
        }
    },
    {
        content: "每日推荐饮水量1500-2000ml，运动时适当增加。饮水不足会导致疲劳、头痛和注意力下降。建议分多次饮水，每次200-300ml，不要等到口渴才喝水。晨起一杯温水有助于唤醒身体机能。",
        metadata: {
            doc_type: "diet_guide",
            source: "diet-guide.md",
            chunk_index: 0
        }
    },
    {
        content: "健康饮食原则：食物多样化，谷物为主；多吃蔬菜水果和薯类；每天吃奶类、大豆或其制品；常吃适量的鱼禽蛋和瘦肉；减少烹调油用量，吃清淡少盐膳食。饮食评分参考：5分为均衡健康，3分为一般，1分为大量外卖和重油重盐。",
        metadata: {
            doc_type: "diet_guide",
            source: "diet-guide.md",
            chunk_index: 1
        }
    }
]);

// ============================================================================
// 验证查询（运行后可执行以下命令确认数据）
// ============================================================================

// print("=== 集合列表 ===");
// printjson(db.getCollectionNames());
// print("\n=== chatSession 示例 ===");
// printjson(db.chatSession.find().toArray());
// print("\n=== chatMessage 示例 ===");
// printjson(db.chatMessage.find().toArray());
// print("\n=== aiAnalysis 示例 ===");
// printjson(db.aiAnalysis.find().toArray());
// print("\n=== habit_knowledge 示例 ===");
// printjson(db.habit_knowledge.find({}, { content: 1, metadata: 1 }).toArray());
// print("\n=== 索引列表 ===");
// db.getCollectionNames().forEach(function(col) {
//     print("\n--- " + col + " indexes ---");
//     printjson(db.getCollection(col).getIndexes());
// });

print("MongoDB 初始化完成！共创建 5 个集合、11 个索引、示例数据已插入。");
