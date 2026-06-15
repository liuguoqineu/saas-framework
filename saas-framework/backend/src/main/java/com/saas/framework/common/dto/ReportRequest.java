package com.saas.framework.common.dto;

import lombok.Data;

@Data
public class ReportRequest {
    private Long templateId;
    private String reportType;
    private String reportPeriod;
    private String contentText;
}
