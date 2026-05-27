package com.saas.framework.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private StringToLocalDateConverter stringToLocalDateConverter;

    @Resource
    private StringToLocalDateTimeConverter stringToLocalDateTimeConverter;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(stringToLocalDateConverter);
        registry.addConverter(stringToLocalDateTimeConverter);
    }
}
