package com.saas.framework.controller;

import com.saas.framework.common.Result;
import com.saas.framework.common.dto.PageResult;
import com.saas.framework.common.dto.PurchaseOrderRequest;
import com.saas.framework.common.dto.StockInRequest;
import com.saas.framework.config.FilePathConfig;
import com.saas.framework.entity.DevicePurchaseOrder;
import com.saas.framework.entity.DeviceStockInOrder;
import com.saas.framework.service.PurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/purchase")
@Tag(name = "采购管理", description = "采购单管理、入库操作")
public class PurchaseController {

    @Resource
    private PurchaseService purchaseService;

    @Resource
    private FilePathConfig filePathConfig;

    @Operation(summary = "分页查询采购列表")
    @GetMapping("/page")
    public Result<PageResult<DevicePurchaseOrder>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String supplierName,
            @RequestParam(required = false) String purchaseDateStart,
            @RequestParam(required = false) String purchaseDateEnd,
            @RequestParam(required = false) Integer status) {
        log.info("查询采购列表: page={}, size={}, orderNo={}, supplierName={}, status={}", page, size, orderNo, supplierName, status);
        PageResult<DevicePurchaseOrder> result = purchaseService.page(page, size, orderNo, supplierName,
                purchaseDateStart, purchaseDateEnd, status);
        return Result.ok(result);
    }

    @Operation(summary = "查看采购单详情（含明细）")
    @GetMapping("/{id}")
    public Result<DevicePurchaseOrder> detail(@PathVariable Long id) {
        log.info("查看采购详情: id={}", id);
        DevicePurchaseOrder order = purchaseService.detail(id);
        return Result.ok(order);
    }

    @Operation(summary = "新增采购单")
    @PostMapping
    public Result<DevicePurchaseOrder> create(@Valid @RequestBody PurchaseOrderRequest request) {
        log.info("新增采购单: supplierName={}", request.getSupplierName());
        DevicePurchaseOrder order = purchaseService.create(request);
        return Result.ok("采购单创建成功", order);
    }

    @Operation(summary = "修改采购单")
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody PurchaseOrderRequest request) {
        log.info("修改采购单: id={}", id);
        purchaseService.update(id, request);
        return Result.ok("采购单修改成功");
    }

    @Operation(summary = "删除采购单")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        log.info("删除采购单: id={}", id);
        purchaseService.delete(id);
        return Result.ok("采购单已删除");
    }

    @Operation(summary = "采购入库操作")
    @PostMapping("/stock-in")
    public Result<?> stockIn(@Valid @RequestBody StockInRequest request) {
        log.info("采购入库: purchaseOrderId={}", request.getPurchaseOrderId());
        purchaseService.stockIn(request);
        return Result.ok("入库操作成功");
    }

    @Operation(summary = "查询采购单的入库记录")
    @GetMapping("/{id}/stock-in-orders")
    public Result<List<DeviceStockInOrder>> listStockInOrders(@PathVariable Long id) {
        log.info("查询采购入库记录: purchaseOrderId={}", id);
        List<DeviceStockInOrder> orders = purchaseService.listStockInOrders(id);
        return Result.ok(orders);
    }

    @Operation(summary = "上传采购相关文件（合格证/检验报告/发货单）")
    @PostMapping("/upload-file")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file,
                                      @RequestParam(value = "fileType", required = false) String fileType) {
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String storedFileName = UUID.randomUUID().toString() + fileExtension;

            File uploadDir = new File(filePathConfig.getUploadPath());
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            File dest = new File(uploadDir, storedFileName);
            file.transferTo(dest);

            log.info("采购文件上传成功: originalName={}, storedName={}", originalFilename, storedFileName);
            return Result.ok(storedFileName);
        } catch (Exception e) {
            log.error("采购文件上传失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    @Operation(summary = "下载采购相关文件")
    @GetMapping("/file/download")
    public void downloadFile(@RequestParam("path") String path, HttpServletResponse response) {
        try {
            File file = new File(filePathConfig.getUploadPath(), path);
            if (!file.exists()) {
                response.setStatus(404);
                response.getWriter().write("文件不存在");
                return;
            }

            String fileName = path;
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, "UTF-8"));
            response.setContentLengthLong(file.length());

            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }
        } catch (Exception e) {
            log.error("采购文件下载失败", e);
        }
    }
}
