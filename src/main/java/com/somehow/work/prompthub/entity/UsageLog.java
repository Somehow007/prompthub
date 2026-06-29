package com.somehow.work.prompthub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 使用日志实体
 */
@Data
@TableName("usage_log")
public class UsageLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 模板ID */
    private Long templateId;

    /** 输入参数摘要 */
    private String inputParams;

    /** 使用时间 */
    private LocalDateTime createdAt;
}
