package com.somehow.work.prompthub.controller;

import com.somehow.work.prompthub.service.TagService;
import com.somehow.work.prompthub.util.Result;
import com.somehow.work.prompthub.vo.TagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 标签控制器
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /** 获取标签树（公开访问） */
    @GetMapping
    public Result<List<TagVO>> getTags() {
        List<TagVO> tags = tagService.getTags();
        return Result.ok(tags);
    }
}
