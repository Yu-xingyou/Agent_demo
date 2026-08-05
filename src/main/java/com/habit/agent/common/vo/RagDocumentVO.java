package com.habit.agent.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库文档片段（阶段八）
 *
 * <p>用于 /api/rag/documents 列表返回。
 * 出于性能考虑，查询时已排除 1024 维 embedding 字段，本 VO 不包含向量数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagDocumentVO {

    @Schema(description = "片段ID（向量库文档主键）", example = "0f2c1a3e-...")
    private String id;

    @Schema(description = "片段内容预览")
    private String content;

    @Schema(description = "知识类型（sleep/exercise/diet/custom）", example = "sleep")
    private String docType;

    @Schema(description = "来源文件名", example = "sleep-guide.md")
    private String source;

    @Schema(description = "该片段在原文中的序号（从 0 开始）", example = "2")
    private Integer chunkIndex;

    @Schema(description = "入库时间", example = "2026-08-05T10:12:33")
    private String importTime;
}
