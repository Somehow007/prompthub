package com.somehow.work.prompthub.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.somehow.work.prompthub.dto.CreateActivityDTO;
import com.somehow.work.prompthub.service.ActivityService;
import com.somehow.work.prompthub.util.Result;
import com.somehow.work.prompthub.vo.ActivityVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 限时活动控制器
 */
@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    /** 活动列表（公开） */
    @GetMapping
    public Result<List<ActivityVO>> list() {
        List<ActivityVO> list = activityService.listActivities();
        return Result.ok(list);
    }

    /** 领取活动模板（需登录） */
    @PostMapping("/{id}/claim")
    public Result<String> claim(@PathVariable Long id) {
        long userId = StpUtil.getLoginIdAsLong();
        activityService.claim(id, userId);
        return Result.ok("领取成功");
    }
}
