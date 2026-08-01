package com.habit.agent.controller;

import com.habit.agent.common.vo.HabitRecordVO;
import com.habit.agent.service.HabitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * 页面路由控制器（子模块 3-1）
 *
 * 负责 Thymeleaf 页面渲染，非 REST API。
 * 数据通过 HabitService 获取并注入 Model 供模板使用。
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PageController {

    private final HabitService habitService;

    /**
     * GET / — 首页
     * 注入今日记录和最近 7 天记录
     */
    @GetMapping("/")
    public String index(Model model) {
        HabitRecordVO todayRecord = habitService.getTodayRecord(null);
        List<HabitRecordVO> recentRecords = habitService.getRecentRecords(null, 7);

        model.addAttribute("todayRecord", todayRecord);
        model.addAttribute("recentRecords", recentRecords);
        log.info("首页加载: todayRecord={}, recentRecords={}",
                todayRecord != null ? todayRecord.getRecordDate() : "null",
                recentRecords.size());
        return "index";
    }
}
