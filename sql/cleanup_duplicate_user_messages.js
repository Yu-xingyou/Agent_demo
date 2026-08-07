/**
 * 一次性清理脚本：修复「聊天记录用户消息重复」的存量脏数据。
 *
 * 背景：
 *   Spring AI 2.0 官方 MessageChatMemoryAdvisor.before() 每次请求都会无条件写入用户消息，
 *   流式失败降级（同步兜底）/重试重新执行调用链时，同一轮用户消息被写入多次，
 *   导致 ai_chat_memory 中出现 [user, user, assistant] 这类连续重复的 user 文档。
 *
 * 作用：
 *   对每个 conversationId，按 timestamp 升序遍历消息，删除「连续重复且文本相同的 user 消息」
 *   （保留每轮第一条）。仅处理连续重复，不误删间隔有 assistant 的正常多轮对话。
 *
 * 用法：
 *   mongosh --quiet habit_agent --eval "$(cat sql/cleanup_duplicate_user_messages.js)"
 *   （或：mongosh habit_agent，然后 load('sql/cleanup_duplicate_user_messages.js')）
 *
 * 注意：脚本只会删除「同一轮内连续重复的 user」，不会改动其他消息；执行前建议先备份。
 */
(function () {
  var coll = db.getCollection('ai_chat_memory');

  var conversations = coll.aggregate([
    { $sort: { conversationId: 1, timestamp: 1 } },
    { $group: { _id: '$conversationId', docs: { $push: '$$ROOT' } } }
  ]).toArray();

  var removed = 0;
  conversations.forEach(function (conv) {
    var lastUserContent = null;
    conv.docs.forEach(function (doc) {
      var msg = doc.message;
      if (msg && msg.type === 'user') {
        var content = msg.content;
        if (lastUserContent !== null && content === lastUserContent) {
          // 同一轮连续重复的 user 消息 → 删除（保留第一条）
          coll.deleteOne({ _id: doc._id });
          removed++;
          return;
        }
        lastUserContent = content;
      } else {
        // 遇到 assistant/system/tool 消息则重置，避免跨轮误删
        lastUserContent = null;
      }
    });
  });

  print('Removed duplicate user messages: ' + removed);
})();
