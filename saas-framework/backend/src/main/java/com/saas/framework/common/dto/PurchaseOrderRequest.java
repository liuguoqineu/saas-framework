package com.saas.framework.common.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PurchaseOrderRequest {

    private Long id;
    private String orderNo;
    private LocalDate purchaseDate;
    private String supplierName;
    private String supplierContact;
    private String supplierPhone;
    private String supplierAddress;
    private String supplierUnifiedCode;
    private BigDecimal totalAmount;
    private String purchaser;
    private String purchaserPhone;
    private String remark;
    private List<PurchaseItemDTO> items;

    @Data
    public static class PurchaseItemDTO {
        private Long id;
        private Integer itemType;
        private String itemName;
        private String brand;
        private String model;
        private String spec;
        private Integer quantity;
        private String unit;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private String factoryNo;
        private String certFile;
        private String inspectFile;
        private String deliveryFile;
    }
}
