package com.habit.agent.service;

import java.util.List;
import java.util.Map;

/**
 * 知识库服务接口（RAG，远程 Atlas MongoDB 向量库）
 */
public interface RagService {

    /**
     * 导入 classpath:rag-docs/ 预设健康知识文档（幂等，已导入的文件跳过）
     *
     * @return 导入结果统计（已导入/已跳过数量等）
     */
    Map<String, Object> importPresetDocs();

    /**
     * 上传自定义文档并分块向量化入库
     *
     * @param fileName 文件名（用于 docType 推断与 source 标记）
     * @param content  文件内容
     * @return 上传结果（文档 id/分片数等）
     */
    Map<String, Object> uploadDocument(String fileName, byte[] content);

    /**
     * 语义检索知识库
     *
     * @param query 检索问题
     * @param topK  返回条数
     * @return 命中片段列表（含文本与相似度分数）
     */
    List<Map<String, Object>> search(String query, int topK);

    /**
     * 查询已入库片段列表
     *
     * @param docType 文档类型（sleep/exercise/diet/custom），为空返回全部
     * @return 已入库知识片段概览列表
     */
    List<Map<String, Object>> listDocuments(String docType);

    /**
     * 按片段 id 删除文档
     *
     * @param id 知识片段 id
     */
    void deleteDocument(String id);
}
