package com.somehow.work.prompthub.service;

import com.somehow.work.prompthub.vo.TagVO;

import java.util.List;

/**
 * 标签服务接口
 */
public interface TagService {

    /** 获取标签树 */
    List<TagVO> getTags();
}
