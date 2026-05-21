package com.saas.framework.common.dto;

import lombok.Data;

@Data
public class RepairOrderRequest {

    private Long customerId;
    private String customerName;
    private String contactPerson;
    private String contactPhone;
    private String repairContent;
    private String repairType;
    private String repairTime;
    private String repairAddress;
    private String urgency;
    private String faultDescription;
}
