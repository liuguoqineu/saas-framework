package com.saas.framework.common.dto;

import lombok.Data;

@Data
public class IndependentStockInRequest {
    private Integer itemType; // 1-设备，2-配件
    private String itemName;
    private String brand;
    private String model;
    private String spec;
    private Integer quantity;
    private String unit;
    private String warehouseName;
    private String location;
    private String handler;
    private String remark;
}
