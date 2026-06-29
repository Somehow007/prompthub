package com.somehow.work.prompthub.service;

import com.somehow.work.prompthub.dto.CreateReviewDTO;
import com.somehow.work.prompthub.vo.ReviewVO;

import java.util.List;

/**
 * 评价服务接口
 */
public interface ReviewService {

    /** 创建评价（事务：插入评价+更新模板评分） */
    ReviewVO create(CreateReviewDTO dto, Long userId);

    /** 模板评价列表 */
    List<ReviewVO> listByTemplate(Long templateId);

    /** 删除自己的评价 */
    void delete(Long reviewId, Long userId);
}
