package com.habit.agent.controller;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.common.result.Result;
import com.habit.agent.common.vo.ImportResultVO;
import com.habit.agent.common.vo.RagDocumentVO;
import com.habit.agent.common.vo.RagSearchResultVO;
import com.habit.agent.service.RagService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RAG 知识库接口（阶段八）
 *
 * <p>提供预设文档导入、自定义文档上传、语义检索、片段列表与删除五个端点，
 * 底层依托 MongoDB Atlas Vector Search。
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rag")
@Tag(name = "RAG 知识库", description = "健康知识库导入/上传/检索/列表/删除接口")
public class RagController {

    private final RagService ragService;

    @PostMapping("/import")
    @Operation(summary = "导入预设知识库", description = "导入 classpath:rag-docs/ 下的睡眠/运动/饮食指南，重复调用幂等")
    public Result<ImportResultVO> importPresetDocs() {
        return Result.success(ragService.importPresetDocs());
    }

    @PostMapping("/upload")
    @Operation(summary = "上传知识文档", description = "上传自定义 .md / .txt 文档入库，大小不超过 2MB")
    public Result<ImportResultVO> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(ragService.uploadDocument(file));
    }

    @GetMapping("/search")
    @Operation(summary = "知识库检索", description = "按语义相似度返回 Top-K 知识片段及得分")
    public Result<List<RagSearchResultVO>> search(
            @RequestParam @NotBlank(message = "检索内容不能为空") String query,
            @RequestParam(defaultValue = "3")
            @Min(value = 1, message = "topK 最小为 1")
            @Max(value = AgentConstants.RAG_MAX_TOP_K, message = "topK 最大为 10") int topK) {
        return Result.success(ragService.search(query, topK));
    }

    @GetMapping("/documents")
    @Operation(summary = "知识片段列表", description = "查询已入库片段，可按 docType（sleep/exercise/diet/custom）过滤")
    public Result<List<RagDocumentVO>> documents(
            @RequestParam(required = false) String docType) {
        return Result.success(ragService.listDocuments(docType));
    }

    @DeleteMapping("/documents/{id}")
    @Operation(summary = "删除知识片段", description = "按片段 ID 从向量库中删除")
    public Result<Void> deleteDocument(@PathVariable String id) {
        ragService.deleteDocument(id);
        return Result.success();
    }
}
