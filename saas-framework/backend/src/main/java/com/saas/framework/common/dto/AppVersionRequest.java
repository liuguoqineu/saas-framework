package com.saas.framework.common.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * APP版本创建/修改请求 DTO
 */
@Data
public class AppVersionRequest {

    @NotNull(message = "版本号不能为空")
    private Integer versionCode;

    @NotBlank(message = "版本名称不能为空")
    private String versionName;

    @NotBlank(message = "平台不能为空")
    private String platform;

    @NotBlank(message = "下载地址不能为空")
    private String downloadUrl;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件MD5校验值 */
    private String md5;

    /** 更新内容 */
    private String updateContent;

    /** 是否强制更新：0-否，1-是 */
    private Integer forceUpdate;

    /** 状态：0-禁用，1-启用 */
    private Integer status;
}
