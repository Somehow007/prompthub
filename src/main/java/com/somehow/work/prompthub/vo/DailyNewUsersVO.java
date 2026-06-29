package com.somehow.work.prompthub.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 每日新增用户数据点
 */
@Data
@Builder
public class DailyNewUsersVO {

    /** 日期，格式 yyyy-MM-dd */
    private String date;
    /** 当日新增用户数 */
    private Integer count;
}
