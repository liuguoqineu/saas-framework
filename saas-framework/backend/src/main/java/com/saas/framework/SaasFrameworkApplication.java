package com.saas.framework;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SaaS 多租户教学框架 - 启动类
 */
@SpringBootApplication
@MapperScan("com.saas.framework.mapper")
public class SaasFrameworkApplication {

    public static void main(String[] args) {
        SpringApplication.run(SaasFrameworkApplication.class, args);
        System.out.println("========================================");
        System.out.println("  SaaS 多租户教学框架启动成功！");
        System.out.println("  API 文档: http://localhost:8080/swagger-ui.html");
        System.out.println("========================================");
    }
}
