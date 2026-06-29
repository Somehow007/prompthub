package com.somehow.work.prompthub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评分评价实体
 */
@Data
@TableName("review")
public class Review {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 模板ID */
    private Long templateId;

    /** 评分（1-5） */
    private Integer rating;

    /** 评价内容 */
    private String content;

    /** 评价时间 */
    private LocalDateTime createdAt;
}
