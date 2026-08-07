package com.habit.agent.controller;

import com.habit.agent.common.result.Result;
import com.habit.agent.service.ChatSessionService;
import com.habit.agent.vo.ChatSessionVO;
import com.habit.agent.vo.MessageVO;
import com.habit.agent.vo.SessionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 会话管理 REST API。
 *
 * <p>路径 {@code /api/sessions} 对齐开发文档第 2 节；本控制器仅负责「会话」(sessionId)，
 * 流式对话见 {@code ChatController}（使用 conversationId，两者概念不同，见 1.6 节）。</p>
 */
@Slf4j
@Tag(name = "会话管理", description = "生活习惯助手会话的创建（sessionId，非对话 conversationId）")
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final ChatSessionService chatSessionService;

    /**
     * POST /api/sessions — 新建会话
     *
     * @param num 首屏返回的快捷示例条数（默认 3）
     */
    @Operation(summary = "新建会话", description = "生成唯一 sessionId 并持久化，返回首屏助手介绍与随机抽取的快捷示例")
    @PostMapping
    public Result<SessionVO> createSession(
            @Parameter(description = "快捷示例条数", example = "3")
            @RequestParam(value = "n", defaultValue = "3") Integer num) {
        return Result.success(chatSessionService.createSession(num));
    }

    /**
     * GET /api/sessions/hot — 获取热门问题
     *
     * @param num 返回的热门问题条数（默认 3）
     */
    @Operation(summary = "获取热门问题", description = "从配置的热门问题列表中随机抽取 num 条，用于前端首屏「热门问题」榜")
    @GetMapping("/hot")
    public Result<List<SessionVO.HotQuestion>> hotQuestions(
            @Parameter(description = "热门问题条数", example = "3")
            @RequestParam(value = "n", defaultValue = "3") Integer num) {
        return Result.success(chatSessionService.hotQuestions(num));
    }

    /**
     * GET /api/sessions/{sessionId} — 查询单个历史对话详情
     *
     * @param sessionId 会话 ID
     * @return 对话记录列表（用户提问 + AI 回答，按时间正序）
     */
    @Operation(summary = "查询历史对话详情", description = "根据 sessionId 从会话记忆中读取历史消息，仅返回用户提问与 AI 回答")
    @GetMapping("/{sessionId}")
    public Result<List<MessageVO>> queryBySessionId(
            @Parameter(description = "会话ID", example = "f1dbf6ca0ed34eeda02ec0d0545a4429")
            @PathVariable("sessionId") String sessionId) {
        return Result.success(chatSessionService.queryBySessionId(sessionId));
    }

    /**
     * GET /api/sessions/history — 查询历史会话列表（按更新时间分组）
     *
     * <p>分组维度：当天 / 最近30天 / 最近1年 / 1年以上；仅返回已生成标题的会话，最多 30 条。</p>
     */
    @Operation(summary = "查询历史会话列表", description = "按更新时间将历史会话分组为 当天/最近30天/最近1年/1年以上 返回")
    @GetMapping("/history")
    public Result<Map<String, List<ChatSessionVO>>> queryHistorySession() {
        return Result.success(chatSessionService.queryHistorySession());
    }

    /**
     * DELETE /api/sessions/history — 删除历史会话（同时清空对话记忆）
     */
    @Operation(summary = "删除历史会话", description = "删除指定会话及其对话记忆")
    @DeleteMapping("/history")
    public Result<Void> deleteHistorySession(@RequestParam("sessionId") String sessionId) {
        chatSessionService.deleteHistorySession(sessionId);
        return Result.success();
    }

    /**
     * PUT /api/sessions/history — 更新历史会话标题（手动重命名）
     */
    @Operation(summary = "更新历史会话标题", description = "更新指定会话的标题（最多 100 字）")
    @PutMapping("/history")
    public Result<Void> updateTitle(@RequestParam("sessionId") String sessionId,
                                    @RequestParam("title") String title) {
        chatSessionService.updateTitle(sessionId, title);
        return Result.success();
    }
}
