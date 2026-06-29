package com.somehow.work.prompthub.service;

import com.somehow.work.prompthub.vo.*;

import java.util.List;

/**
 * 数据统计服务接口
 */
public interface StatisticsService {

    /** 热门排行（默认 Top 20） */
    List<HotTemplateVO> getHotRanking(int limit);

    /** 模板使用趋势（近30天） */
    List<UsageTrendVO> getTemplateTrend(Long templateId);

    /** 创作者数据看板 */
    CreatorDashboardVO getCreatorDashboard(Long userId);

    /** 平台总览 */
    PlatformOverviewVO getPlatformOverview();

    /** 定时更新创作者等级 */
    void updateCreatorLevels();
}
