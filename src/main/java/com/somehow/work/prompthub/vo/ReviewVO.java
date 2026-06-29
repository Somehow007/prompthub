package com.somehow.work.prompthub.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价视图对象
 */
@Data
@Builder
public class ReviewVO {

    private Long id;
    private Long userId;
    private String username;
    private String avatarUrl;
    private Long templateId;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
}
