package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * APP版本管理表 (app_version)
 */
@Data
@TableName("app_version")
public class AppVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 版本号（数字，用于比较大小） */
    private Integer versionCode;

    /** 版本名称（如1.0.0） */
    private String versionName;

    /** 平台：iOS/Android */
    private String platform;

    /** 下载地址 */
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

    /** 租户ID（0表示全局） */
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
