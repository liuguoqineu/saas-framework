package com.saas.framework.common.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 租户管理员提交员工申请 DTO
 */
@Data
public class EmployeeRequestCreateDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    /** 岗位类型 */
    private String postType;

    /** 密码（可选，默认123456） */
    private String password;

    /** 资质证书内容 */
    private String zhizhiContent;

    /** 资质证书图片URL */
    private String zhizhiImageUrl;
}
