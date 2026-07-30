package com.saas.framework.common.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ContractRequest {

    private String contractNo;
    private Long customerId;

    private String customerName;
    private LocalDate signDate;
    private LocalDate expireDate;
    private LocalDate renewDate;
    private BigDecimal contractAmount;
    private BigDecimal serviceFee;
    private String serviceContent;
    private String paymentMethod;
    private Long personInChargeId;
    private String personInCharge;
    private String remark;
    private String contractStatus;
}