package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("device_inventory")
public class DeviceInventory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer itemType;
    private String itemName;
    private String brand;
    private String model;
    private String spec;
    private String unit;
    private String warehouseName;
    private Integer totalQty;
    private Integer stockedInQty;
    private Integer stockedOutQty;
    private Integer minStockQty;
    private String remark;
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
