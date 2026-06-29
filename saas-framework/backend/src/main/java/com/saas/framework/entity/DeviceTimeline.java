package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("device_timeline")
public class DeviceTimeline {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long deviceId;
    private Integer eventType;
    private LocalDateTime eventTime;
    private String eventTitle;
    private String eventDesc;
    private Long relatedId;
    private String relatedOrderNo;
    private String operator;
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
