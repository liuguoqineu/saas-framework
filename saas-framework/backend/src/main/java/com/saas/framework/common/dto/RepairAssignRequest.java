package com.saas.framework.common.dto;

import lombok.Data;

@Data
public class RepairAssignRequest {

    private Long assigneeId;
    private String assigneeName;
}
