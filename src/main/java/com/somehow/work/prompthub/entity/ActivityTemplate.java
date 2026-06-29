package com.somehow.work.prompthub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 限时活动模板实体
 */
@Data
@TableName("activity_template")
public class ActivityTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模板ID */
    private Long templateId;

    /** 活动开始时间 */
    private LocalDateTime startTime;

    /** 活动结束时间 */
    private LocalDateTime endTime;

    /** 限量总份数 */
    private Integer totalQuota;

    /** 剩余份数 */
    private Integer remainingQuota;

    /** 状态：1-进行中 0-已结束 2-未开始 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
