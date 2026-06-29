package com.somehow.work.prompthub.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.somehow.work.prompthub.dto.TemplateCreateDTO;
import com.somehow.work.prompthub.dto.TemplateQueryDTO;
import com.somehow.work.prompthub.dto.TemplateUpdateDTO;
import com.somehow.work.prompthub.entity.*;
import com.somehow.work.prompthub.exception.BusinessException;
import com.somehow.work.prompthub.mapper.*;
import com.somehow.work.prompthub.service.TemplateService;
import com.somehow.work.prompthub.vo.TagVO;
import com.somehow.work.prompthub.vo.TemplateDetailVO;
import com.somehow.work.prompthub.vo.TemplateVO;
import com.somehow.work.prompthub.vo.VersionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模板服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final TemplateMapper templateMapper;
    private final TemplateVersionMapper versionMapper;
    private final TemplateTagMapper templateTagMapper;
    private final TagMapper tagMapper;
    private final UserMapper userMapper;
    private final UsageLogMapper usageLogMapper;

    // ──────────────────── 创建 ────────────────────

    @Override
    @Transactional
    public TemplateDetailVO create(TemplateCreateDTO dto, Long userId) {
        // 1. 插入模板主记录
        Template template = new Template();
        template.setCreatorId(userId);
        template.setTitle(dto.getTitle());
        template.setDescription(dto.getDescription());
        template.setCoverUrl(dto.getCoverUrl());
        template.setCurrentVersion(1);
        template.setPrice(dto.getPrice() != null ? dto.getPrice() : BigDecimal.ZERO);
        template.setStatus(1); // 默认上架
        templateMapper.insert(template);

        // 2. 插入 v1 版本
        TemplateVersion version = new TemplateVersion();
        version.setTemplateId(template.getId());
        version.setVersionNumber(1);
        version.setPromptContent(dto.getPromptContent());
        version.setChangeNote("初始版本");
        versionMapper.insert(version);

        // 3. 关联标签
        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            saveTags(template.getId(), dto.getTagIds());
        }

        log.info("模板创建成功: id={}, title={}", template.getId(), template.getTitle());
        return getDetail(template.getId());
    }

    // ──────────────────── 列表查询 ────────────────────

    @Override
    public Page<TemplateVO> query(TemplateQueryDTO query) {
        // 如果有关键字，走全文搜索
        if (StrUtil.isNotBlank(query.getKeyword())) {
            return search(query.getKeyword(), query.getPage(), query.getSize());
        }

        LambdaQueryWrapper<Template> wrapper = new LambdaQueryWrapper<>();

        // 状态筛选
        if (query.getStatus() != null) {
            wrapper.eq(Template::getStatus, query.getStatus());
        }

        // 创作者筛选
        if (query.getCreatorId() != null) {
            wrapper.eq(Template::getCreatorId, query.getCreatorId());
        }

        // 价格区间
        if (query.getMinPrice() != null) {
            wrapper.ge(Template::getPrice, query.getMinPrice());
        }
        if (query.getMaxPrice() != null) {
            wrapper.le(Template::getPrice, query.getMaxPrice());
        }

        // 标签筛选（通过 EXISTS 子查询）
        if (query.getTagId() != null) {
            wrapper.exists("SELECT 1 FROM template_tag WHERE template_id = template.id AND tag_id = " + query.getTagId());
        }

        // 排序
        buildSort(wrapper, query.getSortBy());

        Page<Template> page = templateMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()), wrapper);

        return toTemplateVOPage(page);
    }

    // ──────────────────── 详情 ────────────────────

    @Override
    public TemplateDetailVO getDetail(Long id) {
        Template template = templateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }

        return toDetailVO(template);
    }

    // ──────────────────── 编辑（生成新版本） ────────────────────

    @Override
    @Transactional
    public TemplateDetailVO update(Long id, TemplateUpdateDTO dto, Long userId) {
        Template template = checkOwnership(id, userId);

        // 更新模板主记录
        template.setTitle(dto.getTitle());
        template.setDescription(dto.getDescription());
        template.setCoverUrl(dto.getCoverUrl());
        template.setPrice(dto.getPrice() != null ? dto.getPrice() : BigDecimal.ZERO);
        template.setCurrentVersion(template.getCurrentVersion() + 1);
        templateMapper.updateById(template);

        // 插入新版本
        TemplateVersion version = new TemplateVersion();
        version.setTemplateId(template.getId());
        version.setVersionNumber(template.getCurrentVersion());
        version.setPromptContent(dto.getPromptContent());
        version.setChangeNote(StrUtil.isNotBlank(dto.getChangeNote()) ? dto.getChangeNote() : "编辑模板");
        versionMapper.insert(version);

        // 更新标签关联
        if (dto.getTagIds() != null) {
            // 删除旧关联
            templateTagMapper.delete(
                    new LambdaQueryWrapper<TemplateTag>().eq(TemplateTag::getTemplateId, id));
            // 插入新关联
            if (!dto.getTagIds().isEmpty()) {
                saveTags(id, dto.getTagIds());
            }
        }

        log.info("模板编辑成功: id={}, newVersion={}", id, template.getCurrentVersion());
        return getDetail(id);
    }

    // ──────────────────── 上下架 ────────────────────

    @Override
    public void updateStatus(Long id, Integer status, Long userId) {
        Template template = checkOwnership(id, userId);
        if (status != 0 && status != 1 && status != 2) {
            throw new BusinessException("状态值无效，仅支持：1-上架 0-下架 2-审核中");
        }
        template.setStatus(status);
        templateMapper.updateById(template);
        log.info("模板状态变更: id={}, status={}", id, status);
    }

    // ──────────────────── 版本管理 ────────────────────

    @Override
    public List<VersionVO> getVersions(Long templateId) {
        List<TemplateVersion> versions = versionMapper.selectList(
                new LambdaQueryWrapper<TemplateVersion>()
                        .eq(TemplateVersion::getTemplateId, templateId)
                        .orderByDesc(TemplateVersion::getVersionNumber));

        return versions.stream().map(this::toVersionVO).collect(Collectors.toList());
    }

    @Override
    public VersionVO getVersionDetail(Long versionId) {
        TemplateVersion version = versionMapper.selectById(versionId);
        if (version == null) {
            throw new BusinessException("版本不存在");
        }
        return toVersionVO(version);
    }

    @Override
    public List<VersionVO> compareVersions(Long versionId1, Long versionId2) {
        VersionVO v1 = getVersionDetail(versionId1);
        VersionVO v2 = getVersionDetail(versionId2);
        return List.of(v1, v2);
    }

    @Override
    @Transactional
    public TemplateDetailVO rollback(Long templateId, Long versionId, Long userId) {
        Template template = checkOwnership(templateId, userId);

        // 获取目标版本
        TemplateVersion targetVersion = versionMapper.selectById(versionId);
        if (targetVersion == null || !targetVersion.getTemplateId().equals(templateId)) {
            throw new BusinessException("目标版本不存在或不属于此模板");
        }

        // 生成新版本（内容与目标版本相同）
        int newVersionNum = template.getCurrentVersion() + 1;
        template.setCurrentVersion(newVersionNum);
        templateMapper.updateById(template);

        TemplateVersion newVersion = new TemplateVersion();
        newVersion.setTemplateId(templateId);
        newVersion.setVersionNumber(newVersionNum);
        newVersion.setPromptContent(targetVersion.getPromptContent());
        newVersion.setChangeNote("回滚到 v" + targetVersion.getVersionNumber());
        versionMapper.insert(newVersion);

        log.info("模板回滚成功: id={}, from=v{}, to=v{}", templateId, targetVersion.getVersionNumber(), newVersionNum);
        return getDetail(templateId);
    }

    // ──────────────────── 记录使用 ────────────────────

    @Override
    @Transactional
    public void recordUse(Long templateId, Long userId, String inputParams) {
        Template template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }

        // 记录使用日志
        UsageLog log = new UsageLog();
        log.setUserId(userId);
        log.setTemplateId(templateId);
        log.setInputParams(inputParams);
        log.setCreatedAt(LocalDateTime.now());
        usageLogMapper.insert(log);

        // 更新使用统计
        template.setUseCount(template.getUseCount() + 1);
        template.setUseCount7d((template.getUseCount7d() != null ? template.getUseCount7d() : 0) + 1);
        templateMapper.updateById(template);
    }

    // ──────────────────── 搜索 ────────────────────

    @Override
    public Page<TemplateVO> search(String keyword, int page, int size) {
        long offset = (page - 1) * (long) size;

        // 优先使用 LIKE 模糊搜索，支持部分关键字匹配
        List<Template> list = templateMapper.searchByKeywordLike(keyword);

        // 如果 LIKE 没结果，尝试全文索引作为补充
        if (list.isEmpty() && keyword.length() >= 2) {
            list = templateMapper.searchByKeyword(keyword);
        }

        // 手动分页
        int total = list.size();
        int fromIndex = (int) Math.min(offset, total);
        int toIndex = (int) Math.min(offset + size, total);
        List<Template> pagedList = list.subList(fromIndex, toIndex);

        Page<Template> resultPage = new Page<>(page, size, total);
        resultPage.setRecords(pagedList);

        return toTemplateVOPage(resultPage);
    }

    // ──────────────────── 私有辅助方法 ────────────────────

    /** 检查模板所有权，返回模板实体 */
    private Template checkOwnership(Long templateId, Long userId) {
        Template template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        if (!template.getCreatorId().equals(userId)) {
            throw new BusinessException("无权操作此模板");
        }
        return template;
    }

    /** 保存标签关联 */
    private void saveTags(Long templateId, List<Long> tagIds) {
        for (Long tagId : tagIds) {
            TemplateTag tt = new TemplateTag();
            tt.setTemplateId(templateId);
            tt.setTagId(tagId);
            templateTagMapper.insert(tt);
        }
    }

    /** 构建排序条件 */
    private void buildSort(LambdaQueryWrapper<Template> wrapper, String sortBy) {
        switch (sortBy != null ? sortBy : "hot") {
            case "newest" -> wrapper.orderByDesc(Template::getCreatedAt);
            case "rating" -> wrapper.orderByDesc(Template::getAvgRating);
            case "price_asc" -> wrapper.orderByAsc(Template::getPrice);
            case "price_desc" -> wrapper.orderByDesc(Template::getPrice);
            default -> // hot: 综合使用量降序
                wrapper.orderByDesc(Template::getUseCount);
        }
    }

    /** 分页结果转 TemplateVO 分页 */
    private Page<TemplateVO> toTemplateVOPage(Page<Template> page) {
        // 收集创作者 ID
        Set<Long> creatorIds = page.getRecords().stream()
                .map(Template::getCreatorId)
                .collect(Collectors.toSet());

        // 批量查用户
        Map<Long, User> userMap = Map.of();
        if (!creatorIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(creatorIds);
            userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
        }

        // 批量查标签（收集所有模板的标签）
        Set<Long> templateIds = page.getRecords().stream()
                .map(Template::getId)
                .collect(Collectors.toSet());
        Map<Long, List<TemplateTag>> templateTagsMap = Map.of();
        Map<Long, Tag> tagMap = Map.of();
        if (!templateIds.isEmpty()) {
            List<TemplateTag> allTTs = templateTagMapper.selectList(
                    new LambdaQueryWrapper<TemplateTag>().in(TemplateTag::getTemplateId, templateIds));
            templateTagsMap = allTTs.stream().collect(Collectors.groupingBy(TemplateTag::getTemplateId));
            Set<Long> tagIds = allTTs.stream().map(TemplateTag::getTagId).collect(Collectors.toSet());
            if (!tagIds.isEmpty()) {
                List<Tag> tags = tagMapper.selectBatchIds(tagIds);
                tagMap = tags.stream().collect(Collectors.toMap(Tag::getId, t -> t));
            }
        }

        // 组装 VO
        Map<Long, User> finalUserMap = userMap;
        Map<Long, List<TemplateTag>> finalTTMap = templateTagsMap;
        Map<Long, Tag> finalTagMap = tagMap;

        List<TemplateVO> voList = page.getRecords().stream()
                .map(t -> {
                    User creator = finalUserMap.get(t.getCreatorId());
                    List<TemplateTag> tts = finalTTMap.getOrDefault(t.getId(), List.of());
                    List<Long> tIds = tts.stream().map(TemplateTag::getTagId).toList();
                    List<String> tNames = tts.stream()
                            .map(tt -> {
                                Tag tag = finalTagMap.get(tt.getTagId());
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
                            .tagIds(tIds)
                            .tagNames(tNames)
                            .createdAt(t.getCreatedAt())
                            .updatedAt(t.getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());

        Page<TemplateVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(voList);
        return result;
    }

    /** 模板实体转详情 VO */
    private TemplateDetailVO toDetailVO(Template template) {
        // 查询创作者
        User creator = userMapper.selectById(template.getCreatorId());

        // 查询当前版本
        TemplateVersion currentVer = versionMapper.selectOne(
                new LambdaQueryWrapper<TemplateVersion>()
                        .eq(TemplateVersion::getTemplateId, template.getId())
                        .eq(TemplateVersion::getVersionNumber, template.getCurrentVersion()));

        // 查询标签
        List<TemplateTag> tts = templateTagMapper.selectList(
                new LambdaQueryWrapper<TemplateTag>().eq(TemplateTag::getTemplateId, template.getId()));
        List<TagVO> tagVOs = List.of();
        if (!tts.isEmpty()) {
            Set<Long> tagIds = tts.stream().map(TemplateTag::getTagId).collect(Collectors.toSet());
            List<Tag> tags = tagMapper.selectBatchIds(tagIds);
            tagVOs = tags.stream()
                    .map(t -> TagVO.builder()
                            .id(t.getId())
                            .name(t.getName())
                            .parentId(t.getParentId())
                            .level(t.getLevel())
                            .sortOrder(t.getSortOrder())
                            .children(List.of())
                            .build())
                    .collect(Collectors.toList());
        }

        return TemplateDetailVO.builder()
                .id(template.getId())
                .creatorId(template.getCreatorId())
                .creatorName(creator != null ? creator.getUsername() : "")
                .creatorAvatar(creator != null ? creator.getAvatarUrl() : null)
                .title(template.getTitle())
                .description(template.getDescription())
                .coverUrl(template.getCoverUrl())
                .price(template.getPrice())
                .status(template.getStatus())
                .currentVersion(template.getCurrentVersion())
                .promptContent(currentVer != null ? currentVer.getPromptContent() : "")
                .changeNote(currentVer != null ? currentVer.getChangeNote() : "")
                .useCount(template.getUseCount())
                .favoriteCount(template.getFavoriteCount())
                .reviewCount(template.getReviewCount())
                .avgRating(template.getAvgRating())
                .tags(tagVOs)
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }

    /** 版本实体转 VO */
    private VersionVO toVersionVO(TemplateVersion v) {
        return VersionVO.builder()
                .id(v.getId())
                .templateId(v.getTemplateId())
                .versionNumber(v.getVersionNumber())
                .promptContent(v.getPromptContent())
                .changeNote(v.getChangeNote())
                .createdAt(v.getCreatedAt())
                .build();
    }
}
