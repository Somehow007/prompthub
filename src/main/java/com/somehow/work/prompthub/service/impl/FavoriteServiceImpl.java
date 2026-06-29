package com.somehow.work.prompthub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.somehow.work.prompthub.entity.*;
import com.somehow.work.prompthub.exception.BusinessException;
import com.somehow.work.prompthub.mapper.*;
import com.somehow.work.prompthub.service.FavoriteService;
import com.somehow.work.prompthub.vo.TemplateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 收藏服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final TemplateMapper templateMapper;
    private final UserMapper userMapper;
    private final TemplateTagMapper templateTagMapper;
    private final TagMapper tagMapper;

    @Override
    @Transactional
    public boolean toggle(Long templateId, Long userId) {
        // 检查模板存在
        Template template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }

        // 查找是否已收藏
        Favorite existing = favoriteMapper.selectOne(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getTemplateId, templateId));

        if (existing != null) {
            // 取消收藏
            favoriteMapper.deleteById(existing.getId());
            template.setFavoriteCount(Math.max(0, template.getFavoriteCount() - 1));
            templateMapper.updateById(template);
            log.info("取消收藏: userId={}, templateId={}", userId, templateId);
            return false;
        } else {
            // 添加收藏
            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setTemplateId(templateId);
            favoriteMapper.insert(favorite);
            template.setFavoriteCount(template.getFavoriteCount() + 1);
            templateMapper.updateById(template);
            log.info("添加收藏: userId={}, templateId={}", userId, templateId);
            return true;
        }
    }

    @Override
    public List<TemplateVO> listFavorites(Long userId) {
        List<Favorite> favorites = favoriteMapper.selectList(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreatedAt));

        if (favorites.isEmpty()) {
            return List.of();
        }

        List<Long> templateIds = favorites.stream()
                .map(Favorite::getTemplateId)
                .toList();

        List<Template> templates = templateMapper.selectBatchIds(templateIds);

        // 构建 VO（复用 TemplateServiceImpl 中的逻辑）
        // 简化：手动组装
        Set<Long> creatorIds = templates.stream()
                .map(Template::getCreatorId)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(creatorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 标签
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

    @Override
    public boolean isFavorited(Long templateId, Long userId) {
        return favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getTemplateId, templateId)) > 0;
    }
}
