package com.somehow.work.prompthub.service;

import com.somehow.work.prompthub.vo.TemplateVO;

import java.util.List;

/**
 * 智能推荐服务接口
 */
public interface RecommendService {

    /** 获取个性化推荐模板（基于标签相似度） */
    List<TemplateVO> recommend(Long userId, int limit);
}
