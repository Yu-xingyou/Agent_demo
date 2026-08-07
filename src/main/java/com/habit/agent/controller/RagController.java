package com.habit.agent.controller;

import java.util.List;
import java.util.Map;

import cn.hutool.core.collection.CollStreamUtil;
import com.habit.agent.common.vo.ImportResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识库 RAG 接口（PRD 阶段八）。
 *
 * <p>围绕 MongoDB Atlas 向量库（集合 {@code habit_knowledge}）提供知识的管理与检索能力，
 * 与对话链路中的 {@code RetrievalAugmentationAdvisor} 共用同一向量库。
 * 语义检索参数（topK / 相似度阈值 / 过滤）采用 Spring AI 2.0 的
 * {@link SearchRequest}（{@code query/topK/similarityThreshold/filterExpression}）。</p>
 *
 * <p>端点的设计参照 /api/embedding 的批量写入约定，并扩展出导入、检索、列举、删除四类操作，
 * 供知识库初始化脚本（sql/knowledge/init-knowledge.mjs）与后台管理页调用。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
@Tag(name = "知识库 RAG", description = "健康科普知识库的导入、语义检索、列举与删除")
public class RagController {

    /** 文档元数据中记录知识分类的键（与 ChatServiceImpl 的 RAG 检索、导入脚本一致） */
    private static final String DOC_TYPE_KEY = "docType";

    private final VectorStore vectorStore;

    /**
     * POST /api/rag/import — 批量导入知识文本（带分类）。
     *
     * @param messages 文本片段列表（每条作为一篇独立 Document）
     * @param docType  知识分类（sleep / exercise / diet，可选；null 表示未分类）
     */
    @Operation(summary = "批量导入知识", description = "将知识文本写入向量库，可附 docType 分类元数据")
    @PostMapping("/import")
    public ImportResultVO importKnowledge(
            @RequestParam("messages") List<String> messages,
            @RequestParam(value = "docType", required = false) String docType) {

        log.info("导入知识库，文档数：{}，docType：{}", messages.size(), docType);
        List<Document> documents = CollStreamUtil.toList(messages, message ->
                Document.builder()
                        .text(message)
                        .metadata(docType == null || docType.isBlank()
                                ? Map.of()
                                : Map.of(DOC_TYPE_KEY, docType.trim().toLowerCase()))
                        .build());

        int chunkCount = documents.size();
        try {
            this.vectorStore.add(documents);
            log.info("导入知识库成功，分块数：{}", chunkCount);
            return ImportResultVO.builder()
                    .totalDocs(messages.size())
                    .totalChunks(chunkCount)
                    .success(true)
                    .errors(List.of())
                    .build();
        } catch (Exception e) {
            log.error("导入知识库失败：{}", e.getMessage(), e);
            return ImportResultVO.builder()
                    .totalDocs(messages.size())
                    .totalChunks(0)
                    .success(false)
                    .errors(List.of(e.getMessage()))
                    .build();
        }
    }

    /**
     * GET /api/rag/search — 语义检索知识。
     *
     * @param message 检索问句
     * @param docType 按分类过滤（可选；sleep / exercise / diet）
     * @param topK    返回条数（默认 5，最大 20）
     */
    @Operation(summary = "语义检索知识", description = "基于向量相似度检索相关知识，可按 docType 过滤")
    @GetMapping("/search")
    public List<Document> search(
            @RequestParam("message") String message,
            @RequestParam(value = "docType", required = false) String docType,
            @RequestParam(value = "topK", defaultValue = "5") Integer topK) {

        int k = Math.max(1, Math.min(topK == null ? 5 : topK, 20));
        SearchRequest.Builder reqBuilder = SearchRequest.builder()
                .query(message)
                .topK(k);

        if (docType != null && !docType.isBlank()) {
            Filter.Expression expr = new Filter.Expression(
                    Filter.ExpressionType.EQ,
                    new Filter.Key(DOC_TYPE_KEY),
                    new Filter.Value(docType.trim().toLowerCase()));
            reqBuilder.filterExpression(expr);
        }

        return this.vectorStore.similaritySearch(reqBuilder.build());
    }

    /**
     * GET /api/rag/documents — 列举知识库全部文档（用于管理页校验）。
     */
    @Operation(summary = "列举全部知识", description = "返回向量库中的全部文档（topK 拉全量）")
    @GetMapping("/documents")
    public List<Document> listDocuments() {
        return this.vectorStore.similaritySearch(
                SearchRequest.builder().query("").topK(999).build());
    }

    /**
     * DELETE /api/rag/documents — 按文档 ID 删除知识。
     *
     * @param ids 待删除的文档 ID 列表
     */
    @Operation(summary = "删除知识", description = "按文档 ID 批量删除向量库中的知识")
    @DeleteMapping("/documents")
    public void deleteDocuments(@RequestParam("ids") List<String> ids) {
        log.info("删除知识库文档，ids：{}", ids);
        this.vectorStore.delete(ids);
    }
}
