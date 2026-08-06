package com.saas.framework.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private StringToLocalDateConverter stringToLocalDateConverter;

    @Resource
    private StringToLocalDateTimeConverter stringToLocalDateTimeConverter;

    @Resource
    private FilePathConfig filePathConfig;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(stringToLocalDateConverter);
        registry.addConverter(stringToLocalDateTimeConverter);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射上传文件目录，使头像等上传文件可通过URL访问
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + filePathConfig.getUploadPath());
    }
}
