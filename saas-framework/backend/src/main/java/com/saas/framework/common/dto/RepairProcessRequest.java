package com.saas.framework.common.dto;

import lombok.Data;

@Data
public class RepairProcessRequest {

    private String status;
    private String processMethod;
    private String replacedParts;
    private String faultReason;
}
