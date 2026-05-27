package com.saas.framework.common.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 客户请求 DTO（新增和修改共用）
 */
@Data
public class CustomerRequest {

    @NotBlank(message = "客户名称不能为空")
    private String name;

    private String address;

    private String detailAddress;

    private String region;

    private String contactPerson;

    private String contactPhone;

    @NotBlank(message = "业务分类不能为空")
    private String businessCategory;

    private String businessType;

    private String cooperationCategory;

    private String cooperationStatus;

    private String maintenanceCategory;

    private String gasScale;

    private String smartGasSystem;

    private String contractInfo;

    private Long followUpPersonId;

    private String followUpPerson;
}
