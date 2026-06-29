package com.somehow.work.prompthub.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 收入趋势数据点视图
 */
@Data
@Builder
public class IncomeTrendVO {

    /** 月份，格式 yyyy-MM */
    private String month;
    /** 当月收入金额 */
    private BigDecimal amount;
}
