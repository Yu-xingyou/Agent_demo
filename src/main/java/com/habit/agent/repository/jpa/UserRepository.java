package com.habit.agent.repository.jpa;

import com.habit.agent.entity.jpa.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户 Repository（子模块 2-1）
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名（唯一）
     * @return 命中的用户实体；不存在时返回空 Optional
     */
    Optional<User> findByUsername(String username);
}
