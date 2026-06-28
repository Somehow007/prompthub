package com.somehow.work.prompthub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提示词模板实体
 */
@Data
@TableName("template")
public class Template {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 创作者用户ID */
    private Long creatorId;

    /** 模板标题 */
    private String title;

    /** 适用场景描述 */
    private String description;

    /** 封面图URL */
    private String coverUrl;

    /** 当前版本号 */
    private Integer currentVersion;

    /** 价格（0=免费） */
    private BigDecimal price;

    /** 状态：1-上架 0-下架 2-审核中 */
    private Integer status;

    /** 总使用次数 */
    private Long useCount;

    /** 近7天使用次数 */
    @TableField("use_count_7d")
    private Integer useCount7d;

    /** 收藏数 */
    private Integer favoriteCount;

    /** 评价数 */
    private Integer reviewCount;

    /** 平均评分 */
    private BigDecimal avgRating;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
