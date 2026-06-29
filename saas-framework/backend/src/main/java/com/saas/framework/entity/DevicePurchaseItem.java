package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("device_purchase_item")
public class DevicePurchaseItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;
    private Integer itemType;
    private String itemName;
    private String brand;
    private String model;
    private String spec;
    private Integer quantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String factoryNo;
    private String certFile;
    private String inspectFile;
    private String deliveryFile;
    private Integer stockedQty;
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    /** 剩余可入库数量（非数据库字段） */
    @TableField(exist = false)
    private Integer remainQty;
}
