package com.somehow.work.prompthub.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 标签视图（支持树形结构）
 */
@Data
@Builder
public class TagVO {

    private Long id;
    private String name;
    private Long parentId;
    private Integer level;
    private Integer sortOrder;

    /** 子标签列表 */
    private List<TagVO> children;
}
