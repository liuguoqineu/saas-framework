package com.saas.framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 安全配置
 * 提供 BCrypt 密码编码器
 */
@Configuration
public class SecurityConfig {

    /**
     * BCrypt 密码编码器
     * 用于用户密码的加密存储和验证
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
