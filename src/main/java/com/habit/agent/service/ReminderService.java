package com.habit.agent.service;

import java.util.List;

import com.habit.agent.common.vo.ReminderCreateVO;
import com.habit.agent.entity.jpa.Reminder;

/**
 * 阶段十（打卡提醒）业务接口。
 */
public interface ReminderService {

    /** 创建提醒 */
    Reminder create(Long userId, ReminderCreateVO vo);

    /** 更新提醒（标题定位，用户维度隔离） */
    Reminder update(Long userId, Long id, ReminderCreateVO vo);

    /** 删除提醒 */
    void delete(Long userId, Long id);

    /** 查询用户全部提醒（倒序） */
    List<Reminder> list(Long userId);

    /** 开关启用状态 */
    Reminder toggle(Long userId, Long id, Boolean active);
}
