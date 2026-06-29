package com.somehow.work.prompthub.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 平台总览视图
 */
@Data
@Builder
public class PlatformOverviewVO {

    /** 总用户数 */
    private Long totalUsers;
    /** 总模板数 */
    private Long totalTemplates;
    /** 总交易额 */
    private BigDecimal totalTransactionAmount;
    /** 总订单数 */
    private Long totalOrders;
    /** 近30天新增用户趋势 */
    private List<DailyNewUsersVO> newUserTrend;
}
