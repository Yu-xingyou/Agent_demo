package com.tianji.aigc.memory;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Set;

/**
 * 基于Redis实现的ChatMemoryRepository
 */
public class RedisChatMemoryRepository implements ChatMemoryRepository {
    // 默认redis中key的前缀
    public static final String DEFAULT_PREFIX = "CHAT:";
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private final String prefix;
    public RedisChatMemoryRepository(String prefix){
        this.prefix=prefix;
    }
    public RedisChatMemoryRepository() {
        this.prefix = DEFAULT_PREFIX;
    }
    @Override
    public List<String> findConversationIds() {
        Set<String> keys =  this.stringRedisTemplate.keys(this.prefix+"*");
        return StreamUtil.of(keys)
                .map(key-> StrUtil.replace(key,this.prefix,""))
                .toList();
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        var redisKey = this.getKey(conversationId);
        var listOps = this.stringRedisTemplate.boundListOps(redisKey);
        var messages = listOps.range(0,-1);
        return CollStreamUtil.toList(messages,MessageUtil::toMessage);
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        //注意：messages是全量
        var redisKey = this.getKey(conversationId);
        var listOps = this.stringRedisTemplate.boundGeoOps(redisKey);
        //message是全量数据列表，保存数据之前应该删除以前所有的聊天记录
        this.deleteByConversationId(conversationId);
        messages.forEach(message ->listOps.rightPush(JSONUtil.toJsonStr(message)));
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        var redisKey = this.getKey(conversationId);
        this.stringRedisTemplate.delete(redisKey);
    }

    private String getKey(String conversationId){
        return prefix + conversationId;
    }
}
