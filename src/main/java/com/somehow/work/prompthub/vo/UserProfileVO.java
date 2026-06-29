package com.somehow.work.prompthub.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户主页视图对象
 */
@Data
@Builder
public class UserProfileVO {

    // 基本信息
    private Long id;
    private String username;
    private String email;
    private String avatarUrl;
    private Integer creatorLevel;
    private BigDecimal balance;
    private LocalDateTime createdAt;

    // 统计
    private Long templateCount;
    private Long favoriteCount;
    private Long totalUseCount;
    private BigDecimal totalIncome;

    // 最近模板
    private List<TemplateVO> recentTemplates;

    // 收入明细
    private List<IncomeRecordVO> incomeRecords;
}
