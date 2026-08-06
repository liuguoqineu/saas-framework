package com.saas.framework.common.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 个人信息修改请求 DTO
 */
@Data
public class ProfileUpdateRequest {

    @Size(max = 50, message = "真实姓名最长50个字符")
    private String realName;

    @Size(max = 20, message = "手机号最长20个字符")
    private String phone;

    @Size(max = 100, message = "邮箱最长100个字符")
    private String email;
}
