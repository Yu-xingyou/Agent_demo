package com.habit.agent.controller;

import java.util.List;

import cn.hutool.core.collection.CollStreamUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识库向量写入接口（RAG 数据入库）。
 *
 * <p>将健康生活科普文本批量写入 MongoDB Atlas 向量库（集合 {@code habit_knowledge}），
 * 供 {@code QuestionAnswerAdvisor} 在对话时做语义检索增强。</p>
 *
 * <p>说明：向量索引（{@code habit_knowledge_vector_index}）由 mongo-init 脚本创建，
 * 本项目 {@code initialize-schema=false} 不自动建索引，故此处仅负责写入文档。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/embedding")
@RequiredArgsConstructor
public class EmbeddingController {

    private final VectorStore vectorStore;

    /**
     * 批量写入知识文本到向量库。
     *
     * @param messages 原始文本列表（每条作为一篇独立 Document）
     */
    @PostMapping
    public void saveVectorStore(@RequestParam("messages") List<String> messages) {
        log.info("写入向量库，文档数量：{}", messages.size());
        // 构建文档（每条文本一篇 Document）
        List<Document> documents = CollStreamUtil.toList(messages, message ->
                Document.builder().text(message).build());
        // 存储到 MongoDB 向量库
        this.vectorStore.add(documents);
        log.info("写入向量库成功，数量：{}", messages.size());
    }
}
