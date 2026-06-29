package com.saas.framework.common.dto;

import lombok.Data;

@Data
public class StockInCheckRequest {
    private Integer checkStatus; // 0-待验收，1-验收通过，2-验收不通过
    private String checkPhoto;
}
