package com.somehow.work.prompthub.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 热门排行条目视图
 */
@Data
@Builder
public class HotTemplateVO {

    private Long templateId;
    private String title;
    private String creatorName;
    private BigDecimal price;
    private Long useCount;
    private BigDecimal avgRating;
    private Integer favoriteCount;
    /** 综合热度分 = useCount*0.5 + avgRating*10*0.3 + favoriteCount*0.2 */
    private Double hotScore;
}
