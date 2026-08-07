import request from '@/utils/request'

// 对应后端 RagController 的接口：/api/rag/*
// 底层为 MongoDB Atlas Vector Search，文档经分块 + 向量化后存入 habit_knowledge 集合。
// 注意：预设知识（sql/knowledge/*.md）通过后端初始化脚本 init-knowledge.mjs
// 调用 /api/embedding 写入，并非由前端的「导入预设」按钮完成；
// 下方 importDocuments 用于导入调用方自行提供的文本（对齐后端 importKnowledge 的必填 messages 参数）。

/**
 * 导入自定义知识文档（文本列表）。
 * 对齐后端 RagController.importKnowledge：POST /api/rag/import?messages=...&docType=
 * @param {string[]} messages 文档文本片段（必填，后端要求非空）
 * @param {string} [docType] 可选分类（sleep/exercise/diet/custom）
 */
export function importDocuments(messages, docType) {
  return request.post('/rag/import', null, {
    params: { messages, ...(docType ? { docType } : {}) },
    timeout: 120000,
  })
}

/**
 * 语义检索知识库，返回 Top-K 片段及相似度得分。
 * 对齐后端 RagController.searchKnowledge：GET /api/rag/search?message=&topK=
 * @param {string} query 检索问句
 * @param {number} [topK=3] 返回片段数
 */
export function searchKnowledge(query, topK = 3) {
  return request.get('/rag/search', { params: { message: query, topK } })
}

/** 查询已入库片段列表，docType 可选（sleep/exercise/diet/custom） */
export function listDocuments(docType) {
  return request.get('/rag/documents', { params: docType ? { docType } : {} })
}

/**
 * 按片段 ID 列表删除知识库文档。
 * 对齐后端 RagController.deleteDocuments：DELETE /api/rag/documents?ids=
 * @param {string[]} ids 片段 ID 列表
 */
export function deleteDocuments(ids) {
  return request.delete('/rag/documents', { params: { ids } })
}
