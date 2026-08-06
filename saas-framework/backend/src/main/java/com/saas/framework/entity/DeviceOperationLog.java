package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("device_operation_log")
public class DeviceOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String module;
    private String action;
    private Long targetId;
    private String targetNo;
    private String detail;
    private Long operatorId;
    private String operatorName;
    private String ip;
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
