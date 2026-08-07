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
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    /**
     * 导入预设知识库（幂等）
     */
    @PostMapping("/import")
    public Result<Map<String, Object>> importPresetDocs() {
        return Result.success(ragService.importPresetDocs());
    }

    /**
     * 上传自定义知识文档（.md/.txt）
     */
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) throws Exception {
        return Result.success(ragService.uploadDocument(file.getOriginalFilename(), file.getBytes()));
    }

    /**
     * 语义检索知识库
     */
    @GetMapping("/search")
    public Result<List<Map<String, Object>>> search(@RequestParam String query,
                                                    @RequestParam(defaultValue = "3") int topK) {
        return Result.success(ragService.search(query, topK));
    }

    /**
     * 已入库片段列表
     */
    @GetMapping("/documents")
    public Result<List<Map<String, Object>>> listDocuments(@RequestParam(required = false) String docType) {
        return Result.success(ragService.listDocuments(docType));
    }

    /**
     * 删除知识片段
     */
    @DeleteMapping("/documents/{id}")
    public Result<Void> deleteDocument(@PathVariable("id") String id) {
        ragService.deleteDocument(id);
        return Result.success();
    }
}
