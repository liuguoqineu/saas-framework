package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("device_purchase_order")
public class DevicePurchaseOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;
    private LocalDate purchaseDate;
    private String supplierName;
    private String supplierContact;
    private String supplierPhone;
    private String supplierAddress;
    private String supplierUnifiedCode;
    private BigDecimal totalAmount;
    private String purchaser;
    private String purchaserPhone;
    private Integer status;
    private String remark;
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    /** 采购明细（非数据库字段） */
    @TableField(exist = false)
    private java.util.List<DevicePurchaseItem> items;
}
