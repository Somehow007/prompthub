package com.somehow.work.prompthub.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.somehow.work.prompthub.service.FavoriteService;
import com.somehow.work.prompthub.util.Result;
import com.somehow.work.prompthub.vo.TemplateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收藏控制器
 */
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /** 切换收藏状态 */
    @PostMapping("/{templateId}")
    public Result<Boolean> toggle(@PathVariable Long templateId) {
        long userId = StpUtil.getLoginIdAsLong();
        boolean favorited = favoriteService.toggle(templateId, userId);
        return Result.ok(favorited ? "已收藏" : "已取消收藏", favorited);
    }

    /** 检查是否已收藏 */
    @GetMapping("/check/{templateId}")
    public Result<Boolean> check(@PathVariable Long templateId) {
        long userId = StpUtil.getLoginIdAsLong();
        boolean favorited = favoriteService.isFavorited(templateId, userId);
        return Result.ok(favorited);
    }

    /** 我的收藏列表 */
    @GetMapping
    public Result<List<TemplateVO>> list() {
        long userId = StpUtil.getLoginIdAsLong();
        List<TemplateVO> list = favoriteService.listFavorites(userId);
        return Result.ok(list);
    }
}
