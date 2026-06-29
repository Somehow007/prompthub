package com.somehow.work.prompthub.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建评价请求
 */
@Data
public class CreateReviewDTO {

    @NotNull(message = "模板ID不能为空")
    private Long templateId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分范围为 1-5")
    @Max(value = 5, message = "评分范围为 1-5")
    private Integer rating;

    /** 评价内容（可选） */
    private String content;
}
