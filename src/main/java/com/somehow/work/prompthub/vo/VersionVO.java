package com.somehow.work.prompthub.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 版本视图
 */
@Data
@Builder
public class VersionVO {

    private Long id;
    private Long templateId;
    private Integer versionNumber;
    private String promptContent;
    private String changeNote;
    private LocalDateTime createdAt;
}
