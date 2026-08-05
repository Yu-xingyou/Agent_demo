package com.habit.agent.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库检索结果片段（阶段八）
 *
 * <p>用于 /api/rag/search 返回，按相似度得分降序排列。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagSearchResultVO {

    @Schema(description = "片段ID（向量库文档主键）", example = "0f2c1a3e-...")
    private String id;

    @Schema(description = "片段正文内容")
    private String content;

    @Schema(description = "相似度得分（0-1，越大越相关）", example = "0.82")
    private Double score;

    @Schema(description = "知识类型（sleep/exercise/diet/custom）", example = "sleep")
    private String docType;

    @Schema(description = "来源文件名", example = "sleep-guide.md")
    private String source;
}
