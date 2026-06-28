package com.somehow.work.prompthub.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.somehow.work.prompthub.dto.LoginDTO;
import com.somehow.work.prompthub.dto.LoginResultDTO;
import com.somehow.work.prompthub.dto.RegisterDTO;
import com.somehow.work.prompthub.entity.User;
import com.somehow.work.prompthub.exception.BusinessException;
import com.somehow.work.prompthub.mapper.UserMapper;
import com.somehow.work.prompthub.service.UserService;
import com.somehow.work.prompthub.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public UserVO register(RegisterDTO dto) {
        // 检查用户名是否已存在
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已被注册");
        }

        // 检查邮箱是否已存在
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            count = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail()));
            if (count > 0) {
                throw new BusinessException("邮箱已被注册");
            }
        }

        // 构建用户实体
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPasswordHash(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setCreatorLevel(0);
        user.setBalance(java.math.BigDecimal.ZERO);
        user.setRole("user");
        user.setStatus(1);

        userMapper.insert(user);
        log.info("用户注册成功: {}", user.getUsername());

        return toVO(user);
    }

    @Override
    public LoginResultDTO login(LoginDTO dto) {
        // 查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        // 检查用户状态
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        // 验证密码
        if (!BCrypt.checkpw(dto.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }

        // Sa-Token 登录
        StpUtil.login(user.getId());

        // 构建返回
        String token = StpUtil.getTokenValue();
        log.info("用户登录成功: {}", user.getUsername());

        return LoginResultDTO.builder()
                .token(token)
                .user(toVO(user))
                .build();
    }

    @Override
    public UserVO currentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toVO(user);
    }

    /** 实体转 VO */
    private UserVO toVO(User user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .creatorLevel(user.getCreatorLevel())
                .balance(user.getBalance())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
