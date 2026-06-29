package com.somehow.work.prompthub.service;

import com.somehow.work.prompthub.vo.OrderVO;

import java.util.List;

/**
 * 订单服务接口
 */
public interface OrderService {

    /** 购买模板（事务：扣款+生成订单+记录收入） */
    OrderVO purchase(Long templateId, Long userId);

    /** 我的订单列表 */
    List<OrderVO> listOrders(Long userId);

    /** 检查用户是否已购买某模板 */
    boolean isPurchased(Long templateId, Long userId);

    /** 获取用户已购买的所有模板ID */
    List<Long> getPurchasedTemplateIds(Long userId);
}
