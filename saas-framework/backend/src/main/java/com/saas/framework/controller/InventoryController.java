package com.saas.framework.controller;

import com.saas.framework.common.Result;
import com.saas.framework.common.dto.PageResult;
import com.saas.framework.common.dto.StockOutRequest;
import com.saas.framework.entity.DeviceInventory;
import com.saas.framework.entity.DeviceStockOutOrder;
import com.saas.framework.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/inventory")
@Tag(name = "库存台账", description = "仓库库存管理、出库操作")
public class InventoryController {

    @Resource
    private InventoryService inventoryService;

    @Operation(summary = "分页查询库存列表")
    @GetMapping("/page")
    public Result<PageResult<DeviceInventory>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String itemName,
            @RequestParam(required = false) String warehouseName,
            @RequestParam(required = false) Integer itemType,
            @RequestParam(required = false) Boolean lowStock) {
        log.info("查询库存列表: page={}, size={}, itemName={}, warehouseName={}, itemType={}, lowStock={}",
                page, size, itemName, warehouseName, itemType, lowStock);
        PageResult<DeviceInventory> result = inventoryService.page(page, size, itemName, warehouseName, itemType, lowStock);
        return Result.ok(result);
    }

    @Operation(summary = "查看库存详情")
    @GetMapping("/{id}")
    public Result<DeviceInventory> detail(@PathVariable Long id) {
        log.info("查看库存详情: id={}", id);
        DeviceInventory inventory = inventoryService.detail(id);
        return Result.ok(inventory);
    }

    @Operation(summary = "出库操作")
    @PostMapping("/stock-out")
    public Result<?> stockOut(@Valid @RequestBody StockOutRequest request) {
        log.info("库存出库: inventoryId={}, quantity={}, usageType={}", request.getInventoryId(), request.getQuantity(), request.getUsageType());
        inventoryService.stockOut(request);
        return Result.ok("出库操作成功");
    }

    @Operation(summary = "查询库存的出库记录")
    @GetMapping("/{id}/stock-out-orders")
    public Result<List<DeviceStockOutOrder>> listStockOutOrders(@PathVariable Long id) {
        log.info("查询出库记录: inventoryId={}", id);
        List<DeviceStockOutOrder> orders = inventoryService.listStockOutOrders(id);
        return Result.ok(orders);
    }

    @Operation(summary = "更新预警阈值")
    @PutMapping("/{id}/min-stock-qty")
    public Result<?> updateMinStockQty(@PathVariable Long id, @RequestBody java.util.Map<String, Integer> body) {
        Integer minStockQty = body.get("minStockQty");
        log.info("更新预警阈值: id={}, minStockQty={}", id, minStockQty);
        if (minStockQty != null && minStockQty < 0) {
            return Result.error("预警阈值不能为负数");
        }
        inventoryService.updateMinStockQty(id, minStockQty);
        return Result.ok("预警阈值更新成功");
    }
}
