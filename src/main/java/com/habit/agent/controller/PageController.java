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
 * 页面路由控制器（子模块 3-1 / 3-2）
 *
 * 负责 Thymeleaf 页面渲染，非 REST API。
 * 所有页面数据均从后端 Service 获取后注入 Model，前端不硬编码任何数据。
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

    /**
     * GET /checkin — 每日打卡页
     * 注入今日记录（已打卡则预填表单）
     */
    @GetMapping("/checkin")
    public String checkin(Model model) {
        HabitRecordVO todayRecord = habitService.getTodayRecord(null);
        model.addAttribute("todayRecord", todayRecord);
        log.info("打卡页加载: todayRecord={}", todayRecord != null ? todayRecord.getRecordDate() : "null");
        return "checkin";
    }

    /**
     * GET /history — 历史记录页
     * 注入最近 30 天记录（用于表格和图表）
     */
    @GetMapping("/history")
    public String history(Model model) {
        List<HabitRecordVO> records = habitService.getRecentRecords(null, 30);
        model.addAttribute("records", records);
        log.info("历史页加载: records={}", records.size());
        return "history";
    }
}
