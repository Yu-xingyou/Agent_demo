package com.habit.agent.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.habit.agent.common.result.Result;
import com.habit.agent.common.vo.SessionVO;
import com.habit.agent.service.SessionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * 阶段五（会话管理）SessionController：7 端点，管理 MongoDB 会话元数据（{@code chatSession} 集合）。
 */
@Tag(name = "会话管理", description = "对话会话的列表/创建/详情/重命名/关闭/删除/清理")
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @Operation(summary = "用户会话列表")
    @GetMapping
    public Result<List<SessionVO>> listSessions() {
        return Result.success(sessionService.listSessions());
    }

    @Operation(summary = "创建新会话")
    @PostMapping
    public Result<SessionVO> createSession(@RequestParam(required = false) String title) {
        return Result.success(sessionService.createSession(title));
    }

    @Operation(summary = "查看指定会话")
    @GetMapping("/{conversationId}")
    public Result<SessionVO> getSession(@PathVariable String conversationId) {
        return Result.success(sessionService.getSession(conversationId));
    }

    @Operation(summary = "重命名会话")
    @PutMapping("/{conversationId}/rename")
    public Result<SessionVO> renameSession(
            @PathVariable String conversationId,
            @RequestParam String title) {
        return Result.success(sessionService.renameSession(conversationId, title));
    }

    @Operation(summary = "关闭会话")
    @PostMapping("/{conversationId}/close")
    public Result<Void> closeSession(@PathVariable String conversationId) {
        sessionService.closeSession(conversationId);
        return Result.success();
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/{conversationId}")
    public Result<Void> deleteSession(@PathVariable String conversationId) {
        sessionService.deleteSession(conversationId);
        return Result.success();
    }

    @Operation(summary = "清理过期会话")
    @PostMapping("/clean-expired")
    public Result<Integer> cleanExpiredSessions() {
        return Result.success(sessionService.cleanExpiredSessions());
    }
}
