package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("device")
public class Device {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String deviceCode;
    private String deviceName;
    private String brand;
    private String model;
    private String spec;
    private String category;
    private Long purchaseOrderId;
    private Long purchaseItemId;
    private Long stockInOrderId;
    private Long stockOutOrderId;
    private String warehouseName;
    private String installLocation;
    private LocalDate installDate;
    private String installPerson;
    private LocalDate useDate;
    private String acceptRecord;
    private String installFile;
    private String acceptPhoto;
    private Integer status;
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

    /** 入库单号（非数据库字段，用于展示） */
    @TableField(exist = false)
    private String stockInOrderNo;

    /** 出库单号（非数据库字段，用于展示） */
    @TableField(exist = false)
    private String stockOutOrderNo;

    /** 维修工单列表（非数据库字段，用于展示） */
    @TableField(exist = false)
    private java.util.List<BizRepairOrder> repairOrders;
}
