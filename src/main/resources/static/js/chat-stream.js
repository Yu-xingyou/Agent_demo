/**
 * chat-stream.js — SSE 流式接收逻辑骨架（子模块 3-3）
 *
 * 本文件为阶段三骨架，实际 SSE 连接在阶段五完善。
 * 提供 startStream / stopGeneration / 消息渲染辅助函数。
 */
const ChatStream = (function () {

    let currentEventSource = null;

    /**
     * 启动 SSE 流式接收（阶段五完善）
     *
     * @param {string} message - 用户消息
     * @param {string} conversationId - 会话 ID
     */
    function startStream(message, conversationId) {
        // 阶段五实现：
        // 1. 构造 SSE URL: /api/chat/stream?message=xxx&conversationId=xxx
        // 2. new EventSource(url)
        // 3. onmessage: 解析 chunk，追加到 AI 消息气泡
        // 4. onerror: 关闭连接，显示错误
        // 5. 自定义事件 [DONE]: 结束接收

        // --- 骨架代码 ---
        console.log('[ChatStream] startStream 骨架调用:', { message, conversationId });

        // const url = `/api/chat/stream?message=${encodeURIComponent(message)}&conversationId=${conversationId}`;
        // currentEventSource = new EventSource(url);
        // currentEventSource.onmessage = (e) => { appendChunk(e.data); };
        // currentEventSource.onerror = () => { stopGeneration(); };
    }

    /**
     * 停止生成
     */
    function stopGeneration() {
        if (currentEventSource) {
            currentEventSource.close();
            currentEventSource = null;
            console.log('[ChatStream] 已停止生成');
        }
        toggleButtons(false);
    }

    /**
     * 追加用户消息到对话区域
     *
     * @param {string} text - 消息内容
     */
    function appendUserMessage(text) {
        const container = document.getElementById('chat-messages');
        const div = document.createElement('div');
        div.className = 'chat-message text-end';
        div.innerHTML = `<div class="chat-user">${escapeHtml(text)}</div>`;
        container.appendChild(div);
        scrollToBottom(container);
    }

    /**
     * 追加 AI 消息到对话区域
     *
     * @param {string} text - 消息内容
     */
    function appendAiMessage(text) {
        const container = document.getElementById('chat-messages');
        const div = document.createElement('div');
        div.className = 'chat-message';
        div.innerHTML = `<div class="chat-ai"><i class="bi bi-flower1" style="color:var(--teal-700);"></i> ${escapeHtml(text)}</div>`;
        container.appendChild(div);
        scrollToBottom(container);
    }

    /**
     * 追加流式 chunk 到当前 AI 消息（阶段五使用）
     *
     * @param {string} chunk - 文本片段
     */
    function appendChunk(chunk) {
        // 阶段五：找到当前正在生成的 AI 消息气泡，追加文本
        console.log('[ChatStream] appendChunk 骨架:', chunk);
    }

    /**
     * 显示打字指示器
     */
    function showTypingIndicator() {
        const container = document.getElementById('chat-messages');
        const div = document.createElement('div');
        div.className = 'chat-message';
        div.id = 'typing-indicator';
        div.innerHTML = `<div class="chat-ai"><div class="typing-indicator"><span></span><span></span><span></span></div></div>`;
        container.appendChild(div);
        scrollToBottom(container);
    }

    /**
     * 移除打字指示器
     */
    function removeTypingIndicator() {
        const indicator = document.getElementById('typing-indicator');
        if (indicator) indicator.remove();
    }

    /**
     * 切换发送/停止按钮状态
     *
     * @param {boolean} generating - 是否正在生成
     */
    function toggleButtons(generating) {
        const sendBtn = document.getElementById('send-btn');
        const stopBtn = document.getElementById('stop-btn');
        if (sendBtn) sendBtn.style.display = generating ? 'none' : '';
        if (stopBtn) stopBtn.style.display = generating ? '' : 'none';
    }

    /**
     * 滚动到底部
     */
    function scrollToBottom(container) {
        container.scrollTop = container.scrollHeight;
    }

    /**
     * HTML 转义
     */
    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    // 公开 API
    return {
        startStream,
        stopGeneration,
        appendUserMessage,
        appendAiMessage,
        appendChunk,
        showTypingIndicator,
        removeTypingIndicator,
        toggleButtons
    };
})();
