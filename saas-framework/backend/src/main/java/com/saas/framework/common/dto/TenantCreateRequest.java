package com.saas.framework.common.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 创建租户请求 DTO
 */
@Data
public class TenantCreateRequest {

    @NotBlank(message = "租户名称不能为空")
    private String name;

    @NotBlank(message = "租户编码不能为空")
    private String code;

    /** 管理员用户名（可选，不提供则用 code+"admin"） */
    private String adminUsername;

    /** 管理员密码（可选，不提供则随机生成6位） */
    private String adminPassword;
}
