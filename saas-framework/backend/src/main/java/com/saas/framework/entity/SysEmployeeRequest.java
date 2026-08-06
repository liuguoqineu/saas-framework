package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 员工申请表 (sys_employee_request)
 * 租户管理员提交新增员工申请，超级管理员审核
 */
@Data
@TableName("sys_employee_request")
public class SysEmployeeRequest {

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请的用户名 */
    private String username;

    /** 申请的真实姓名 */
    private String realName;

    /** 申请的角色ID */
    private Long roleId;

    /** 申请的岗位类型 */
    private String postType;

    /** 申请的密码（可选，默认123456） */
    private String password;

    /** 资质证书内容 */
    private String zhizhiContent;

    /** 资质证书图片URL */
    private String zhizhiImageUrl;

    /** 申请的租户ID */
    private Long tenantId;

    /** 申请人ID */
    private Long applicantId;

    /** 申请人姓名 */
    private String applicantName;

    /** 申请状态：PENDING-待审核，APPROVED-已通过，REJECTED-已拒绝 */
    private String status;

    /** 审核人ID */
    private Long reviewerId;

    /** 审核人姓名 */
    private String reviewerName;

    /** 审核意见 */
    private String reviewComment;

    /** 审核时间 */
    private LocalDateTime reviewTime;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
