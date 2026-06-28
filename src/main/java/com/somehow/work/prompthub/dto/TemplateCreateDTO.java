package com.somehow.work.prompthub.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 模板创建请求
 */
@Data
public class TemplateCreateDTO {

    @NotBlank(message = "标题不能为空")
    @Size(min = 2, max = 200, message = "标题长度 2-200 个字符")
    private String title;

    @Size(max = 2000, message = "描述长度不能超过 2000 个字符")
    private String description;

    @NotBlank(message = "Prompt 内容不能为空")
    private String promptContent;

    @DecimalMin(value = "0.00", message = "价格不能为负数")
    @DecimalMax(value = "9999.99", message = "价格不能超过 9999.99")
    private BigDecimal price;

    @Size(max = 500, message = "封面图URL长度不能超过 500 个字符")
    private String coverUrl;

    /** 关联标签ID列表 */
    private List<Long> tagIds;
}
