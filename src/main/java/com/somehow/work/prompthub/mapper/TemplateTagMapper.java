package com.somehow.work.prompthub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somehow.work.prompthub.entity.TemplateTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 模板-标签关联 Mapper
 */
@Mapper
public interface TemplateTagMapper extends BaseMapper<TemplateTag> {
}
