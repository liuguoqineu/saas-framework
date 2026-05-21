package com.saas.framework.common.dto;

import lombok.Data;

@Data
public class RepairExceptionRequest {

    private String exceptionReason;
    private String secondPlan;
    private String secondRemindTime;
}
