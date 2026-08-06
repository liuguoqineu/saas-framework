package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户表 (sys_user)
 * 包含超级账户(tenant_id=0)、租户管理员和租户员工
 */
@Data
@TableName("sys_user")
public class SysUser {

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名，登录用 */
    private String username;

    /** 密码，BCrypt加密存储 */
    @JsonIgnore
    private String password;

    /** 角色ID，关联 sys_role.id */
    private Long roleId;

    /** 租户ID，超级账户为0，租户用户为对应租户ID */
    private Long tenantId;

    /** 真实姓名 */
    private String realName;

    /** 状态：1-启用，0-禁用 */
    private Integer status;

    /** 头像URL */
    private String avatar;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 岗位类型 */
    private String postType;

    /** 领导ID */
    private Long leaderId;

    /** 资质证书内容 */
    private String zhizhiContent;

    /** 资质证书图片URL */
    private String zhizhiImageUrl;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0-未删除，1-已删除 */
    @TableLogic
    private Integer deleted;
}
