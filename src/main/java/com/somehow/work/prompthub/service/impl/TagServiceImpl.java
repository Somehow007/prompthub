package com.somehow.work.prompthub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.somehow.work.prompthub.entity.Tag;
import com.somehow.work.prompthub.mapper.TagMapper;
import com.somehow.work.prompthub.service.TagService;
import com.somehow.work.prompthub.vo.TagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 标签服务实现
 */
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;

    @Override
    public List<TagVO> getTags() {
        // 查询全部标签，按排序号升序
        List<Tag> allTags = tagMapper.selectList(
                new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getSortOrder));

        // 转换为 VO
        List<TagVO> allVOs = allTags.stream()
                .map(t -> TagVO.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .parentId(t.getParentId())
                        .level(t.getLevel())
                        .sortOrder(t.getSortOrder())
                        .children(new ArrayList<>())
                        .build())
                .collect(Collectors.toList());

        // 按 parentId 分组
        Map<Long, List<TagVO>> parentMap = allVOs.stream()
                .filter(v -> v.getParentId() != null)
                .collect(Collectors.groupingBy(TagVO::getParentId));

        // 组装树：为每个父节点设置 children
        for (TagVO vo : allVOs) {
            List<TagVO> children = parentMap.get(vo.getId());
            if (children != null) {
                vo.setChildren(children);
            }
        }

        // 返回顶级节点
        return allVOs.stream()
                .filter(v -> v.getParentId() == null)
                .collect(Collectors.toList());
    }
}
