package com.somehow.work.prompthub.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 限时活动视图对象
 */
@Data
@Builder
public class ActivityVO {

    private Long id;
    private Long templateId;
    private String templateTitle;
    private String creatorName;
    private BigDecimal templatePrice;
    private Double avgRating;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalQuota;
    private Integer remainingQuota;
    private Integer status;

    private LocalDateTime createdAt;
}
