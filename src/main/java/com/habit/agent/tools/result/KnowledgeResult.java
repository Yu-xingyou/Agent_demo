package com.habit.agent.tools.result;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 健康知识检索返回结果（对应 PRD T10）。
 *
 * <p>当前为本地健康常识库检索（RAG 接入前过渡实现），返回带相关度的科普建议片段。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeResult {

    @JsonPropertyDescription("知识类型：SLEEP/EXERCISE/WATER/DIET")
    private String docType;
    @JsonPropertyDescription("知识内容（科普建议）")
    private String content;
    @JsonPropertyDescription("相关度评分，0-1，越大越相关")
    private Double score;
}
