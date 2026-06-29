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
     * 基于标签相似度推荐模板
     * 排除用户已有模板，按标签重叠数+评分+使用量排序
     */
    @Select("<script>" +
            "SELECT t.*, COUNT(DISTINCT tt.tag_id) AS tag_overlap " +
            "FROM template t " +
            "INNER JOIN template_tag tt ON t.id = tt.template_id " +
            "WHERE t.status = 1 " +
            "<if test='excludeIds != null and excludeIds.size() > 0'>" +
            "  AND t.id NOT IN <foreach collection='excludeIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "</if>" +
            "<if test='tagIds != null and tagIds.size() > 0'>" +
            "  AND tt.tag_id IN <foreach collection='tagIds' item='tid' open='(' separator=',' close=')'>#{tid}</foreach> " +
            "</if>" +
            "GROUP BY t.id " +
            "ORDER BY tag_overlap DESC, t.avg_rating DESC, t.use_count DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<Template> recommendByTags(@Param("tagIds") List<Long> tagIds,
                                   @Param("excludeIds") List<Long> excludeIds,
                                   @Param("limit") int limit);

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

    /**
     * 热门排行：综合热度分 = useCount*0.5 + avgRating*10*0.3 + favoriteCount*0.2
     */
    @Select("SELECT t.id AS templateId, t.title, u.username AS creatorName, " +
            "t.price, t.use_count AS useCount, t.avg_rating AS avgRating, " +
            "t.favorite_count AS favoriteCount, " +
            "(t.use_count * 0.5 + COALESCE(t.avg_rating, 0) * 10 * 0.3 + t.favorite_count * 0.2) AS hotScore " +
            "FROM template t " +
            "INNER JOIN user u ON t.creator_id = u.id " +
            "WHERE t.status = 1 " +
            "ORDER BY hotScore DESC " +
            "LIMIT #{limit}")
    List<java.util.Map<String, Object>> hotRankingRaw(@Param("limit") int limit);
}
