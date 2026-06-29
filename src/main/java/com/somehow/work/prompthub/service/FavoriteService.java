package com.somehow.work.prompthub.service;

import com.somehow.work.prompthub.vo.TemplateVO;

import java.util.List;

/**
 * 收藏服务接口
 */
public interface FavoriteService {

    /** 切换收藏状态（收藏/取消收藏），返回是否已收藏 */
    boolean toggle(Long templateId, Long userId);

    /** 我的收藏列表 */
    List<TemplateVO> listFavorites(Long userId);

    /** 是否已收藏 */
    boolean isFavorited(Long templateId, Long userId);
}
