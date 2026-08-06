package com.saas.framework.common.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class DeviceRepairRequest {

    @NotNull(message = "设备ID不能为空")
    private Long deviceId;

    private String faultTime;

    private String faultPart;

    private String faultDescription;

    private String repairPhotoBefore;

    private String urgency;

    private String remark;
}
