package com.somehow.work.prompthub.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 路由拦截配置
 * - 登录校验：除注册/登录外，所有 /api/** 需要认证
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/user/register",
                        "/api/user/login",
                        "/api/tags",                     // 标签公开访问
                        "/api/templates/**",             // 模板浏览公开（创建/编辑需额外权限校验）
                        "/api/statistics/ranking",       // 排行公开
                        "/api/statistics/platform",      // 平台总览公开
                        "/api/activities"                // 活动列表公开（领取需登录）
                );
    }
}
