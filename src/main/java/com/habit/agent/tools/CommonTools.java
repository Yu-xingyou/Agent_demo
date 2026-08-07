package com.habit.agent.tools;

import java.time.DayOfWeek;
import java.time.LocalDate;

import com.habit.agent.tools.constant.ToolConstants;
import com.habit.agent.tools.result.CurrentDateResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * 基础工具（对应 PRD 第 10 节 T12 get_current_date）。
 *
 * <p>供 LLM 推断"今天/昨天/本周"等时间表述，避免依赖训练知识猜测日期。</p>
 */
@Slf4j
@Component
public class CommonTools {

    @Tool(description = ToolConstants.Tools.GET_CURRENT_DATE)
    public CurrentDateResult getCurrentDate() {
        LocalDate now = LocalDate.now();
        // ISO 星期（1=Monday..7=Sunday）→ 中文星期
        int iso = now.getDayOfWeek().getValue();
        String weekday = switch (iso) {
            case 1 -> "星期一";
            case 2 -> "星期二";
            case 3 -> "星期三";
            case 4 -> "星期四";
            case 5 -> "星期五";
            case 6 -> "星期六";
            default -> "星期日";
        };
        return CurrentDateResult.builder()
                .date(now)
                .weekday(weekday)
                .weekdayNum(iso)
                .build();
    }
}
