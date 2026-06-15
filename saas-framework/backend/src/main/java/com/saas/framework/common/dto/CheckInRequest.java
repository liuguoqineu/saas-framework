package com.saas.framework.common.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class CheckInRequest {

    @NotNull(message = "地址不能为空")
    private String address;

    private String remark;
}
