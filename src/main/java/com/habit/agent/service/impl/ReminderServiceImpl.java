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

    @Override
    @Transactional
    public void delete(Long userId, Long id) {
        Reminder reminder = owned(userId, id);
        reminderRepository.delete(reminder);
    }

    @Override
    public List<Reminder> list(Long userId) {
        Long uid = userId == null ? AgentConstants.DEFAULT_USER_ID : userId;
        return reminderRepository.findByUserIdOrderByCreateTimeDesc(uid);
    }

    @Override
    @Transactional
    public Reminder toggle(Long userId, Long id, Boolean active) {
        Reminder reminder = owned(userId, id);
        reminder.setIsActive(active);
        return reminderRepository.save(reminder);
    }

    private Reminder owned(Long userId, Long id) {
        Long uid = userId == null ? AgentConstants.DEFAULT_USER_ID : userId;
        return reminderRepository.findById(id)
                .filter(r -> r.getUserId().equals(uid))
                .orElseThrow(() -> new BusinessException(
                        AgentConstants.CODE_REMINDER_NOT_FOUND, "提醒不存在或无权访问"));
    }
}
