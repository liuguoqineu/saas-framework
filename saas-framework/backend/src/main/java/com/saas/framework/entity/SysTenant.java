package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户信息表 (sys_tenant)
 * 仅超级账户可管理
 */
@Data
@TableName("sys_tenant")
public class SysTenant {

    /** 主键ID，自增，同时也是租户ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户名称（公司名称） */
    private String name;

    /** 租户编码，唯一标识，用于自动生成管理员账号 */
    private String code;

    /** 状态：1-启用，0-禁用 */
    private Integer status;

    /** 关联的管理员用户ID */
    private Long adminUserId;

    /** 管理员初始密码（明文，创建时展示给超级账户） */
    private String adminPassword;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
