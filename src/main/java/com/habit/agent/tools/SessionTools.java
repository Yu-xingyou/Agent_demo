package com.habit.agent.tools;

import java.util.List;

import com.habit.agent.service.ChatSessionService;
import com.habit.agent.tools.constant.ToolConstants;
import com.habit.agent.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 会话管理工具（对应 PRD 第 10 节 T11 manage_session）。
 *
 * <p>当前后端仅提供历史查询能力（{@link ChatSessionService#queryBySessionId}）；
 * 重命名/删除/关闭等写操作待 ChatSessionService 补充后接入。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionTools {

    private final ChatSessionService chatSessionService;

    /**
     * 查询指定会话的历史对话记录。
     */
    @Tool(description = ToolConstants.Tools.QUERY_SESSION_HISTORY)
    public List<MessageVO> querySessionHistory(
            @ToolParam(description = ToolConstants.ToolParams.SESSION_ID) String sessionId) {
        log.info("Agent 查询会话历史: sessionId={}", sessionId);
        return chatSessionService.queryBySessionId(sessionId);
    }
}
