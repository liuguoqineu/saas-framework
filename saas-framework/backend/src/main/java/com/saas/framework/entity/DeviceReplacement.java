package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("device_replacement")
public class DeviceReplacement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String replacementNo;
    private Integer replacementType;
    private Long repairOrderId;
    private LocalDateTime replaceTime;
    private String replacePerson;
    private String replaceReason;
    private String replacePhoto;
    private String operator;
    private String remark;
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    /** 更换明细（非数据库字段） */
    @TableField(exist = false)
    private java.util.List<DeviceReplacementItem> items;

    /** 维修单号用于展示（非数据库字段） */
    @TableField(exist = false)
    private String repairNo;
}
