package com.saas.framework.common.dto;

import lombok.Data;

@Data
public class StockOutRequest {

    private Long inventoryId;
    private Integer quantity;
    private Integer usageType;
    private String deptName;
    private String receiver;
    private String receiverPhone;
    private String reviewer;
    private String remark;
}
