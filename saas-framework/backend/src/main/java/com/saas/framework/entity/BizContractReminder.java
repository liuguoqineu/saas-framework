package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("biz_contract_reminder")
public class BizContractReminder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long contractId;
    private String contractNo;
    private String customerName;
    private Integer remindDays;
    private LocalDate remindDate;
    private Long personInChargeId;
    private String personInCharge;
    private Integer isRead;
    private Integer isHandled;
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
