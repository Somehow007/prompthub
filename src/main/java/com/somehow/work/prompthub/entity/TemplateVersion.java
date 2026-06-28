package com.somehow.work.prompthub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模板版本实体
 */
@Data
@TableName("template_version")
public class TemplateVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模板ID */
    private Long templateId;

    /** 版本号（递增） */
    private Integer versionNumber;

    /** Prompt内容 */
    private String promptContent;

    /** 变更说明 */
    private String changeNote;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
