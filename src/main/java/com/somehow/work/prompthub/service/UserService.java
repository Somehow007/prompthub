package com.somehow.work.prompthub.service;

import com.somehow.work.prompthub.dto.LoginDTO;
import com.somehow.work.prompthub.dto.LoginResultDTO;
import com.somehow.work.prompthub.dto.RegisterDTO;
import com.somehow.work.prompthub.vo.UserVO;

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
}
