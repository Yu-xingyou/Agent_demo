package com.habit.agent.common.vo;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库导入结果（阶段八）
 *
 * <p>用于 /api/rag/import 与 /api/rag/upload 的统一返回。
 * 单篇文档失败不中断整体导入，失败原因收集在 {@code errors} 中。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResultVO {

    @Schema(description = "成功导入的文档数", example = "3")
    private Integer totalDocs;

    @Schema(description = "生成并入库的文本分块数", example = "18")
    private Integer totalChunks;

    @Schema(description = "是否全部成功（存在任一失败即为 false）", example = "true")
    private Boolean success;

    @Schema(description = "失败原因列表，全部成功时为空数组")
    private List<String> errors;
}
