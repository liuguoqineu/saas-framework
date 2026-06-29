package com.saas.framework.common.dto;

import lombok.Data;

@Data
public class IndependentStockOutRequest {

    private Long inventoryId; // 库存记录ID
    private Integer quantity;
    private Integer usageType; // 1-新装设备，2-维修更换，3-抢修备用
    private Long repairOrderId; // 关联维修工单ID（usageType=2时填写）
    private String deptName;
    private String receiver;
    private String receiverPhone;
    private String reviewer;
    private String outPhoto;
    private String remark;
}
