package com.somehow.work.prompthub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.somehow.work.prompthub.entity.*;
import com.somehow.work.prompthub.mapper.*;
import com.somehow.work.prompthub.service.RecommendService;
import com.somehow.work.prompthub.vo.TemplateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能推荐服务实现
 *
 * 算法思路（协同过滤简化版）：
 * 1. 收集用户历史交互过的模板（使用/收藏/已购）
 * 2. 提取这些模板的标签作为用户兴趣标签
 * 3. 查找拥有相同标签的其他模板（排除用户已交互的）
 * 4. 按标签重叠数 + 评分 + 热度综合排序
 * 5. 新用户（无历史交互）回退到热门推荐
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendServiceImpl implements RecommendService {

    private final TemplateMapper templateMapper;
    private final TemplateTagMapper templateTagMapper;
    private final TagMapper tagMapper;
    private final UserMapper userMapper;
    private final FavoriteMapper favoriteMapper;
    private final UsageLogMapper usageLogMapper;
    private final OrderMapper orderMapper;

    @Override
    public List<TemplateVO> recommend(Long userId, int limit) {
        // 1. 收集用户已交互的模板ID
        Set<Long> interactedIds = new HashSet<>();

        // 用户创建的模板
        List<Template> ownTemplates = templateMapper.selectList(
                new LambdaQueryWrapper<Template>().eq(Template::getCreatorId, userId));
        ownTemplates.forEach(t -> interactedIds.add(t.getId()));

        // 收藏的模板
        List<Favorite> favorites = favoriteMapper.selectList(
                new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, userId));
        favorites.forEach(f -> interactedIds.add(f.getTemplateId()));

        // 使用过的模板
        List<UsageLog> usageLogs = usageLogMapper.selectList(
                new LambdaQueryWrapper<UsageLog>()
                        .eq(UsageLog::getUserId, userId)
                        .orderByDesc(UsageLog::getCreatedAt)
                        .last("LIMIT 50"));
        usageLogs.forEach(u -> interactedIds.add(u.getTemplateId()));

        // 已购买的模板
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .eq(Order::getStatus, 1));
        orders.forEach(o -> interactedIds.add(o.getTemplateId()));

        // 2. 收集用户兴趣标签
        Set<Long> interestTagIds = new HashSet<>();
        if (!interactedIds.isEmpty()) {
            List<TemplateTag> allTTs = templateTagMapper.selectList(
                    new LambdaQueryWrapper<TemplateTag>().in(TemplateTag::getTemplateId, interactedIds));
            allTTs.forEach(tt -> interestTagIds.add(tt.getTagId()));
        }

        List<Template> recommended;

        if (interestTagIds.isEmpty()) {
            // 新用户：回退到热门推荐（高评分 + 高使用量）
            log.info("新用户推荐: userId={}, 使用热门回退", userId);
            recommended = templateMapper.selectList(
                    new LambdaQueryWrapper<Template>()
                            .eq(Template::getStatus, 1)
                            .orderByDesc(Template::getAvgRating)
                            .orderByDesc(Template::getUseCount)
                            .last("LIMIT " + limit));
        } else {
            // 基于标签相似度推荐
            List<Long> excludeList = new ArrayList<>(interactedIds);
            List<Long> tagList = new ArrayList<>(interestTagIds);
            recommended = templateMapper.recommendByTags(tagList, excludeList, limit);

            // 如果推荐结果不足，用热门补足
            if (recommended.size() < limit) {
                Set<Long> recIds = recommended.stream().map(Template::getId).collect(Collectors.toSet());
                excludeList.addAll(recIds);
                List<Template> fillers = templateMapper.selectList(
                        new LambdaQueryWrapper<Template>()
                                .eq(Template::getStatus, 1)
                                .notIn(!excludeList.isEmpty(), Template::getId, excludeList)
                                .orderByDesc(Template::getAvgRating)
                                .orderByDesc(Template::getUseCount)
                                .last("LIMIT " + (limit - recommended.size())));
                recommended.addAll(fillers);
            }
        }

        // 3. 转换为 VO
        return toVOList(recommended);
    }

    // ──────────── VO 转换 ────────────

    private List<TemplateVO> toVOList(List<Template> templates) {
        if (templates.isEmpty()) return List.of();

        Set<Long> creatorIds = templates.stream().map(Template::getCreatorId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(creatorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        Set<Long> tIds = templates.stream().map(Template::getId).collect(Collectors.toSet());
        List<TemplateTag> allTTs = templateTagMapper.selectList(
                new LambdaQueryWrapper<TemplateTag>().in(TemplateTag::getTemplateId, tIds));
        Map<Long, List<TemplateTag>> ttMap = allTTs.stream()
                .collect(Collectors.groupingBy(TemplateTag::getTemplateId));
        Set<Long> tagIds = allTTs.stream().map(TemplateTag::getTagId).collect(Collectors.toSet());
        final Map<Long, Tag> tagMap;
        if (!tagIds.isEmpty()) {
            tagMap = tagMapper.selectBatchIds(tagIds).stream()
                    .collect(Collectors.toMap(Tag::getId, t -> t));
        } else {
            tagMap = Map.of();
        }

        return templates.stream()
                .map(t -> {
                    User creator = userMap.get(t.getCreatorId());
                    List<TemplateTag> tts = ttMap.getOrDefault(t.getId(), List.of());
                    List<Long> tIdList = tts.stream().map(TemplateTag::getTagId).toList();
                    List<String> tNames = tts.stream()
                            .map(tt -> {
                                Tag tag = tagMap.get(tt.getTagId());
                                return tag != null ? tag.getName() : "";
                            })
                            .filter(n -> !n.isEmpty())
                            .toList();

                    return TemplateVO.builder()
                            .id(t.getId())
                            .creatorId(t.getCreatorId())
                            .creatorName(creator != null ? creator.getUsername() : "")
                            .creatorAvatar(creator != null ? creator.getAvatarUrl() : null)
                            .title(t.getTitle())
                            .description(t.getDescription())
                            .coverUrl(t.getCoverUrl())
                            .price(t.getPrice())
                            .status(t.getStatus())
                            .currentVersion(t.getCurrentVersion())
                            .useCount(t.getUseCount())
                            .favoriteCount(t.getFavoriteCount())
                            .reviewCount(t.getReviewCount())
                            .avgRating(t.getAvgRating())
                            .tagIds(tIdList)
                            .tagNames(tNames)
                            .createdAt(t.getCreatedAt())
                            .updatedAt(t.getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }
}
