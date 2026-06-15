package com.saas.framework.entity.report;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rp_report")
public class RpReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long deptId;
    private Long templateId;
    private String reportType;
    private String reportPeriod;
    private String contentText;
    private String status;
    private LocalDateTime submitTime;
    private Long tenantId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
