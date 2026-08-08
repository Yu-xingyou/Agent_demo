package com.habit.agent.service;

import java.util.List;

import com.habit.agent.common.vo.ReminderCreateVO;
import com.habit.agent.entity.jpa.Reminder;

/**
 * 阶段十（打卡提醒）业务接口。
 */
public interface ReminderService {

    /**
     * 创建提醒
     *
     * @param userId 用户 id，可空（空时使用默认用户）
     * @param vo     提醒创建视图
     * @return 创建成功的提醒实体
     */
    Reminder create(Long userId, ReminderCreateVO vo);

    /**
     * 更新提醒（标题定位，用户维度隔离）
     *
     * @param userId 用户 id，可空（空时使用默认用户）
     * @param id     提醒 id
     * @param vo     更新后的提醒视图
     * @return 更新后的提醒实体
     */
    Reminder update(Long userId, Long id, ReminderCreateVO vo);

    /**
     * 删除提醒
     *
     * @param userId 用户 id，可空（空时使用默认用户）
     * @param id     提醒 id
     */
    void delete(Long userId, Long id);

    /**
     * 查询用户全部提醒（倒序）
     *
     * @param userId 用户 id，可空（空时使用默认用户）
     * @return 该用户全部提醒实体列表（倒序）
     */
    List<Reminder> list(Long userId);

    /**
     * 开关启用状态
     *
     * @param userId 用户 id，可空（空时使用默认用户）
     * @param id     提醒 id
     * @param active 目标启用状态
     * @return 切换后的提醒实体
     */
    Reminder toggle(Long userId, Long id, Boolean active);
}
