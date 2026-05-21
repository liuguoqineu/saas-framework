package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统权限表 (sys_permission)
 * 存储菜单权限和按钮权限，通过 parent_id 形成树形结构
 */
@Data
@TableName("sys_permission")
public class SysPermission {

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 权限名称，如"学生管理"、"学生列表" */
    private String name;

    /** 权限编码，如 "student:list"，按钮权限使用冒号分隔 */
    private String code;

    /** 权限类型：menu-菜单，button-按钮 */
    private String type;

    /** 父权限ID，用于构建权限树 */
    private Long parentId;

    /** 排序号 */
    private Integer sort;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
