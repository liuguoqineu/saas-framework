package com.saas.framework.common.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class StockInRequest {

    private Long purchaseOrderId;
    private String warehouseName;
    private LocalDate stockInDate;
    private String handler;
    private String remark;
    private List<StockInItemDTO> items;

    @Data
    public static class StockInItemDTO {
        private Long purchaseItemId;
        private Integer quantity;
        private String remark;
    }
}
