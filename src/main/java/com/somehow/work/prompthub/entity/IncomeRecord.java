package com.somehow.work.prompthub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收入明细实体
 */
@Data
@TableName("income_record")
public class IncomeRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 创作者用户ID */
    private Long userId;

    /** 模板ID */
    private Long templateId;

    /** 关联订单ID */
    private Long orderId;

    /** 收入金额 */
    private BigDecimal amount;

    /** 类型：sale-销售 refund-退款 */
    private String type;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
