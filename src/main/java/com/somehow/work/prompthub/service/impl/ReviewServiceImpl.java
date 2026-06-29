package com.somehow.work.prompthub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.somehow.work.prompthub.dto.CreateReviewDTO;
import com.somehow.work.prompthub.entity.Review;
import com.somehow.work.prompthub.entity.Template;
import com.somehow.work.prompthub.entity.User;
import com.somehow.work.prompthub.exception.BusinessException;
import com.somehow.work.prompthub.mapper.ReviewMapper;
import com.somehow.work.prompthub.mapper.TemplateMapper;
import com.somehow.work.prompthub.mapper.UserMapper;
import com.somehow.work.prompthub.service.ReviewService;
import com.somehow.work.prompthub.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评价服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final TemplateMapper templateMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public ReviewVO create(CreateReviewDTO dto, Long userId) {
        // 1. 检查模板存在
        Template template = templateMapper.selectById(dto.getTemplateId());
        if (template == null) {
            throw new BusinessException("模板不存在");
        }

        // 2. 检查是否已评价
        Long count = reviewMapper.selectCount(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getUserId, userId)
                        .eq(Review::getTemplateId, dto.getTemplateId()));
        if (count > 0) {
            throw new BusinessException("您已经评价过此模板");
        }

        // 3. 插入评价
        Review review = new Review();
        review.setUserId(userId);
        review.setTemplateId(dto.getTemplateId());
        review.setRating(dto.getRating());
        review.setContent(dto.getContent());
        review.setCreatedAt(LocalDateTime.now());
        reviewMapper.insert(review);

        // 4. 更新模板评分统计
        // 计算平均分
        List<Review> allReviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getTemplateId, dto.getTemplateId()));
        double avg = allReviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
        BigDecimal avgRating = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP);

        template.setReviewCount(allReviews.size());
        template.setAvgRating(avgRating);
        templateMapper.updateById(template);

        log.info("评价成功: userId={}, templateId={}, rating={}", userId, dto.getTemplateId(), dto.getRating());

        // 5. 构建 VO（含用户名）
        User user = userMapper.selectById(userId);
        return ReviewVO.builder()
                .id(review.getId())
                .userId(review.getUserId())
                .username(user != null ? user.getUsername() : "")
                .avatarUrl(user != null ? user.getAvatarUrl() : null)
                .templateId(review.getTemplateId())
                .rating(review.getRating())
                .content(review.getContent())
                .createdAt(review.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void delete(Long reviewId, Long userId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        if (!review.getUserId().equals(userId)) {
            throw new BusinessException("只能删除自己的评价");
        }

        Long templateId = review.getTemplateId();
        reviewMapper.deleteById(reviewId);

        // 更新模板评分统计
        List<Review> allReviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getTemplateId, templateId));
        Template template = templateMapper.selectById(templateId);
        if (template != null) {
            if (allReviews.isEmpty()) {
                template.setReviewCount(0);
                template.setAvgRating(BigDecimal.ZERO);
            } else {
                double avg = allReviews.stream()
                        .mapToInt(Review::getRating)
                        .average()
                        .orElse(0.0);
                template.setReviewCount(allReviews.size());
                template.setAvgRating(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
            }
            templateMapper.updateById(template);
        }

        log.info("评价删除成功: reviewId={}, userId={}", reviewId, userId);
    }

    @Override
    public List<ReviewVO> listByTemplate(Long templateId) {
        List<Review> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getTemplateId, templateId)
                        .orderByDesc(Review::getCreatedAt));

        if (reviews.isEmpty()) {
            return List.of();
        }

        // 批量获取用户名
        List<Long> userIds = reviews.stream()
                .map(Review::getUserId)
                .distinct()
                .toList();
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return reviews.stream()
                .map(r -> {
                    User u = userMap.get(r.getUserId());
                    return ReviewVO.builder()
                            .id(r.getId())
                            .userId(r.getUserId())
                            .username(u != null ? u.getUsername() : "")
                            .avatarUrl(u != null ? u.getAvatarUrl() : null)
                            .templateId(r.getTemplateId())
                            .rating(r.getRating())
                            .content(r.getContent())
                            .createdAt(r.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }
}
