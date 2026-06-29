package com.somehow.work.prompthub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somehow.work.prompthub.entity.ActivityTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 限时活动 Mapper
 */
@Mapper
public interface ActivityTemplateMapper extends BaseMapper<ActivityTemplate> {

    /** 原子扣减剩余份数（WHERE remaining_quota > 0 防止超扣） */
    @Update("UPDATE activity_template SET remaining_quota = remaining_quota - 1 WHERE id = #{id} AND remaining_quota > 0")
    int decrementQuota(@Param("id") Long id);
}
