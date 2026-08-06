package com.saas.framework.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.saas.framework.common.dto.PageResult;
import com.saas.framework.common.dto.PurchaseOrderRequest;
import com.saas.framework.common.dto.StockInRequest;
import com.saas.framework.entity.DevicePurchaseOrder;
import com.saas.framework.entity.DeviceStockInOrder;

import java.util.List;

public interface PurchaseService {

    PageResult<DevicePurchaseOrder> page(int page, int size, String orderNo, String supplierName,
                                          String purchaseDateStart, String purchaseDateEnd, Integer status);

    DevicePurchaseOrder detail(Long id);

    DevicePurchaseOrder create(PurchaseOrderRequest request);

    void update(Long id, PurchaseOrderRequest request);

    void delete(Long id);

    void stockIn(StockInRequest request);

    List<DeviceStockInOrder> listStockInOrders(Long purchaseOrderId);
}
