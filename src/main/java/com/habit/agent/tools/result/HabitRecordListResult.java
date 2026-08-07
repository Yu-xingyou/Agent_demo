package com.habit.agent.tools.result;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.habit.agent.common.vo.HabitRecordVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 打卡记录列表查询工具返回结果。
 *
 * <p>封装记录列表与统计概要，便于 LLM 在对话中直接复述关键数值，
 * 无需自行遍历原始列表。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HabitRecordListResult {

    @JsonPropertyDescription("命中的打卡记录数量")
    private Integer total;

    @JsonPropertyDescription("查询到的打卡记录列表，按日期倒序")
    private List<HabitRecordResult> records;

    /**
     * 由 HabitRecordVO 列表转换。
     *
     * @param vos 打卡记录 VO 列表（可为 null 或空）
     * @return 转换后的列表结果；输入为空时返回 total=0 的空结果
     */
    public static HabitRecordListResult of(List<HabitRecordVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return HabitRecordListResult.builder()
                    .total(0)
                    .records(List.of())
                    .build();
        }
        List<HabitRecordResult> records = vos.stream()
                .map(HabitRecordResult::of)
                .toList();
        return HabitRecordListResult.builder()
                .total(records.size())
                .records(records)
                .build();
    }
}
