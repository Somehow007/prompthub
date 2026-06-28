package com.somehow.work.prompthub.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.somehow.work.prompthub.dto.LoginDTO;
import com.somehow.work.prompthub.dto.LoginResultDTO;
import com.somehow.work.prompthub.dto.RegisterDTO;
import com.somehow.work.prompthub.service.UserService;
import com.somehow.work.prompthub.util.Result;
import com.somehow.work.prompthub.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 注册 */
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterDTO dto) {
        UserVO vo = userService.register(dto);
        return Result.ok("注册成功", vo);
    }

    /** 登录 */
    @PostMapping("/login")
    public Result<LoginResultDTO> login(@Valid @RequestBody LoginDTO dto) {
        LoginResultDTO result = userService.login(dto);
        return Result.ok("登录成功", result);
    }

    /** 获取当前用户信息（需登录） */
    @GetMapping("/info")
    public Result<UserVO> info() {
        long userId = StpUtil.getLoginIdAsLong();
        UserVO vo = userService.currentUser(userId);
        return Result.ok(vo);
    }
}
