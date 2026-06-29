package com.somehow.work.prompthub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somehow.work.prompthub.entity.Template;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 模板 Mapper
 */
@Mapper
public interface TemplateMapper extends BaseMapper<Template> {

    /**
     * 全文搜索模板（BOOLEAN 模式）
     */
    @Select("SELECT * FROM template WHERE status = 1 " +
            "AND MATCH(title, description) AGAINST(#{keyword} IN BOOLEAN MODE) " +
            "ORDER BY use_count DESC")
    List<Template> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 模糊搜索模板（LIKE 模式，支持部分关键字匹配）
     */
    @Select("SELECT * FROM template WHERE status = 1 " +
            "AND (title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR description LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY use_count DESC")
    List<Template> searchByKeywordLike(@Param("keyword") String keyword);
}
