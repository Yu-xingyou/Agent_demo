package com.habit.agent.controller;

import com.habit.agent.common.result.Result;
import com.habit.agent.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 知识库接口（RAG，远程 Atlas MongoDB 向量库，对齐前端 rag.js）
 */
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    /**
     * 导入预设知识库（幂等）
     *
     * @return 导入结果统计（已导入/已跳过数量等）
     */
    @PostMapping("/import")
    public Result<Map<String, Object>> importPresetDocs() {
        return Result.success(ragService.importPresetDocs());
    }

    /**
     * 上传自定义知识文档（.md/.txt）
     *
     * @param file 上传的知识文档（仅支持 .md/.txt）
     * @return 上传结果（文档 id/分片数等）
     * @throws Exception 当文件读取或向量化失败时抛出
     */
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) throws Exception {
        return Result.success(ragService.uploadDocument(file.getOriginalFilename(), file.getBytes()));
    }

    /**
     * 语义检索知识库
     *
     * @param query 检索文本
     * @param topK  返回的最相似片段数量，默认 3
     * @return 命中片段列表（含文本与相似度分数）
     */
    @GetMapping("/search")
    public Result<List<Map<String, Object>>> search(@RequestParam String query,
                                                    @RequestParam(defaultValue = "3") int topK) {
        return Result.success(ragService.search(query, topK));
    }

    /**
     * 已入库片段列表
     *
     * @param docType 文档类型过滤（可选，为空返回全部）
     * @return 已入库知识片段概览列表
     */
    @GetMapping("/documents")
    public Result<List<Map<String, Object>>> listDocuments(@RequestParam(required = false) String docType) {
        return Result.success(ragService.listDocuments(docType));
    }

    /**
     * 删除知识片段
     *
     * @param id 知识片段 id
     * @return 统一成功响应
     */
    @DeleteMapping("/documents/{id}")
    public Result<Void> deleteDocument(@PathVariable("id") String id) {
        ragService.deleteDocument(id);
        return Result.success();
    }
}
