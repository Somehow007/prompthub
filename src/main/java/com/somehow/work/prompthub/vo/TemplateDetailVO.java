package com.somehow.work.prompthub.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 模板详情视图（含当前版本 Prompt 内容和标签）
 */
@Data
@Builder
public class TemplateDetailVO {

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

    /** 当前版本的 Prompt 内容 */
    private String promptContent;
    /** 当前版本的变更说明 */
    private String changeNote;

    private Long useCount;
    private Integer favoriteCount;
    private Integer reviewCount;
    private BigDecimal avgRating;

    /** 标签列表 */
    private List<TagVO> tags;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
