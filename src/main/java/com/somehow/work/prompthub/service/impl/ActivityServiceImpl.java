package com.somehow.work.prompthub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.somehow.work.prompthub.dto.CreateActivityDTO;
import com.somehow.work.prompthub.entity.*;
import com.somehow.work.prompthub.exception.BusinessException;
import com.somehow.work.prompthub.mapper.*;
import com.somehow.work.prompthub.service.ActivityService;
import com.somehow.work.prompthub.service.OrderService;
import com.somehow.work.prompthub.vo.ActivityVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 限时活动服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final ActivityTemplateMapper activityTemplateMapper;
    private final TemplateMapper templateMapper;
    private final UserMapper userMapper;
    private final OrderMapper orderMapper;
    private final IncomeRecordMapper incomeRecordMapper;
    private final OrderService orderService;
    private final RedissonClient redissonClient;

    @Override
    @Transactional
    public ActivityVO createActivity(CreateActivityDTO dto) {
        // 校验时间
        if (dto.getEndTime().isBefore(dto.getStartTime()) || dto.getEndTime().isEqual(dto.getStartTime())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }

        // 校验模板存在且上架
        Template template = templateMapper.selectById(dto.getTemplateId());
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        if (template.getStatus() != 1) {
            throw new BusinessException("模板未上架，无法创建活动");
        }
        if (template.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("免费模板无需创建活动");
        }

        // 检查同一时间窗口是否已有活动
        Long count = activityTemplateMapper.selectCount(
                new LambdaQueryWrapper<ActivityTemplate>()
                        .eq(ActivityTemplate::getTemplateId, dto.getTemplateId())
                        .eq(ActivityTemplate::getStatus, 1));
        if (count > 0) {
            throw new BusinessException("该模板已有进行中的活动");
        }

        // 计算初始状态
        LocalDateTime now = LocalDateTime.now();
        int status = 2; // 默认未开始
        if (now.isAfter(dto.getStartTime()) && now.isBefore(dto.getEndTime())) {
            status = 1; // 刚好在时间窗口内
        } else if (now.isAfter(dto.getEndTime())) {
            throw new BusinessException("结束时间不能早于当前时间");
        }

        ActivityTemplate activity = new ActivityTemplate();
        activity.setTemplateId(dto.getTemplateId());
        activity.setStartTime(dto.getStartTime());
        activity.setEndTime(dto.getEndTime());
        activity.setTotalQuota(dto.getTotalQuota());
        activity.setRemainingQuota(dto.getTotalQuota());
        activity.setStatus(status);

        activityTemplateMapper.insert(activity);
        log.info("活动创建成功: id={}, templateId={}, quota={}", activity.getId(), dto.getTemplateId(), dto.getTotalQuota());

        return toVO(activity);
    }

    @Override
    public List<ActivityVO> listActivities() {
        // 查询所有活动，按状态和开始时间排序
        List<ActivityTemplate> activities = activityTemplateMapper.selectList(
                new LambdaQueryWrapper<ActivityTemplate>()
                        .orderByAsc(ActivityTemplate::getStatus)
                        .orderByDesc(ActivityTemplate::getStartTime));

        // 自动刷新活动状态（根据时间窗口）
        LocalDateTime now = LocalDateTime.now();
        for (ActivityTemplate a : activities) {
            boolean statusChanged = false;
            if (a.getStatus() == 2 && now.isAfter(a.getStartTime()) && now.isBefore(a.getEndTime())) {
                a.setStatus(1);
                statusChanged = true;
            } else if (a.getStatus() == 1 && now.isAfter(a.getEndTime())) {
                a.setStatus(0);
                statusChanged = true;
            }
            if (statusChanged) {
                activityTemplateMapper.updateById(a);
            }
        }

        return activities.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void claim(Long activityId, Long userId) {
        // 1. 分布式锁
        RLock lock = redissonClient.getLock("activity:lock:" + activityId);
        try {
            if (!lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                throw new BusinessException("活动太火爆了，请稍后再试");
            }

            // 2. 检查活动有效性
            ActivityTemplate activity = activityTemplateMapper.selectById(activityId);
            if (activity == null) {
                throw new BusinessException("活动不存在");
            }

            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(activity.getStartTime())) {
                throw new BusinessException("活动尚未开始");
            }
            if (now.isAfter(activity.getEndTime())) {
                throw new BusinessException("活动已结束");
            }
            if (activity.getStatus() != 1) {
                // 尝试自动刷新状态
                if (now.isAfter(activity.getStartTime()) && now.isBefore(activity.getEndTime())) {
                    activity.setStatus(1);
                    activityTemplateMapper.updateById(activity);
                } else {
                    throw new BusinessException("活动已结束");
                }
            }

            // 3. 检查剩余份数
            if (activity.getRemainingQuota() <= 0) {
                throw new BusinessException("已被抢光");
            }

            // 4. 检查是否已领取/购买
            if (orderService.isPurchased(activity.getTemplateId(), userId)) {
                throw new BusinessException("您已获取过此模板，无需重复领取");
            }

            // 5. 原子扣减库存
            int rows = activityTemplateMapper.decrementQuota(activityId);
            if (rows == 0) {
                throw new BusinessException("已被抢光");
            }

            // 6. 生成免费订单（金额=0），使用户获得模板访问权限
            Order order = new Order();
            order.setOrderNo(UUID.randomUUID().toString().replace("-", "").substring(0, 20));
            order.setUserId(userId);
            order.setTemplateId(activity.getTemplateId());
            order.setAmount(BigDecimal.ZERO);
            order.setStatus(1);
            order.setPayTime(now);
            orderMapper.insert(order);

            // 7. 更新模板使用统计
            Template template = templateMapper.selectById(activity.getTemplateId());
            if (template != null) {
                template.setUseCount(template.getUseCount() + 1);
                template.setUseCount7d((template.getUseCount7d() != null ? template.getUseCount7d() : 0) + 1);
                templateMapper.updateById(template);
            }

            log.info("活动领取成功: activityId={}, userId={}, templateId={}", activityId, userId, activity.getTemplateId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("系统繁忙，请稍后再试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // ──────────── 工具方法 ────────────

    private ActivityVO toVO(ActivityTemplate a) {
        String templateTitle = "";
        String creatorName = "";
        BigDecimal templatePrice = BigDecimal.ZERO;
        Double avgRating = 0.0;

        Template template = templateMapper.selectById(a.getTemplateId());
        if (template != null) {
            templateTitle = template.getTitle();
            templatePrice = template.getPrice();
            avgRating = template.getAvgRating() != null ? template.getAvgRating().doubleValue() : 0.0;

            User creator = userMapper.selectById(template.getCreatorId());
            if (creator != null) {
                creatorName = creator.getUsername();
            }
        }

        return ActivityVO.builder()
                .id(a.getId())
                .templateId(a.getTemplateId())
                .templateTitle(templateTitle)
                .creatorName(creatorName)
                .templatePrice(templatePrice)
                .avgRating(avgRating)
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .totalQuota(a.getTotalQuota())
                .remainingQuota(a.getRemainingQuota())
                .status(a.getStatus())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
