package com.somehow.work.prompthub.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单视图对象
 */
@Data
@Builder
public class OrderVO {

    private Long id;
    private String orderNo;
    private Long userId;
    private Long templateId;
    private String templateTitle;
    private BigDecimal amount;
    private Integer status;
    private LocalDateTime payTime;
    private LocalDateTime createdAt;
}
