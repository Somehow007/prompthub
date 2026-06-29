package com.somehow.work.prompthub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.somehow.work.prompthub.entity.*;
import com.somehow.work.prompthub.exception.BusinessException;
import com.somehow.work.prompthub.mapper.*;
import com.somehow.work.prompthub.service.OrderService;
import com.somehow.work.prompthub.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 订单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final TemplateMapper templateMapper;
    private final UserMapper userMapper;
    private final IncomeRecordMapper incomeRecordMapper;

    @Override
    @Transactional
    public OrderVO purchase(Long templateId, Long userId) {
        // 1. 检查模板是否存在且上架
        Template template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        if (template.getStatus() != 1) {
            throw new BusinessException("模板已下架，无法购买");
        }
        if (template.getCreatorId().equals(userId)) {
            throw new BusinessException("无法购买自己创建的模板");
        }

        // 2. 检查是否已购买
        Long count = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .eq(Order::getTemplateId, templateId)
                        .eq(Order::getStatus, 1));
        if (count > 0) {
            throw new BusinessException("您已购买过此模板");
        }

        BigDecimal amount = template.getPrice();

        // 3. 付费模板：扣减余额
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            User user = userMapper.selectById(userId);
            if (user.getBalance().compareTo(amount) < 0) {
                throw new BusinessException("余额不足，请先充值");
            }
            // UPDATE user SET balance = balance - amount WHERE id = userId AND balance >= amount
            int rows = userMapper.deductBalance(userId, amount);
            if (rows == 0) {
                throw new BusinessException("扣款失败，余额不足");
            }
        }

        // 4. 生成订单
        Order order = new Order();
        order.setOrderNo(UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        order.setUserId(userId);
        order.setTemplateId(templateId);
        order.setAmount(amount);
        order.setStatus(1); // 直接完成
        order.setPayTime(LocalDateTime.now());
        orderMapper.insert(order);

        // 5. 记录创作者收入
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            IncomeRecord income = new IncomeRecord();
            income.setUserId(template.getCreatorId());
            income.setTemplateId(templateId);
            income.setOrderId(order.getId());
            income.setAmount(amount);
            income.setType("sale");
            incomeRecordMapper.insert(income);
        }

        // 6. 更新模板使用统计
        template.setUseCount(template.getUseCount() + 1);
        // use_count_7d 通过 @TableField 映射正确字段
        template.setUseCount7d((template.getUseCount7d() != null ? template.getUseCount7d() : 0) + 1);
        templateMapper.updateById(template);

        log.info("购买成功: userId={}, templateId={}, amount={}", userId, templateId, amount);

        return OrderVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .templateId(order.getTemplateId())
                .templateTitle(template.getTitle())
                .amount(order.getAmount())
                .status(order.getStatus())
                .payTime(order.getPayTime())
                .createdAt(order.getCreatedAt())
                .build();
    }

    @Override
    public List<OrderVO> listOrders(Long userId) {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .orderByDesc(Order::getCreatedAt));

        // 批量获取模板标题
        Map<Long, String> titleMap = Map.of();
        if (!orders.isEmpty()) {
            List<Long> templateIds = orders.stream()
                    .map(Order::getTemplateId)
                    .distinct()
                    .toList();
            List<Template> templates = templateMapper.selectBatchIds(templateIds);
            titleMap = templates.stream()
                    .collect(Collectors.toMap(Template::getId, Template::getTitle));
        }

        Map<Long, String> finalTitleMap = titleMap;
        return orders.stream()
                .map(o -> OrderVO.builder()
                        .id(o.getId())
                        .orderNo(o.getOrderNo())
                        .userId(o.getUserId())
                        .templateId(o.getTemplateId())
                        .templateTitle(finalTitleMap.getOrDefault(o.getTemplateId(), ""))
                        .amount(o.getAmount())
                        .status(o.getStatus())
                        .payTime(o.getPayTime())
                        .createdAt(o.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
