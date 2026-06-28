package com.somehow.work.prompthub.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 模板列表视图
 */
@Data
@Builder
public class TemplateVO {

    private Long id;
    private Long creatorId;
    private String creatorName;
    private String creatorAvatar;

    private String title;
    private String description;
    private String coverUrl;

    private BigDecimal price;
    private Integer status;
    private Integer currentVersion;

    private Long useCount;
    private Integer favoriteCount;
    private Integer reviewCount;
    private BigDecimal avgRating;

    /** 关联标签ID列表 */
    private List<Long> tagIds;

    /** 关联标签名称列表 */
    private List<String> tagNames;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
