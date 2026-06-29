package com.somehow.work.prompthub.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.somehow.work.prompthub.dto.TemplateCreateDTO;
import com.somehow.work.prompthub.dto.TemplateQueryDTO;
import com.somehow.work.prompthub.dto.TemplateUpdateDTO;
import com.somehow.work.prompthub.service.TemplateService;
import com.somehow.work.prompthub.util.Result;
import com.somehow.work.prompthub.vo.TemplateDetailVO;
import com.somehow.work.prompthub.vo.TemplateVO;
import com.somehow.work.prompthub.vo.VersionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模板控制器
 */
@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    // ──────────── 模板 CRUD ────────────

    /** 创建模板（需登录） */
    @PostMapping
    public Result<TemplateDetailVO> create(@Valid @RequestBody TemplateCreateDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        TemplateDetailVO vo = templateService.create(dto, userId);
        return Result.ok("创建成功", vo);
    }

    /** 模板列表（公开，支持分页+筛选+排序） */
    @GetMapping
    public Result<Page<TemplateVO>> list(TemplateQueryDTO query) {
        Page<TemplateVO> page = templateService.query(query);
        return Result.ok(page);
    }

    /** 模板详情（公开，登录后返回购买状态） */
    @GetMapping("/{id}")
    public Result<TemplateDetailVO> detail(@PathVariable Long id) {
        Long currentUserId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        TemplateDetailVO vo = templateService.getDetail(id, currentUserId);
        return Result.ok(vo);
    }

    /** 编辑模板（需登录，仅创作者本人可操作） */
    @PutMapping("/{id}")
    public Result<TemplateDetailVO> update(@PathVariable Long id, @Valid @RequestBody TemplateUpdateDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        TemplateDetailVO vo = templateService.update(id, dto, userId);
        return Result.ok("编辑成功", vo);
    }

    /** 上下架（需登录） */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        long userId = StpUtil.getLoginIdAsLong();
        templateService.updateStatus(id, status, userId);
        return Result.ok(status == 1 ? "上架成功" : "下架成功", null);
    }

    // ──────────── 版本管理 ────────────

    /** 版本历史列表（公开） */
    @GetMapping("/{id}/versions")
    public Result<List<VersionVO>> versions(@PathVariable Long id) {
        List<VersionVO> versions = templateService.getVersions(id);
        return Result.ok(versions);
    }

    /** 版本详情（公开） */
    @GetMapping("/versions/{versionId}")
    public Result<VersionVO> versionDetail(@PathVariable Long versionId) {
        VersionVO vo = templateService.getVersionDetail(versionId);
        return Result.ok(vo);
    }

    /** 版本对比（公开） */
    @GetMapping("/versions/compare")
    public Result<List<VersionVO>> compareVersions(@RequestParam Long v1, @RequestParam Long v2) {
        List<VersionVO> versions = templateService.compareVersions(v1, v2);
        return Result.ok(versions);
    }

    /** 回滚到指定版本（需登录） */
    @PostMapping("/{id}/rollback/{versionId}")
    public Result<TemplateDetailVO> rollback(@PathVariable Long id, @PathVariable Long versionId) {
        long userId = StpUtil.getLoginIdAsLong();
        TemplateDetailVO vo = templateService.rollback(id, versionId, userId);
        return Result.ok("回滚成功", vo);
    }

    // ──────────── 搜索 ────────────

    /** 关键字搜索（公开） */
    @GetMapping("/search")
    public Result<Page<TemplateVO>> search(@RequestParam String keyword,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "12") int size) {
        Page<TemplateVO> result = templateService.search(keyword, page, size);
        return Result.ok(result);
    }

    // ──────────── 使用日志 ────────────

    /** 记录模板使用（需登录） */
    @PostMapping("/{id}/use")
    public Result<Void> recordUse(@PathVariable Long id, @RequestParam(required = false) String inputParams) {
        long userId = StpUtil.getLoginIdAsLong();
        templateService.recordUse(id, userId, inputParams);
        return Result.ok("已记录使用", null);
    }
}
