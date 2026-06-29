package com.somehow.work.prompthub.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创作者数据看板视图
 */
@Data
@Builder
public class CreatorDashboardVO {

    /** 模板总数 */
    private Integer totalTemplates;
    /** 总使用次数 */
    private Long totalUseCount;
    /** 总收入 */
    private BigDecimal totalIncome;
    /** 平均评分 */
    private BigDecimal avgRating;
    /** 近30天使用趋势 */
    private List<UsageTrendVO> usageTrend;
    /** 近12个月收入趋势 */
    private List<IncomeTrendVO> incomeTrend;
}
