package com.saas.framework.controller;

import com.saas.framework.common.Result;
import com.saas.framework.common.dto.IndependentStockInRequest;
import com.saas.framework.common.dto.PageResult;
import com.saas.framework.common.dto.StockInCheckRequest;
import com.saas.framework.entity.DeviceStockInOrder;
import com.saas.framework.service.StockInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/stock-in")
@Tag(name = "入库管理", description = "入库单查询、独立入库、入库验收")
public class StockInController {

    @Resource
    private StockInService stockInService;

    @Operation(summary = "分页查询入库单列表")
    @GetMapping("/page")
    public Result<PageResult<DeviceStockInOrder>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String itemName,
            @RequestParam(required = false) String warehouseName,
            @RequestParam(required = false) String stockInDateStart,
            @RequestParam(required = false) String stockInDateEnd,
            @RequestParam(required = false) Integer checkStatus) {
        log.info("查询入库单列表: page={}, size={}, orderNo={}, itemName={}, warehouseName={}, checkStatus={}",
                page, size, orderNo, itemName, warehouseName, checkStatus);
        PageResult<DeviceStockInOrder> result = stockInService.page(page, size, orderNo, itemName,
                warehouseName, stockInDateStart, stockInDateEnd, checkStatus);
        return Result.ok(result);
    }

    @Operation(summary = "查看入库单详情")
    @GetMapping("/{id}")
    public Result<DeviceStockInOrder> detail(@PathVariable Long id) {
        log.info("查看入库单详情: id={}", id);
        DeviceStockInOrder order = stockInService.detail(id);
        return Result.ok(order);
    }

    @Operation(summary = "独立入库（不关联采购单）")
    @PostMapping("/independent")
    public Result<?> independentStockIn(@Valid @RequestBody IndependentStockInRequest request) {
        log.info("独立入库: itemName={}, quantity={}, warehouseName={}",
                request.getItemName(), request.getQuantity(), request.getWarehouseName());
        stockInService.independentStockIn(request);
        return Result.ok("独立入库操作成功");
    }

    @Operation(summary = "入库验收")
    @PutMapping("/{id}/check")
    public Result<?> check(@PathVariable Long id, @Valid @RequestBody StockInCheckRequest request) {
        log.info("入库验收: id={}, checkStatus={}", id, request.getCheckStatus());
        stockInService.check(id, request);
        return Result.ok("验收操作成功");
    }
}
