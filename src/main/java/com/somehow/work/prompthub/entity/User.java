package com.somehow.work.prompthub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名，唯一 */
    private String username;

    /** 密码哈希（bcrypt） */
    private String passwordHash;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 头像URL */
    private String avatarUrl;

    /** 创作者等级（0-5） */
    private Integer creatorLevel;

    /** 账户余额 */
    private BigDecimal balance;

    /** 角色：user / admin */
    private String role;

    /** 状态：1-正常 0-禁用 */
    private Integer status;

    /** 注册时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
