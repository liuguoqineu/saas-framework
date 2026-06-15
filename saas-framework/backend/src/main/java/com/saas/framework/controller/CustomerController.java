package com.saas.framework.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.saas.framework.common.Result;
import com.saas.framework.common.annotation.OperationLog;
import com.saas.framework.common.annotation.RequirePermission;
import com.saas.framework.common.dto.CustomerRequest;
import com.saas.framework.common.dto.PageResult;
import com.saas.framework.entity.BizCustomer;
import com.saas.framework.entity.BizCustomerAttachment;
import com.saas.framework.entity.BizCustomerModifyLog;
import com.saas.framework.service.CustomerService;
import com.saas.framework.service.DictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 客户管理控制器
 * 包含客户CRUD、附件管理、修改日志、Excel导入导出
 */
@Slf4j
@RestController
@RequestMapping("/api/customer")
@Tag(name = "客户管理", description = "客户信息的增删改查、附件管理、修改追溯、导入导出")
public class CustomerController {

    @Resource
    private CustomerService customerService;

    @Resource
    private DictService dictService;

    @Operation(summary = "获取客户相关字典数据")
    @GetMapping("/dicts")
    public Result<Map<String, Object>> getDicts() {
        log.info("获取客户字典数据");
        return Result.ok(dictService.getCustomerDicts());
    }

    @Operation(summary = "分页查询客户列表（多条件筛选+租户隔离，默认过滤无效客户）")
    @GetMapping("/page")
    @RequirePermission("customer:list")
    @OperationLog(operation = "QUERY", module = "客户", description = "查询客户列表")
    public Result<PageResult<BizCustomer>> page(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(required = false) String name,
                                                 @RequestParam(required = false) String businessCategory,
                                                 @RequestParam(required = false) String businessType,
                                                 @RequestParam(required = false) String cooperationStatus,
                                                 @RequestParam(required = false) String region,
                                                 @RequestParam(required = false) String contactPerson,
                                                 @RequestParam(required = false) String maintenanceCategory) {
        log.info("查询客户列表: page={}, size={}, name={}, businessCategory={}, businessType={}, cooperationStatus={}, region={}, contactPerson={}, maintenanceCategory={}",
                page, size, name, businessCategory, businessType, cooperationStatus, region, contactPerson, maintenanceCategory);
        IPage<BizCustomer> iPage = customerService.page(page, size, name, businessCategory, businessType,
                cooperationStatus, region, contactPerson, maintenanceCategory);
        return Result.ok(PageResult.of(iPage));
    }

    @Operation(summary = "查看客户详情")
    @GetMapping("/{id}")
    @RequirePermission("customer:list")
    @OperationLog(operation = "QUERY", module = "客户", description = "查看客户详情")
    public Result<?> detail(@PathVariable Long id) {
        log.info("查看客户详情: id={}", id);
        BizCustomer customer = customerService.detail(id);
        return Result.ok(customer);
    }

    @Operation(summary = "新增客户")
    @PostMapping
    @RequirePermission("customer:add")
    @OperationLog(operation = "CREATE", module = "客户", description = "新增客户")
    public Result<?> create(@Valid @RequestBody CustomerRequest request) {
        log.info("新增客户: name={}", request.getName());
        customerService.create(request);
        return Result.ok("客户添加成功");
    }

    @Operation(summary = "修改客户信息")
    @PutMapping("/{id}")
    @RequirePermission("customer:edit")
    @OperationLog(operation = "UPDATE", module = "客户", description = "修改客户")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        log.info("修改客户: id={}", id);
        customerService.update(id, request);
        return Result.ok("客户修改成功");
    }

    @Operation(summary = "标记客户为无效")
    @PutMapping("/{id}/invalid")
    @RequirePermission("customer:invalid")
    @OperationLog(operation = "UPDATE", module = "客户", description = "标记客户无效")
    public Result<?> markInvalid(@PathVariable Long id) {
        log.info("标记客户无效: id={}", id);
        customerService.markInvalid(id);
        return Result.ok("客户已标记为无效");
    }

    @Operation(summary = "恢复无效客户为正常")
    @PutMapping("/{id}/restore")
    @RequirePermission("customer:edit")
    @OperationLog(operation = "UPDATE", module = "客户", description = "恢复无效客户")
    public Result<?> restoreInvalid(@PathVariable Long id) {
        log.info("恢复无效客户: id={}", id);
        customerService.restoreInvalid(id);
        return Result.ok("客户已恢复为正常状态");
    }

    @Operation(summary = "删除客户（软删除，标记为无效）")
    @DeleteMapping("/{id}")
    @RequirePermission("customer:delete")
    @OperationLog(operation = "DELETE", module = "客户", description = "删除客户")
    public Result<?> delete(@PathVariable Long id) {
        log.info("删除客户（软删除）: id={}", id);
        customerService.delete(id);
        return Result.ok("客户已标记为无效");
    }

    @Operation(summary = "查询客户附件列表")
    @GetMapping("/{id}/attachments")
    @RequirePermission("customer:list")
    @OperationLog(operation = "QUERY", module = "客户", description = "查询客户附件列表")
    public Result<List<BizCustomerAttachment>> listAttachments(@PathVariable Long id) {
        log.info("查询客户附件: customerId={}", id);
        List<BizCustomerAttachment> attachments = customerService.listAttachments(id);
        return Result.ok(attachments);
    }

    @Operation(summary = "上传客户附件")
    @PostMapping("/{id}/attachment")
    @RequirePermission("customer:edit")
    @OperationLog(operation = "CREATE", module = "客户", description = "上传客户附件")
    public Result<?> uploadAttachment(@PathVariable Long id,
                                       @RequestParam("file") MultipartFile file,
                                       @RequestParam(value = "fileType", defaultValue = "其他") String fileType) {
        log.info("上传客户附件: customerId={}, fileName={}, fileType={}", id, file.getOriginalFilename(), fileType);
        customerService.uploadAttachment(id, file, fileType);
        return Result.ok("附件上传成功");
    }

    @Operation(summary = "删除客户附件")
    @DeleteMapping("/attachment/{attachmentId}")
    @RequirePermission("customer:edit")
    @OperationLog(operation = "DELETE", module = "客户", description = "删除客户附件")
    public Result<?> deleteAttachment(@PathVariable Long attachmentId) {
        log.info("删除客户附件: attachmentId={}", attachmentId);
        customerService.deleteAttachment(attachmentId);
        return Result.ok("附件已删除");
    }

    @Operation(summary = "下载客户附件")
    @GetMapping("/attachment/{attachmentId}/download")
    @RequirePermission("customer:list")
    @OperationLog(operation = "QUERY", module = "客户", description = "下载客户附件")
    public void downloadAttachment(@PathVariable Long attachmentId, HttpServletResponse response) {
        log.info("下载客户附件: attachmentId={}", attachmentId);
        customerService.downloadAttachment(attachmentId, response);
    }

    @Operation(summary = "查询客户修改记录")
    @GetMapping("/{id}/modify-logs")
    @RequirePermission("customer:list")
    @OperationLog(operation = "QUERY", module = "客户", description = "查询客户修改记录")
    public Result<List<BizCustomerModifyLog>> listModifyLogs(@PathVariable Long id) {
        log.info("查询客户修改记录: customerId={}", id);
        List<BizCustomerModifyLog> logs = customerService.listModifyLogs(id);
        return Result.ok(logs);
    }

    @Operation(summary = "Excel批量导入客户")
    @PostMapping("/import")
    @RequirePermission("customer:import")
    @OperationLog(operation = "IMPORT", module = "客户", description = "导入客户")
    public Result<?> importCustomers(@RequestParam("file") MultipartFile file) {
        log.info("Excel导入客户: fileName={}", file.getOriginalFilename());
        customerService.importCustomers(file);
        return Result.ok("客户导入成功");
    }

    @Operation(summary = "Excel导出客户列表")
    @GetMapping("/export")
    @RequirePermission("customer:export")
    @OperationLog(operation = "EXPORT", module = "客户", description = "导出客户")
    public void exportCustomers(HttpServletResponse response,
                                 @RequestParam(required = false) String name,
                                 @RequestParam(required = false) String businessCategory,
                                 @RequestParam(required = false) String businessType,
                                 @RequestParam(required = false) String cooperationStatus,
                                 @RequestParam(required = false) String region) {
        log.info("Excel导出客户: name={}, businessCategory={}, businessType={}, cooperationStatus={}, region={}",
                name, businessCategory, businessType, cooperationStatus, region);
        customerService.exportCustomers(response, name, businessCategory, businessType, cooperationStatus, region);
    }

    @Operation(summary = "查询公共客户池（未分配跟进人的客户）")
    @GetMapping("/public-pool")
    @RequirePermission("customer:assign")
    @OperationLog(operation = "QUERY", module = "客户", description = "查询公共客户池")
    public Result<PageResult<BizCustomer>> publicPool(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(required = false) String name,
                                                       @RequestParam(required = false) String businessCategory,
                                                       @RequestParam(required = false) String businessType,
                                                       @RequestParam(required = false) String cooperationStatus,
                                                       @RequestParam(required = false) String region) {
        log.info("查询公共客户池: page={}, size={}, name={}, businessCategory={}, businessType={}, cooperationStatus={}, region={}",
                page, size, name, businessCategory, businessType, cooperationStatus, region);
        IPage<BizCustomer> iPage = customerService.publicPool(page, size, name, businessCategory,
                businessType, cooperationStatus, region);
        return Result.ok(PageResult.of(iPage));
    }

    @Operation(summary = "分配客户给销售人员")
    @PostMapping("/{id}/assign")
    @RequirePermission("customer:assign")
    @OperationLog(operation = "UPDATE", module = "客户", description = "分配客户")
    public Result<?> assignCustomer(@PathVariable Long id,
                                     @RequestParam Long userId,
                                     @RequestParam String username) {
        log.info("分配客户: customerId={}, to userId={}, username={}", id, userId, username);
        customerService.assignCustomer(id, userId, username);
        return Result.ok("客户分配成功");
    }

    @Operation(summary = "转移客户给另一个销售人员")
    @PostMapping("/{id}/transfer")
    @RequirePermission("customer:assign")
    @OperationLog(operation = "UPDATE", module = "客户", description = "转移客户")
    public Result<?> transferCustomer(@PathVariable Long id,
                                       @RequestParam Long userId,
                                       @RequestParam String username) {
        log.info("转移客户: customerId={}, to userId={}, username={}", id, userId, username);
        customerService.transferCustomer(id, userId, username);
        return Result.ok("客户转移成功");
    }

    @Operation(summary = "回收客户到公共池")
    @PostMapping("/{id}/reclaim")
    @RequirePermission("customer:assign")
    @OperationLog(operation = "UPDATE", module = "客户", description = "回收客户")
    public Result<?> reclaimCustomer(@PathVariable Long id) {
        log.info("回收客户到公共池: customerId={}", id);
        customerService.reclaimCustomer(id);
        return Result.ok("客户已回收到公共池");
    }
}
