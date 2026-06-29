package com.somehow.work.prompthub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 */
@Data
@TableName("`order`")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单编号（业务唯一） */
    private String orderNo;

    /** 用户ID */
    private Long userId;

    /** 模板ID */
    private Long templateId;

    /** 交易金额 */
    private BigDecimal amount;

    /** 状态：0-待支付 1-已完成 2-已取消 3-已退款 */
    private Integer status;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
