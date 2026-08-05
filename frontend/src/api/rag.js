import request from '@/utils/request'

// 对应后端 RagController 的阶段八接口：/api/rag/*
// 底层为 MongoDB Atlas Vector Search，文档经分块 + 向量化后存入 habit_knowledge 集合。

/** 导入 classpath:rag-docs/ 下的预设健康知识文档（幂等，重复调用不会重复膨胀） */
export function importPresetDocs() {
  // 向量化耗时较长（需批量调用 Embedding 接口），单独放宽超时
  return request.post('/rag/import', null, { timeout: 120000 })
}

/** 上传自定义知识文档（.md / .txt，不超过 2MB） */
export function uploadDocument(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/rag/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000,
  })
}

/** 语义检索知识库，返回 Top-K 片段及相似度得分 */
export function searchKnowledge(query, topK = 3) {
  return request.get('/rag/search', { params: { query, topK } })
}

/** 查询已入库片段列表，docType 可选（sleep/exercise/diet/custom） */
export function listDocuments(docType) {
  return request.get('/rag/documents', { params: docType ? { docType } : {} })
}

/** 按片段 ID 删除知识库文档 */
export function deleteDocument(id) {
  return request.delete(`/rag/documents/${id}`)
}
