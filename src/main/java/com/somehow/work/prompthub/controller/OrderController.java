package com.somehow.work.prompthub.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.somehow.work.prompthub.service.OrderService;
import com.somehow.work.prompthub.util.Result;
import com.somehow.work.prompthub.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单控制器
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 购买模板 */
    @PostMapping
    public Result<OrderVO> purchase(@RequestParam Long templateId) {
        long userId = StpUtil.getLoginIdAsLong();
        OrderVO vo = orderService.purchase(templateId, userId);
        return Result.ok("购买成功", vo);
    }

    /** 我的订单列表 */
    @GetMapping
    public Result<List<OrderVO>> list() {
        long userId = StpUtil.getLoginIdAsLong();
        List<OrderVO> list = orderService.listOrders(userId);
        return Result.ok(list);
    }

    /** 获取已购买的模板ID列表（用于前端判断是否已拥有） */
    @GetMapping("/purchased-ids")
    public Result<List<Long>> purchasedIds() {
        long userId = StpUtil.getLoginIdAsLong();
        List<Long> ids = orderService.getPurchasedTemplateIds(userId);
        return Result.ok(ids);
    }
}
