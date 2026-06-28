package com.somehow.work.prompthub.dto;

import com.somehow.work.prompthub.vo.UserVO;
import lombok.Builder;
import lombok.Data;

/**
 * 登录响应：包含 Token 和用户信息
 */
@Data
@Builder
public class LoginResultDTO {

    private String token;
    private UserVO user;
}
