package com.somehow.work.prompthub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模板-标签关联实体
 */
@Data
@TableName("template_tag")
public class TemplateTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模板ID */
    private Long templateId;

    /** 标签ID */
    private Long tagId;

    /** 关联时间 */
    private LocalDateTime createdAt;
}
