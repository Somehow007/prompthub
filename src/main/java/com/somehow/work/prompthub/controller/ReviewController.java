package com.somehow.work.prompthub.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.somehow.work.prompthub.dto.CreateReviewDTO;
import com.somehow.work.prompthub.service.ReviewService;
import com.somehow.work.prompthub.util.Result;
import com.somehow.work.prompthub.vo.ReviewVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评价控制器
 */
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /** 创建评价 */
    @PostMapping("/api/reviews")
    public Result<ReviewVO> create(@Valid @RequestBody CreateReviewDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        ReviewVO vo = reviewService.create(dto, userId);
        return Result.ok("评价成功", vo);
    }

    /** 模板评价列表 */
    @GetMapping("/api/templates/{templateId}/reviews")
    public Result<List<ReviewVO>> listByTemplate(@PathVariable Long templateId) {
        List<ReviewVO> list = reviewService.listByTemplate(templateId);
        return Result.ok(list);
    }

    /** 删除自己的评价 */
    @DeleteMapping("/api/reviews/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        long userId = StpUtil.getLoginIdAsLong();
        reviewService.delete(id, userId);
        return Result.ok("评价已删除", null);
    }
}
