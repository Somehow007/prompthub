package com.somehow.work.prompthub.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户信息视图对象（不暴露密码等敏感字段）
 */
@Data
@Builder
public class UserVO {

    private Long id;
    private String username;
    private String email;
    private String phone;
    private String avatarUrl;
    private Integer creatorLevel;
    private BigDecimal balance;
    private String role;
    private LocalDateTime createdAt;
}
