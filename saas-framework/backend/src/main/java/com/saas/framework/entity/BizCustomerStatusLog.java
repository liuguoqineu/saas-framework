package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户状态变更记录表 (biz_customer_status_log)
 * 记录客户合作状态的变更历史
 */
@Data
@TableName("biz_customer_status_log")
public class BizCustomerStatusLog {

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 客户ID */
    private Long customerId;

    /** 原合作状态 */
    private String oldCooperationStatus;

    /** 新合作状态 */
    private String newCooperationStatus;

    /** 变更原因 */
    private String changeReason;

    /** 关联跟进记录ID */
    private Long followUpRecordId;

    /** 变更人ID */
    private Long changePersonId;

    /** 变更人姓名 */
    private String changePerson;

    /** 变更时间 */
    private LocalDateTime changeTime;

    /** 租户ID，用于数据隔离 */
    private Long tenantId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
