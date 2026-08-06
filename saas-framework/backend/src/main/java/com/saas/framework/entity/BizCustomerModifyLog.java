package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户修改记录表 (biz_customer_modify_log)
 * 记录客户信息的每次修改，便于追溯
 * SaaS业务表标准格式：含 tenant_id + deleted 逻辑删除标记
 */
@Data
@TableName("biz_customer_modify_log")
public class BizCustomerModifyLog {

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联客户ID */
    private Long customerId;

    /** 修改字段名 */
    private String fieldName;

    /** 修改前的值 */
    private String oldValue;

    /** 修改后的值 */
    private String newValue;

    /** 修改人ID */
    private Long modifyUserId;

    /** 修改人用户名 */
    private String modifyUser;

    /** 修改时间 */
    private LocalDateTime modifyTime;

    /** 租户ID，用于数据隔离（SaaS多租户核心字段） */
    private Long tenantId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0-未删除，1-已删除（SaaS业务表标准标记） */
    @TableLogic
    private Integer deleted;
}
