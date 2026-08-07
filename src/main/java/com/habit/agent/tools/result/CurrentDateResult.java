package com.habit.agent.tools.result;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 当前日期工具返回结果（对应 PRD T12）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentDateResult {

    @JsonPropertyDescription("当前日期，格式 yyyy-MM-dd")
    private LocalDate date;
    @JsonPropertyDescription("中文星期，例如 星期一")
    private String weekday;
    @JsonPropertyDescription("星期数字，1-7（1=周一）")
    private Integer weekdayNum;
}
