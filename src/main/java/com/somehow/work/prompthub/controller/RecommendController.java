package com.somehow.work.prompthub.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.somehow.work.prompthub.service.RecommendService;
import com.somehow.work.prompthub.util.Result;
import com.somehow.work.prompthub.vo.TemplateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 智能推荐控制器
 */
@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    /** 获取个性化推荐（需登录） */
    @GetMapping
    public Result<List<TemplateVO>> recommend(@RequestParam(defaultValue = "10") int limit) {
        long userId = StpUtil.getLoginIdAsLong();
        List<TemplateVO> list = recommendService.recommend(userId, Math.min(limit, 20));
        return Result.ok(list);
    }
}
