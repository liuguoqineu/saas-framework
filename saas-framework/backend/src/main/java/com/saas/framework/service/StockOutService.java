package com.saas.framework.service;

import com.saas.framework.common.dto.PageResult;
import com.saas.framework.common.dto.IndependentStockOutRequest;
import com.saas.framework.entity.DeviceStockOutOrder;

public interface StockOutService {

    PageResult<DeviceStockOutOrder> page(int page, int size, String orderNo, String itemName, Integer usageType,
                                          String receiver, String stockOutDateStart, String stockOutDateEnd);

    DeviceStockOutOrder detail(Long id);

    void stockOut(IndependentStockOutRequest request);
}
