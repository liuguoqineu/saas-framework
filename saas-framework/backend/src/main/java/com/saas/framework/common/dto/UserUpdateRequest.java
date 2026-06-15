package com.saas.framework.common.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 修改员工请求 DTO
 */
@Data
public class UserUpdateRequest {

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    private Integer status;

    private String postType;
}
