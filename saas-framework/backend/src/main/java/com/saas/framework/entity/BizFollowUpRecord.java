package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 跟进记录表 (biz_follow_up_record)
 * 记录客户跟进的详细信息，包括跟进时间、方式、内容等
 */
@Data
@TableName("biz_follow_up_record")
public class BizFollowUpRecord {

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联客户ID */
    private Long customerId;

    /** 客户名称（非数据库字段，用于查询返回） */
    @TableField(exist = false)
    private String customerName;

    /** 跟进时间 */
    private LocalDateTime followUpTime;

    /** 跟进人ID */
    private Long followUpPersonId;

    /** 跟进人姓名 */
    private String followUpPerson;

    /** 跟进方式: 1-电话 2-微信 3-邮件 4-上门拜访 5-其他 */
    private Integer followUpMethod;

    /** 跟进内容 */
    private String followUpContent;

    /** 下一步计划 */
    private String nextPlan;

    /** 跟进状态: 1-待跟进 2-已跟进 3-已达成意向 */
    private Integer followUpStatus;

    /** 附件信息JSON */
    private String attachments;

    /** 租户ID，用于数据隔离 */
    private Long tenantId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人ID */
    private Long createBy;

    /** 逻辑删除：0-未删除，1-已删除 */
    @TableLogic
    private Integer deleted;
}
