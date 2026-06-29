package com.somehow.work.prompthub.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收入明细视图对象
 */
@Data
@Builder
public class IncomeRecordVO {

    private Long id;
    private Long templateId;
    private String templateTitle;
    private Long orderId;
    private BigDecimal amount;
    private String type;
    private LocalDateTime createdAt;
}
