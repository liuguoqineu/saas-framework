package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("biz_repair_process_log")
public class BizRepairProcessLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long repairId;
    private String action;
    private String oldStatus;
    private String newStatus;
    private String content;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime operateTime;
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
