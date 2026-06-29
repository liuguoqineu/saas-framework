package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("device_stock_in_order")
public class DeviceStockInOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;
    private Long purchaseOrderId;
    private Long purchaseItemId;
    private Integer itemType;
    private String itemName;
    private String brand;
    private String model;
    private String spec;
    private Integer quantity;
    private String unit;
    private Long deviceId;
    private String warehouseName;
    private String location;
    private Integer checkStatus;
    private String checkPhoto;
    private String handler;
    private String remark;
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    /** 采购单号（非数据库字段，用于展示） */
    @TableField(exist = false)
    private String purchaseOrderNo;
}
