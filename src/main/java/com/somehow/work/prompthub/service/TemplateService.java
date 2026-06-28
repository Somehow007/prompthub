package com.somehow.work.prompthub.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.somehow.work.prompthub.dto.TemplateCreateDTO;
import com.somehow.work.prompthub.dto.TemplateQueryDTO;
import com.somehow.work.prompthub.dto.TemplateUpdateDTO;
import com.somehow.work.prompthub.vo.TemplateDetailVO;
import com.somehow.work.prompthub.vo.TemplateVO;
import com.somehow.work.prompthub.vo.VersionVO;

import java.util.List;

/**
 * 模板服务接口
 */
public interface TemplateService {

    /** 创建模板（含 v1 版本和标签关联） */
    TemplateDetailVO create(TemplateCreateDTO dto, Long userId);

    /** 分页查询模板列表 */
    Page<TemplateVO> query(TemplateQueryDTO query);

    /** 模板详情（含当前版本 Prompt 和标签） */
    TemplateDetailVO getDetail(Long id);

    /** 编辑模板（自动生成新版本） */
    TemplateDetailVO update(Long id, TemplateUpdateDTO dto, Long userId);

    /** 上下架 */
    void updateStatus(Long id, Integer status, Long userId);

    /** 版本历史列表 */
    List<VersionVO> getVersions(Long templateId);

    /** 版本详情 */
    VersionVO getVersionDetail(Long versionId);

    /** 版本对比（返回两个版本的内容） */
    List<VersionVO> compareVersions(Long versionId1, Long versionId2);

    /** 回滚到指定版本 */
    TemplateDetailVO rollback(Long templateId, Long versionId, Long userId);

    /** 关键字搜索 */
    Page<TemplateVO> search(String keyword, int page, int size);
}
