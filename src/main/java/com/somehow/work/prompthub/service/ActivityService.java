package com.somehow.work.prompthub.service;

import com.somehow.work.prompthub.dto.CreateActivityDTO;
import com.somehow.work.prompthub.vo.ActivityVO;

import java.util.List;

/**
 * 限时活动服务接口
 */
public interface ActivityService {

    /** 创建活动（管理员） */
    ActivityVO createActivity(CreateActivityDTO dto);

    /** 活动列表（按状态+时间排序） */
    List<ActivityVO> listActivities();

    /** 领取活动模板（Redis 分布式锁 + 事务） */
    void claim(Long activityId, Long userId);
}
