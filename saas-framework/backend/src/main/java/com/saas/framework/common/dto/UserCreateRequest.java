package com.saas.framework.common.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 创建员工请求 DTO
 */
@Data
public class UserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    /** 密码（可选，默认123456） */
    private String password;

    /** 岗位类型 */
    private String postType;
}
