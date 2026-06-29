package com.saas.framework.service;

import com.saas.framework.common.dto.PageResult;
import com.saas.framework.common.dto.StockOutRequest;
import com.saas.framework.entity.DeviceInventory;
import com.saas.framework.entity.DeviceStockOutOrder;

import java.util.List;

public interface InventoryService {

    PageResult<DeviceInventory> page(int page, int size, String itemName, String warehouseName,
                                      Integer itemType, Boolean lowStock);

    DeviceInventory detail(Long id);

    void stockOut(StockOutRequest request);

    List<DeviceStockOutOrder> listStockOutOrders(Long inventoryId);

    void updateMinStockQty(Long id, Integer minStockQty);
}
