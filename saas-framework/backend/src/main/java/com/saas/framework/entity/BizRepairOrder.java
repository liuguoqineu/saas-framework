package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("biz_repair_order")
public class BizRepairOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String repairNo;
    private Long customerId;
    private String customerName;
    private String contactPerson;
    private String contactPhone;
    private String repairContent;
    private String repairType;
    private LocalDateTime repairTime;
    private String repairAddress;
    private String urgency;
    private String status;
    private String faultDescription;
    private Long assigneeId;
    private String assigneeName;
    private LocalDateTime assignTime;
    private Long assignerId;
    private String assignerName;
    private LocalDateTime processTime;
    private String processMethod;
    private String replacedParts;
    private String faultReason;
    private Integer confirmStatus;
    private LocalDateTime confirmTime;
    private String confirmPerson;
    private Integer isException;
    private String exceptionReason;
    private String secondPlan;
    private LocalDateTime secondRemindTime;
    private Long creatorId;
    private String creatorName;
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
