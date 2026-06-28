package com.somehow.work.prompthub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somehow.work.prompthub.entity.Tag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 标签 Mapper
 */
@Mapper
public interface TagMapper extends BaseMapper<Tag> {
}
