package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("device_stock_out_order")
public class DeviceStockOutOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;
    private Integer itemType;
    private String itemName;
    private String brand;
    private String model;
    private String spec;
    private Integer quantity;
    private String unit;
    private Integer usageType;
    private Long outDeviceId;
    private Long repairOrderId;
    private String deptName;
    private String receiver;
    private String receiverPhone;
    private String reviewer;
    private String outPhoto;
    private String remark;
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
