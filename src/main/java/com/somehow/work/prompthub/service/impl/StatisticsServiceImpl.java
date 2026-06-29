package com.somehow.work.prompthub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.somehow.work.prompthub.entity.Template;
import com.somehow.work.prompthub.mapper.OrderMapper;
import com.somehow.work.prompthub.mapper.TemplateMapper;
import com.somehow.work.prompthub.mapper.UsageLogMapper;
import com.somehow.work.prompthub.mapper.UserMapper;
import com.somehow.work.prompthub.service.StatisticsService;
import com.somehow.work.prompthub.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据统计服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final TemplateMapper templateMapper;
    private final UsageLogMapper usageLogMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;

    @Override
    public List<HotTemplateVO> getHotRanking(int limit) {
        List<Map<String, Object>> rows = templateMapper.hotRankingRaw(limit);
        return rows.stream().map(row -> HotTemplateVO.builder()
                .templateId(toLong(row.get("templateId")))
                .title((String) row.get("title"))
                .creatorName((String) row.get("creatorName"))
                .price(toBigDecimal(row.get("price")))
                .useCount(toLong(row.get("useCount")))
                .avgRating(toBigDecimal(row.get("avgRating")))
                .favoriteCount(toInt(row.get("favoriteCount")))
                .hotScore(toDouble(row.get("hotScore")))
                .build()).collect(Collectors.toList());
    }

    @Override
    public List<UsageTrendVO> getTemplateTrend(Long templateId) {
        List<Map<String, Object>> rows = usageLogMapper.dailyTrend(templateId, 30);
        return rows.stream().map(row -> UsageTrendVO.builder()
                .date(toStr(row.get("date")))
                .count(toInt(row.get("count")))
                .build()).collect(Collectors.toList());
    }

    @Override
    public CreatorDashboardVO getCreatorDashboard(Long userId) {
        // 查询创作者的模板
        List<Template> templates = templateMapper.selectList(
                new LambdaQueryWrapper<Template>().eq(Template::getCreatorId, userId));

        int totalTemplates = templates.size();
        long totalUseCount = templates.stream().mapToLong(Template::getUseCount).sum();
        BigDecimal avgRating = BigDecimal.ZERO;
        if (totalTemplates > 0) {
            BigDecimal sum = templates.stream()
                    .map(Template::getAvgRating)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            avgRating = sum.divide(BigDecimal.valueOf(totalTemplates), 1, RoundingMode.HALF_UP);
        }

        // 总收入：从 income_record 表统计（通过 orderMapper 的 monthlyIncome 累加）
        BigDecimal totalIncome = BigDecimal.ZERO;
        List<Map<String, Object>> incomeRows = orderMapper.monthlyIncome(userId);
        List<IncomeTrendVO> incomeTrend = incomeRows.stream().map(row -> {
            BigDecimal amt = toBigDecimal(row.get("amount"));
            return IncomeTrendVO.builder()
                    .month(toStr(row.get("month")))
                    .amount(amt)
                    .build();
        }).collect(Collectors.toList());
        totalIncome = incomeTrend.stream()
                .map(IncomeTrendVO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 近30天使用趋势
        List<Map<String, Object>> trendRows = usageLogMapper.dailyTrendForCreator(userId, 30);
        List<UsageTrendVO> usageTrend = trendRows.stream().map(row -> UsageTrendVO.builder()
                .date(toStr(row.get("date")))
                .count(toInt(row.get("count")))
                .build()).collect(Collectors.toList());

        return CreatorDashboardVO.builder()
                .totalTemplates(totalTemplates)
                .totalUseCount(totalUseCount)
                .totalIncome(totalIncome)
                .avgRating(avgRating)
                .usageTrend(usageTrend)
                .incomeTrend(incomeTrend)
                .build();
    }

    @Override
    public PlatformOverviewVO getPlatformOverview() {
        // 总用户数
        Long totalUsers = userMapper.selectCount(null);
        // 总模板数
        Long totalTemplates = templateMapper.selectCount(null);
        // 总交易额
        BigDecimal totalTransactionAmount = orderMapper.totalTransactionAmount();
        // 总订单数
        Long totalOrders = orderMapper.totalOrders();
        // 近30天新增用户趋势
        List<Map<String, Object>> rows = userMapper.countNewUsersByDay(30);
        List<DailyNewUsersVO> newUserTrend = rows.stream().map(row -> DailyNewUsersVO.builder()
                .date(toStr(row.get("date")))
                .count(toInt(row.get("count")))
                .build()).collect(Collectors.toList());

        return PlatformOverviewVO.builder()
                .totalUsers(totalUsers)
                .totalTemplates(totalTemplates)
                .totalTransactionAmount(totalTransactionAmount)
                .totalOrders(totalOrders)
                .newUserTrend(newUserTrend)
                .build();
    }

    @Override
    @Scheduled(cron = "0 0 3 * * ?") // 每天凌晨3点执行
    public void updateCreatorLevels() {
        try {
            userMapper.callUpdateCreatorLevels();
            log.info("创作者等级更新完成");
        } catch (Exception e) {
            log.error("创作者等级更新失败", e);
        }
    }

    // ──────────── 类型转换工具 ────────────

    /** 转 String：处理 java.sql.Date 等非 String 类型（JDBC DATE() 返回 java.sql.Date） */
    private String toStr(Object val) {
        if (val == null) return "";
        return val.toString();
    }

    private Long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Long) return (Long) val;
        if (val instanceof Number) return ((Number) val).longValue();
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private Integer toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Number) return ((Number) val).intValue();
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Double toDouble(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Double) return (Double) val;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try {
            return Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return BigDecimal.valueOf(((Number) val).doubleValue());
        try {
            return new BigDecimal(val.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
