package com.habit.agent.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.habit.agent.common.constant.AgentConstants;
import com.habit.agent.common.exception.BusinessException;
import com.habit.agent.common.vo.ReminderCreateVO;
import com.habit.agent.entity.jpa.Reminder;
import com.habit.agent.repository.jpa.ReminderRepository;
import com.habit.agent.service.ReminderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 阶段十（打卡提醒）业务逻辑实现。
 *
 * <p>基于 JPA 实体 {@link Reminder}，提供用户维度的提醒 CRUD 与启用开关。
 * 用户隔离以 {@code AgentConstants.DEFAULT_USER_ID}（单用户演示）为准，多用户时由网关注入真实 userId。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderServiceImpl implements ReminderService {

    private final ReminderRepository reminderRepository;

    /**
     * 创建提醒
     *
     * @param userId 用户 id，可空（空时使用默认用户）
     * @param vo     提醒创建视图
     * @return 创建成功的提醒实体
     */
    @Override
    @Transactional
    public Reminder create(Long userId, ReminderCreateVO vo) {
        Long uid = userId == null ? AgentConstants.DEFAULT_USER_ID : userId;
        Reminder reminder = Reminder.builder()
                .userId(uid)
                .title(vo.getTitle())
                .reminderTime(vo.getReminderTime())
                .reminderType(vo.getReminderType())
                .weekdays(vo.getWeekdays() == null || vo.getWeekdays().isBlank()
                        ? "1,2,3,4,5,6,7" : vo.getWeekdays())
                .isActive(vo.getIsActive() == null ? Boolean.TRUE : vo.getIsActive())
                .build();
        return reminderRepository.save(reminder);
    }

    /**
     * 更新提醒（仅覆盖非空字段）
     *
     * @param userId 用户 id，可空（空时使用默认用户）
     * @param id     提醒 id
     * @param vo     更新后的提醒视图
     * @return 更新后的提醒实体
     */
    @Override
    @Transactional
    public Reminder update(Long userId, Long id, ReminderCreateVO vo) {
        Reminder reminder = owned(userId, id);
        reminder.setTitle(vo.getTitle());
        reminder.setReminderTime(vo.getReminderTime());
        reminder.setReminderType(vo.getReminderType());
        if (vo.getWeekdays() != null && !vo.getWeekdays().isBlank()) {
            reminder.setWeekdays(vo.getWeekdays());
        }
        if (vo.getIsActive() != null) {
            reminder.setIsActive(vo.getIsActive());
        }
        return reminderRepository.save(reminder);
    }

    /**
     * 删除提醒
     *
     * @param userId 用户 id，可空（空时使用默认用户）
     * @param id     提醒 id
     */
    @Override
    @Transactional
    public void delete(Long userId, Long id) {
        Reminder reminder = owned(userId, id);
        reminderRepository.delete(reminder);
    }

    /**
     * 查询用户全部提醒（倒序）
     *
     * @param userId 用户 id，可空（空时使用默认用户）
     * @return 该用户全部提醒实体列表（倒序）
     */
    @Override
    public List<Reminder> list(Long userId) {
        Long uid = userId == null ? AgentConstants.DEFAULT_USER_ID : userId;
        return reminderRepository.findByUserIdOrderByCreateTimeDesc(uid);
    }

    /**
     * 切换提醒启用状态
     *
     * @param userId 用户 id，可空（空时使用默认用户）
     * @param id     提醒 id
     * @param active 目标启用状态
     * @return 切换后的提醒实体
     */
    @Override
    @Transactional
    public Reminder toggle(Long userId, Long id, Boolean active) {
        Reminder reminder = owned(userId, id);
        reminder.setIsActive(active);
        return reminderRepository.save(reminder);
    }

    /**
     * 校验提醒归属并返回实体（用户维度隔离）
     *
     * @param userId 用户 id，可空（空时使用默认用户）
     * @param id     提醒 id
     * @return 归属该用户的提醒实体
     * @throws BusinessException 当提醒不存在或不属于该用户时抛出
     */
    private Reminder owned(Long userId, Long id) {
        Long uid = userId == null ? AgentConstants.DEFAULT_USER_ID : userId;
        return reminderRepository.findById(id)
                .filter(r -> r.getUserId().equals(uid))
                .orElseThrow(() -> new BusinessException(
                        AgentConstants.CODE_REMINDER_NOT_FOUND, "提醒不存在或无权访问"));
    }
}
