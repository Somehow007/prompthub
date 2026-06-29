package com.somehow.work.prompthub.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.somehow.work.prompthub.dto.CreateActivityDTO;
import com.somehow.work.prompthub.exception.BusinessException;
import com.somehow.work.prompthub.service.ActivityService;
import com.somehow.work.prompthub.util.Result;
import com.somehow.work.prompthub.vo.ActivityVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员活动管理控制器
 */
@RestController
@RequestMapping("/api/admin/activities")
@RequiredArgsConstructor
public class AdminActivityController {

    private final ActivityService activityService;

    /** 创建活动 */
    @PostMapping
    public Result<ActivityVO> create(@Valid @RequestBody CreateActivityDTO dto) {
        // 简单角色校验：检查是否为 admin
        String role = (String) StpUtil.getSession().get("role");
        if (!"admin".equals(role)) {
            throw new BusinessException(403, "无权限，仅管理员可操作");
        }
        ActivityVO vo = activityService.createActivity(dto);
        return Result.ok("活动创建成功", vo);
    }
}
