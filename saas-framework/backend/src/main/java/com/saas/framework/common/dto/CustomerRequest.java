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

    /** 合作状态: 正常履约/终止合作/高潜力/中潜力/低潜力/无效客户 */
    private String cooperationStatus;

    private String maintenanceCategory;

    private String gasScale;

    private String smartGasSystem;

    private String contractInfo;

    private Long followUpPersonId;

    private String followUpPerson;
}
