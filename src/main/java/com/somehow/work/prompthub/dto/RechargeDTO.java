package com.somehow.work.prompthub.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 充值请求
 */
@Data
public class RechargeDTO {

    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额至少 0.01 元")
    @DecimalMax(value = "99999.99", message = "单次充值金额不能超过 99999.99 元")
    private BigDecimal amount;
}
