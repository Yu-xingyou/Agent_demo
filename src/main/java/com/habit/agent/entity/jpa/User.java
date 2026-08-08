package com.habit.agent.entity.jpa;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 用户实体（子模块 2-1）
 *
 * 单用户演示场景默认 userId=1，后续可扩展多用户。
 * 对应 MySQL 表: user（保留字，通过 auto_quote_keyword 自动加引号）
 */
@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class User {

    /** 主键 id */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 用户名（唯一，不可为空，最长 50 字符） */
    @Column(name = "username", nullable = false, length = 50, unique = true)
    private String username;

    /** 昵称（可选，最长 50 字符） */
    @Column(name = "nickname", length = 50)
    private String nickname;

    /** 创建时间（插入时由 @PrePersist 自动写入，不可更新） */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @PrePersist
    void onCreate() {
        this.createTime = LocalDateTime.now();
    }
}
