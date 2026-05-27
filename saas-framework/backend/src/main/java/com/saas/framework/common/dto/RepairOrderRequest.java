package com.saas.framework.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RepairOrderRequest {

    private Long customerId;
    private String customerName;
    private String contactPerson;
    private String contactPhone;
    private String repairContent;
    private String repairType;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime repairTime;
    private String repairAddress;
    private String urgency;
    private String faultDescription;
}
