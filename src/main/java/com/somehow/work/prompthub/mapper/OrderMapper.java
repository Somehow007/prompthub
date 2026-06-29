package com.somehow.work.prompthub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somehow.work.prompthub.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 订单 Mapper
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 创作者近12个月每月收入
     */
    @Select("SELECT DATE_FORMAT(o.created_at, '%Y-%m') AS month, SUM(o.amount) AS amount " +
            "FROM `order` o " +
            "INNER JOIN template t ON o.template_id = t.id " +
            "WHERE t.creator_id = #{creatorId} " +
            "AND o.status = 1 " +
            "AND o.created_at >= DATE_SUB(NOW(), INTERVAL 12 MONTH) " +
            "GROUP BY DATE_FORMAT(o.created_at, '%Y-%m') " +
            "ORDER BY month")
    List<Map<String, Object>> monthlyIncome(@Param("creatorId") Long creatorId);

    /**
     * 平台总交易额（已完成订单）
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM `order` WHERE status = 1")
    BigDecimal totalTransactionAmount();

    /**
     * 平台总订单数（已完成）
     */
    @Select("SELECT COUNT(*) FROM `order` WHERE status = 1")
    Long totalOrders();
}
