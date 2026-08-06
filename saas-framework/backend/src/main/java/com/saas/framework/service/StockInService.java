package com.saas.framework.service;

import com.saas.framework.common.dto.IndependentStockInRequest;
import com.saas.framework.common.dto.PageResult;
import com.saas.framework.common.dto.StockInCheckRequest;
import com.saas.framework.entity.DeviceStockInOrder;

public interface StockInService {
    PageResult<DeviceStockInOrder> page(int page, int size, String orderNo, String itemName, String warehouseName, String stockInDateStart, String stockInDateEnd, Integer checkStatus);
    DeviceStockInOrder detail(Long id);
    void independentStockIn(IndependentStockInRequest request);
    void check(Long id, StockInCheckRequest request);
}
