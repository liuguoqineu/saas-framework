package com.saas.framework.common.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 检查APP更新请求 DTO
 */
@Data
public class AppVersionCheckRequest {

    @NotNull(message = "当前版本号不能为空")
    private Integer currentVersionCode;

    @NotBlank(message = "平台不能为空")
    private String platform;
}
