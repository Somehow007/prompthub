package com.somehow.work.prompthub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标签实体（支持层级关系）
 */
@Data
@TableName("tag")
public class Tag {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标签名称 */
    private String name;

    /** 父标签ID，NULL表示顶级 */
    private Long parentId;

    /** 层级：1-一级 2-二级 3-三级 */
    private Integer level;

    /** 排序序号 */
    private Integer sortOrder;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
