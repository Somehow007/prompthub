package com.somehow.work.prompthub.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 模板列表查询参数
 */
@Data
public class TemplateQueryDTO {

    /** 页码（从1开始） */
    private Integer page = 1;

    /** 每页条数 */
    private Integer size = 12;

    /** 标签ID筛选 */
    private Long tagId;

    /** 关键字搜索（全文索引） */
    private String keyword;

    /** 最低价格 */
    private BigDecimal minPrice;

    /** 最高价格 */
    private BigDecimal maxPrice;

    /** 排序字段：hot(默认) / newest / rating / price_asc / price_desc */
    private String sortBy = "hot";

    /** 模板状态筛选（创作者个人页用），默认只查上架的 */
    private Integer status = 1;

    /** 创作者ID筛选（个人页用） */
    private Long creatorId;
}
