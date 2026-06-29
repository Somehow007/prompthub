package com.somehow.work.prompthub.service;

import com.somehow.work.prompthub.dto.LoginDTO;
import com.somehow.work.prompthub.dto.LoginResultDTO;
import com.somehow.work.prompthub.dto.RegisterDTO;
import com.somehow.work.prompthub.vo.UserProfileVO;
import com.somehow.work.prompthub.vo.UserVO;

import java.math.BigDecimal;

/**
 * 用户服务接口
 */
public interface UserService {

    /** 注册 */
    UserVO register(RegisterDTO dto);

    /** 登录 */
    LoginResultDTO login(LoginDTO dto);

    /** 获取当前用户信息 */
    UserVO currentUser(Long userId);

    /** 充值 */
    UserVO recharge(Long userId, BigDecimal amount);

    /** 获取用户主页 */
    UserProfileVO profile(Long userId);
}
