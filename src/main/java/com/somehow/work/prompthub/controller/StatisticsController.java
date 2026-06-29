package com.somehow.work.prompthub.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.somehow.work.prompthub.service.StatisticsService;
import com.somehow.work.prompthub.util.Result;
import com.somehow.work.prompthub.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据统计控制器
 */
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    /** 热门排行（公开） */
    @GetMapping("/ranking")
    public Result<List<HotTemplateVO>> ranking(@RequestParam(defaultValue = "20") int limit) {
        List<HotTemplateVO> list = statisticsService.getHotRanking(limit);
        return Result.ok(list);
    }

    /** 模板使用趋势（公开） */
    @GetMapping("/trend/{templateId}")
    public Result<List<UsageTrendVO>> trend(@PathVariable Long templateId) {
        List<UsageTrendVO> trend = statisticsService.getTemplateTrend(templateId);
        return Result.ok(trend);
    }

    /** 创作者数据看板（需登录） */
    @GetMapping("/creator")
    public Result<CreatorDashboardVO> creatorDashboard() {
        long userId = StpUtil.getLoginIdAsLong();
        CreatorDashboardVO dashboard = statisticsService.getCreatorDashboard(userId);
        return Result.ok(dashboard);
    }

    /** 平台总览（公开） */
    @GetMapping("/platform")
    public Result<PlatformOverviewVO> platformOverview() {
        PlatformOverviewVO overview = statisticsService.getPlatformOverview();
        return Result.ok(overview);
    }
}
