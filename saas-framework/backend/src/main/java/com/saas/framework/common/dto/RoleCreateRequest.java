package com.saas.framework.common.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 创建角色请求 DTO
 */
@Data
public class RoleCreateRequest {

    @NotBlank(message = "角色名称不能为空")
    private String name;

    /** 分配的权限ID列表 */
    private List<Long> permissionIds;

    /** 目标租户ID（超级管理员创建时指定，租户管理员创建时忽略） */
    private Long tenantId;
}
