package com.saas.framework.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * 过滤器注册配置
 * 注册 JWT 认证过滤器
 */
@Configuration
public class FilterConfig {

    @Resource
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtFilterRegistration() {
        FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(jwtAuthFilter);
        // 拦截所有 /api/** 请求
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }
}
