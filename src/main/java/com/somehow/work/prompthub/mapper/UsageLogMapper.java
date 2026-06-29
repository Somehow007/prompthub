package com.somehow.work.prompthub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somehow.work.prompthub.entity.UsageLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 使用日志 Mapper
 */
@Mapper
public interface UsageLogMapper extends BaseMapper<UsageLog> {

    /**
     * 指定模板近N天每日使用次数
     */
    @Select("SELECT DATE(created_at) AS date, COUNT(*) AS count " +
            "FROM usage_log " +
            "WHERE template_id = #{templateId} " +
            "AND created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE(created_at) " +
            "ORDER BY date")
    List<Map<String, Object>> dailyTrend(@Param("templateId") Long templateId, @Param("days") int days);

    /**
     * 创作者所有模板近N天每日使用次数（合计）
     */
    @Select("SELECT DATE(ul.created_at) AS date, COUNT(*) AS count " +
            "FROM usage_log ul " +
            "INNER JOIN template t ON ul.template_id = t.id " +
            "WHERE t.creator_id = #{creatorId} " +
            "AND ul.created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE(ul.created_at) " +
            "ORDER BY date")
    List<Map<String, Object>> dailyTrendForCreator(@Param("creatorId") Long creatorId, @Param("days") int days);
}
