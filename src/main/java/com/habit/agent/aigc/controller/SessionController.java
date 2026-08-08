package com.habit.agent.aigc.controller;

import com.habit.agent.aigc.service.ChatSessionService;
import com.habit.agent.aigc.vo.MessageVO;
import com.habit.agent.aigc.vo.SessionVO;
import com.habit.agent.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会话接口（扩展列表/删除）
 */
@RestController
@RequestMapping("/session")
@RequiredArgsConstructor
public class SessionController {

    private final ChatSessionService chatSessionService;

    /**
     * 新建会话
     */
    @PostMapping
    public Result<SessionVO> createSession(@RequestParam(value = "n", defaultValue = "3") Integer num) {
        return Result.success(this.chatSessionService.createSession(num));
    }

    /**
     * 获取热门示例
     */
    @GetMapping("/hot")
    public Result<List<SessionVO.Example>> hotExamples(@RequestParam(value = "n", defaultValue = "3") Integer num) {
        return Result.success(this.chatSessionService.hotExamples(num));
    }

    /**
     * 会话列表（按最后消息时间倒序）
     */
    @GetMapping("/list")
    public Result<List<SessionVO>> listSessions() {
        return Result.success(this.chatSessionService.listSessions());
    }

    /**
     * 查询单个会话历史消息
     */
    @GetMapping("/{sessionId}")
    public Result<List<MessageVO>> queryBySessionId(@PathVariable("sessionId") String sessionId) {
        return Result.success(this.chatSessionService.queryBySessionId(sessionId));
    }

    /**
     * 删除会话（级联清理）
     */
    @DeleteMapping("/{sessionId}")
    public Result<Void> deleteSession(@PathVariable("sessionId") String sessionId) {
        this.chatSessionService.deleteSession(sessionId);
        return Result.success();
    }
}
