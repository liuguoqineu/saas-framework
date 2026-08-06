package com.saas.framework.controller;

import com.saas.framework.common.Result;
import com.saas.framework.common.dto.IndependentStockOutRequest;
import com.saas.framework.common.dto.PageResult;
import com.saas.framework.entity.DeviceStockOutOrder;
import com.saas.framework.service.StockOutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/stock-out")
@Tag(name = "出库管理", description = "出库单查询、出库操作")
public class StockOutController {

    @Resource
    private StockOutService stockOutService;

    @Operation(summary = "分页查询出库单列表")
    @GetMapping("/page")
    public Result<PageResult<DeviceStockOutOrder>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String itemName,
            @RequestParam(required = false) Integer usageType,
            @RequestParam(required = false) String receiver,
            @RequestParam(required = false) String stockOutDateStart,
            @RequestParam(required = false) String stockOutDateEnd) {
        log.info("查询出库单列表: page={}, size={}, orderNo={}, itemName={}, usageType={}, receiver={}",
                page, size, orderNo, itemName, usageType, receiver);
        PageResult<DeviceStockOutOrder> result = stockOutService.page(page, size, orderNo, itemName,
                usageType, receiver, stockOutDateStart, stockOutDateEnd);
        return Result.ok(result);
    }

    @Operation(summary = "查看出库单详情")
    @GetMapping("/{id}")
    public Result<DeviceStockOutOrder> detail(@PathVariable Long id) {
        log.info("查看出库单详情: id={}", id);
        DeviceStockOutOrder order = stockOutService.detail(id);
        return Result.ok(order);
    }

    @Operation(summary = "出库操作")
    @PostMapping
    public Result<?> stockOut(@Valid @RequestBody IndependentStockOutRequest request) {
        log.info("独立出库: inventoryId={}, quantity={}, usageType={}",
                request.getInventoryId(), request.getQuantity(), request.getUsageType());
        stockOutService.stockOut(request);
        return Result.ok("出库操作成功");
    }
}
