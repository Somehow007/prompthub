package com.somehow.work.prompthub.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建限时活动 DTO
 */
@Data
public class CreateActivityDTO {

    @NotNull(message = "模板ID不能为空")
    private Long templateId;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    @Future(message = "结束时间必须在未来")
    private LocalDateTime endTime;

    @NotNull(message = "总份数不能为空")
    @Min(value = 1, message = "总份数至少为1")
    private Integer totalQuota;
}
