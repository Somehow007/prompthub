package com.somehow.work.prompthub.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 使用趋势数据点视图
 */
@Data
@Builder
public class UsageTrendVO {

    /** 日期，格式 yyyy-MM-dd */
    private String date;
    /** 当日使用次数 */
    private Integer count;
}
