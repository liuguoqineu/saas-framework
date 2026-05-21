package com.saas.framework.common.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class CustomerStatusChangeRequest {

    @NotBlank(message = "新合作一级分类不能为空")
    private String newCooperationCategory;

    @NotBlank(message = "新合作二级分类不能为空")
    private String newCooperationStatus;

    private Long followUpRecordId;

    @NotBlank(message = "变更原因不能为空")
    private String changeReason;
}
