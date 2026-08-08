package com.habit.agent.repository.jpa;

import com.habit.agent.entity.jpa.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 打卡提醒 Repository（子模块 2-1）
 */
@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    /**
     * 查询用户所有提醒（按创建时间倒序）
     *
     * @param userId 用户 id
     * @return 该用户全部提醒实体列表（倒序）
     */
    List<Reminder> findByUserIdOrderByCreateTimeDesc(Long userId);

    /**
     * 查询用户启用的提醒
     *
     * @param userId   用户 id
     * @param isActive 启用状态
     * @return 该用户指定启用状态的提醒实体列表
     */
    List<Reminder> findByUserIdAndIsActive(Long userId, Boolean isActive);
}
