package com.saas.framework.common.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ContractRequest {

    private String contractNo;
    private Long customerId;

    private String customerName;
    private String signDate;
    private String expireDate;
    private BigDecimal contractAmount;
    private String serviceContent;
    private String paymentMethod;
    private Long personInChargeId;
    private String personInCharge;
    private String remark;
    private String contractStatus;
}