// =============================================================
// 黑马程序员 · 天机学堂风格 — 生活习惯助手 Agent MongoDB 初始化脚本
// 项目名称 : habit_agent
// 数据库   : MongoDB 7.0+
// 字符集   : utf8
// 说明     : AI 对话 / 会话 / 向量等文档型数据存放 MongoDB（5 个集合 + 11 个索引 + 种子数据）
// 运行方式 : mongosh < mongo-init.js
// 创建人   : developer
// 创建时间 : 2026-08-07
// =============================================================

// 1. 选择数据库
use("habit_agent");

// 2. 初始化前清空已有集合（仅开发 / 初始化使用，生产请删除本段）
const COLLECTIONS = ["chatMessage", "chatSession", "aiAnalysis", "chatMemory", "habit_knowledge"];
COLLECTIONS.forEach(function (c) {
    if (db.getCollectionNames().indexOf(c) !== -1) {
        db[c].drop();
    }
});

// 工具函数：生成占位向量（text-embedding-v3 输出 1024 维；种子数据用零向量，真实数据由后端向量化写入）
function zeroVec(n) {
    return new Array(n).fill(0);
}

// =============================================================
// 3. 种子数据（_init 标记，重复执行安全；集合在首次 insert 时自动创建）
// =============================================================

// 3.1 chatSession —— 会话管理（sessionId，非 conversationId）
//    说明：本地场景会话持久化、不自动删除，故不设置 expireAt 字段、不建 TTL 索引。
db.chatSession.insertOne({
    sessionId: "f1dbf6ca0ed34eeda02ec0d0545a4429",
    userId: 1,
    title: "今日作息记录",
    status: "ACTIVE",
    createTime: new Date("2026-08-07T21:30:12Z"),
    updateTime: new Date("2026-08-07T21:30:12Z")
});

// 3.2 chatMessage —— 对话消息（conversationId 对应「对话」，见开发文档 1.6）
db.chatMessage.insertMany([
    {
        conversationId: "f1dbf6ca0ed34eeda02ec0d0545a4429",
        sessionId: "f1dbf6ca0ed34eeda02ec0d0545a4429",
        userId: 1,
        role: "USER",
        content: "帮我记录今天23点睡觉、喝了2升水",
        createTime: new Date("2026-08-07T21:30:00Z")
    },
    {
        conversationId: "f1dbf6ca0ed34eeda02ec0d0545a4429",
        sessionId: "f1dbf6ca0ed34eeda02ec0d0545a4429",
        userId: 1,
        role: "ASSISTANT",
        content: "已为您记录：23:00 入睡、饮水 2000 毫升。",
        createTime: new Date("2026-08-07T21:30:12Z")
    }
]);

// 3.3 aiAnalysis —— AI 分析结果（DAILY 缓存 1 天，WEEKLY/MONTHLY/CUSTOM 永不过期）
db.aiAnalysis.insertMany([
    {
        userId: 1,
        type: "DAILY",
        recordDate: "2026-08-07",
        summary: "今日睡眠达标，饮水略低于目标。",
        createTime: new Date("2026-08-07T21:00:00Z"),
        expireAt: new Date(Date.now() + 1 * 24 * 60 * 60 * 1000)
    },
    {
        userId: 1,
        type: "WEEKLY",
        days: 7,
        summary: "本周睡眠时长较上周提升，饮水达标率 80%。",
        createTime: new Date("2026-08-07T21:00:00Z")
    }
]);

// 3.4 chatMemory —— Spring AI MongoChatMemoryRepository 会话记忆（由框架管理结构，此处仅种子示例）
db.chatMemory.insertMany([
    {
        conversationId: "f1dbf6ca0ed34eeda02ec0d0545a4429",
        content: [
            { role: "USER", content: "帮我记录今天23点睡觉、喝了2升水" },
            { role: "ASSISTANT", content: "已为您记录：23:00 入睡、饮水 2000 毫升。" }
        ],
        createTime: new Date("2026-08-07T21:30:12Z")
    },
    {
        conversationId: "a7c2e1b40ed34eeda02ec0d0545a8841",
        content: [
            { role: "USER", content: "我最近总是失眠怎么办？" },
            { role: "ASSISTANT", content: "建议固定作息、睡前减少屏幕使用，并参考睡眠指南。" }
        ],
        createTime: new Date("2026-08-06T20:10:00Z")
    }
]);

// 3.5 habit_knowledge —— RAG 知识库向量存储（embedding 为 text-embedding-v3 输出的 1024 维向量）
db.habit_knowledge.insertMany([
    {
        content: "成年人建议每日饮水 1500-2000 毫升，分次少量饮用，避免一次性大量饮水。",
        docType: "diet",
        source: "preset",
        title: "饮水指南",
        chunkIndex: 0,
        embedding: zeroVec(1024)
    },
    {
        content: "成年人建议每日睡眠 7-9 小时，保持规律作息有助于提升睡眠质量。",
        docType: "sleep",
        source: "preset",
        title: "睡眠指南",
        chunkIndex: 0,
        embedding: zeroVec(1024)
    },
    {
        content: "每周进行至少 150 分钟中等强度有氧运动，如快走、慢跑、游泳。",
        docType: "exercise",
        source: "preset",
        title: "运动指南",
        chunkIndex: 0,
        embedding: zeroVec(1024)
    },
    {
        content: "均衡饮食应包含足量蔬菜、水果、优质蛋白与全谷物，减少高糖高油摄入。",
        docType: "diet",
        source: "preset",
        title: "饮食指南",
        chunkIndex: 1,
        embedding: zeroVec(1024)
    },
    {
        content: "睡前 1 小时避免使用电子屏幕，可改为阅读或听轻音乐帮助入睡。",
        docType: "sleep",
        source: "preset",
        title: "睡眠指南",
        chunkIndex: 1,
        embedding: zeroVec(1024)
    }
]);

// =============================================================
// 4. 索引（11 个）
// =============================================================

// 4.1 chatMessage —— 按会话查询消息 / 按用户查询历史
db.chatMessage.createIndex({ conversationId: 1, createTime: 1 }, { name: "idx_conv_time" });
db.chatMessage.createIndex({ userId: 1, createTime: 1 }, { name: "idx_user_time" });

// 4.2 chatSession —— 会话 ID 唯一 / 查询用户会话
//    说明：本地场景会话持久化、不自动删除，故不建 TTL 过期索引（无 expireAt 字段）；
//          实体无 status 字段，故复合索引改为 userId 单字段索引 idx_user。
db.chatSession.createIndex({ sessionId: 1 }, { name: "uk_session_id", unique: true });
db.chatSession.createIndex({ userId: 1 }, { name: "idx_user" });

// 4.3 aiAnalysis —— 按用户和类型查询 / 每日评价按日期查询 / DAILY 1 天缓存（sparse TTL）
db.aiAnalysis.createIndex({ userId: 1, type: 1, createTime: 1 }, { name: "idx_user_type_time" });
db.aiAnalysis.createIndex({ userId: 1, recordDate: 1 }, { name: "idx_user_record_date" });
db.aiAnalysis.createIndex({ expireAt: 1 }, { name: "ttl_expire_at", expireAfterSeconds: 0, sparse: true });

// 4.4 chatMemory —— 按会话查询 / 删除消息
db.chatMemory.createIndex({ conversationId: 1, createTime: 1 }, { name: "idx_conv_time" });

// 4.5 habit_knowledge —— 按文档类型和来源过滤 + 向量检索索引
db.habit_knowledge.createIndex({ docType: 1, source: 1 }, { name: "idx_metadata" });
// 向量检索索引须使用 Atlas Vector Search（或 MongoDB 6.0.11+/7.0+ 的 createSearchIndex）。
// 本地 Community 版不支持，请按 README 在 Atlas 手动创建（名称：habit_vector_index，path：embedding，numDimensions：1024，similarity：cosine）。
try {
    db.habit_knowledge.createSearchIndex({
        name: "habit_vector_index",
        type: "vectorSearch",
        definition: {
            fields: [
                { type: "vector", path: "embedding", numDimensions: 1024, similarity: "cosine" }
            ]
        }
    });
    print("向量检索索引 habit_vector_index 创建成功（或已存在）。");
} catch (e) {
    print("跳过向量检索索引创建（本地 Community 版不支持 createSearchIndex，请在 Atlas 手动创建）：" + e.message);
}

// =============================================================
// 5. 验证
// =============================================================
print("chatSession     count:", db.chatSession.countDocuments());
print("chatMessage     count:", db.chatMessage.countDocuments());
print("aiAnalysis      count:", db.aiAnalysis.countDocuments());
print("chatMemory      count:", db.chatMemory.countDocuments());
print("habit_knowledge count:", db.habit_knowledge.countDocuments());
print("collections     :", db.getCollectionNames());
