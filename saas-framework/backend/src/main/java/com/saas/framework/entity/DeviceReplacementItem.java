package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("device_replacement_item")
public class DeviceReplacementItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long replacementId;
    private Integer itemType;
    private Long oldDeviceId;
    private String oldItemName;
    private String oldItemModel;
    private Integer oldItemStatus;
    private Long newDeviceId;
    private String newItemName;
    private String newItemModel;
    private Integer newItemQty;
    private Long stockOutOrderId;
    private Long newStockOutOrderId;
    private Long tenantId;

    /** 关联出库单号（非数据库字段） */
    @TableField(exist = false)
    private String stockOutOrderNo;

    /** 新设备出库单号（非数据库字段） */
    @TableField(exist = false)
    private String newStockOutOrderNo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
