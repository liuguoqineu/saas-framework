package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色-权限关联表 (sys_role_permission)
 */
@Data
@TableName("sys_role_permission")
public class SysRolePermission {

    /** 角色ID */
    private Long roleId;

    /** 权限ID */
    private Long permissionId;
}
