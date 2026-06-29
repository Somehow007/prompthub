package com.somehow.work.prompthub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somehow.work.prompthub.entity.UsageLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 使用日志 Mapper
 */
@Mapper
public interface UsageLogMapper extends BaseMapper<UsageLog> {
}
