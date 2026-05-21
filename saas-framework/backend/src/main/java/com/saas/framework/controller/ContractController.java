package com.saas.framework.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.saas.framework.common.Result;
import com.saas.framework.common.annotation.OperationLog;
import com.saas.framework.common.annotation.RequirePermission;
import com.saas.framework.common.dto.ContractRequest;
import com.saas.framework.common.dto.ContractStatusChangeRequest;
import com.saas.framework.common.dto.PageResult;
import com.saas.framework.entity.BizContract;
import com.saas.framework.entity.BizContractAttachment;
import com.saas.framework.entity.BizContractModifyLog;
import com.saas.framework.entity.BizContractReminder;
import com.saas.framework.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/contract")
@Tag(name = "合同管理", description = "合同信息增删改查、附件管理、状态变更、到期提醒")
public class ContractController {

    @Resource
    private ContractService contractService;

    @Operation(summary = "分页查询合同列表（多条件筛选+租户隔离）")
    @GetMapping("/page")
    @RequirePermission("contract:list")
    public Result<PageResult<BizContract>> page(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(required = false) String contractNo,
                                                 @RequestParam(required = false) String customerName,
                                                 @RequestParam(required = false) String signDateStart,
                                                 @RequestParam(required = false) String signDateEnd,
                                                 @RequestParam(required = false) String expireDateStart,
                                                 @RequestParam(required = false) String expireDateEnd,
                                                 @RequestParam(required = false) String contractStatus) {
        log.info("查询合同列表: page={}, size={}, contractNo={}, customerName={}, contractStatus={}",
                page, size, contractNo, customerName, contractStatus);
        IPage<BizContract> iPage = contractService.page(page, size, contractNo, customerName,
                signDateStart, signDateEnd, expireDateStart, expireDateEnd, contractStatus);
        return Result.ok(PageResult.of(iPage));
    }

    @Operation(summary = "查看合同详情")
    @GetMapping("/{id}")
    @RequirePermission("contract:list")
    public Result<BizContract> detail(@PathVariable Long id) {
        log.info("查看合同详情: id={}", id);
        BizContract contract = contractService.detail(id);
        return Result.ok(contract);
    }

    @Operation(summary = "新增合同")
    @PostMapping
    @RequirePermission("contract:add")
    @OperationLog(operation = "CREATE", module = "合同", description = "新增合同")
    public Result<BizContract> create(@Valid @RequestBody ContractRequest request) {
        log.info("新增合同: contractNo={}, customerName={}", request.getContractNo(), request.getCustomerName());
        BizContract contract = contractService.create(request);
        return Result.ok("合同添加成功", contract);
    }

    @Operation(summary = "修改合同信息")
    @PutMapping("/{id}")
    @RequirePermission("contract:edit")
    @OperationLog(operation = "UPDATE", module = "合同", description = "修改合同")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody ContractRequest request) {
        log.info("修改合同: id={}", id);
        contractService.update(id, request);
        return Result.ok("合同修改成功");
    }

    @Operation(summary = "彻底删除合同（物理删除，需权限验证）")
    @DeleteMapping("/{id}")
    @RequirePermission("contract:delete")
    @OperationLog(operation = "DELETE", module = "合同", description = "删除合同")
    public Result<?> delete(@PathVariable Long id) {
        log.info("彻底删除合同: id={}", id);
        contractService.delete(id);
        return Result.ok("合同已彻底删除");
    }

    @Operation(summary = "变更合同状态")
    @PutMapping("/{id}/status")
    @RequirePermission("contract:status")
    @OperationLog(operation = "UPDATE", module = "合同", description = "变更合同状态")
    public Result<?> changeStatus(@PathVariable Long id, @Valid @RequestBody ContractStatusChangeRequest request) {
        log.info("变更合同状态: id={}, newStatus={}", id, request.getNewStatus());
        contractService.changeStatus(id, request);
        return Result.ok("合同状态变更成功");
    }

    @Operation(summary = "查询合同附件列表")
    @GetMapping("/{id}/attachments")
    @RequirePermission("contract:list")
    public Result<List<BizContractAttachment>> listAttachments(@PathVariable Long id) {
        log.info("查询合同附件: contractId={}", id);
        List<BizContractAttachment> attachments = contractService.listAttachments(id);
        return Result.ok(attachments);
    }

    @Operation(summary = "上传合同附件（合同扫描件等）")
    @PostMapping("/{id}/attachment")
    @RequirePermission("contract:edit")
    @OperationLog(operation = "CREATE", module = "合同", description = "上传合同附件")
    public Result<?> uploadAttachment(@PathVariable Long id,
                                       @RequestParam("file") MultipartFile file,
                                       @RequestParam(value = "fileType", defaultValue = "合同扫描件") String fileType) {
        log.info("上传合同附件: contractId={}, fileName={}, fileType={}", id, file.getOriginalFilename(), fileType);
        contractService.uploadAttachment(id, file, fileType);
        return Result.ok("附件上传成功");
    }

    @Operation(summary = "删除合同附件")
    @DeleteMapping("/attachment/{attachmentId}")
    @RequirePermission("contract:edit")
    @OperationLog(operation = "DELETE", module = "合同", description = "删除合同附件")
    public Result<?> deleteAttachment(@PathVariable Long attachmentId) {
        log.info("删除合同附件: attachmentId={}", attachmentId);
        contractService.deleteAttachment(attachmentId);
        return Result.ok("附件已删除");
    }

    @Operation(summary = "下载合同附件")
    @GetMapping("/attachment/{attachmentId}")
    @RequirePermission("contract:list")
    public void downloadAttachment(@PathVariable Long attachmentId, HttpServletResponse response) {
        log.info("下载合同附件: attachmentId={}", attachmentId);
        contractService.downloadAttachment(attachmentId, response);
    }

    @Operation(summary = "查询合同修改记录")
    @GetMapping("/{id}/modify-logs")
    @RequirePermission("contract:list")
    public Result<List<BizContractModifyLog>> listModifyLogs(@PathVariable Long id) {
        log.info("查询合同修改记录: contractId={}", id);
        List<BizContractModifyLog> logs = contractService.listModifyLogs(id);
        return Result.ok(logs);
    }

    @Operation(summary = "查询合同到期提醒列表")
    @GetMapping("/{id}/reminders")
    @RequirePermission("contract:list")
    public Result<List<BizContractReminder>> listReminders(@PathVariable Long id) {
        log.info("查询合同到期提醒: contractId={}", id);
        List<BizContractReminder> reminders = contractService.listReminders(id);
        return Result.ok(reminders);
    }

    @Operation(summary = "获取待处理的合同到期提醒")
    @GetMapping("/reminders/pending")
    @RequirePermission("contract:remind")
    public Result<List<BizContractReminder>> getPendingReminders() {
        log.info("获取待处理的合同到期提醒");
        List<BizContractReminder> reminders = contractService.getPendingReminders();
        return Result.ok(reminders);
    }

    @Operation(summary = "标记提醒已读")
    @PutMapping("/reminders/{reminderId}/read")
    @RequirePermission("contract:remind")
    public Result<?> markReminderRead(@PathVariable Long reminderId) {
        log.info("标记提醒已读: reminderId={}", reminderId);
        contractService.markReminderRead(reminderId);
        return Result.ok("已标记为已读");
    }

    @Operation(summary = "标记提醒已处理")
    @PutMapping("/reminders/{reminderId}/handled")
    @RequirePermission("contract:remind")
    public Result<?> markReminderHandled(@PathVariable Long reminderId) {
        log.info("标记提醒已处理: reminderId={}", reminderId);
        contractService.markReminderHandled(reminderId);
        return Result.ok("已标记为已处理");
    }

    @Operation(summary = "Excel导出合同列表")
    @GetMapping("/export")
    @RequirePermission("contract:list")
    @OperationLog(operation = "EXPORT", module = "合同", description = "导出合同列表")
    public void exportContracts(HttpServletResponse response,
                                 @RequestParam(required = false) String contractNo,
                                 @RequestParam(required = false) String customerName,
                                 @RequestParam(required = false) String signDateStart,
                                 @RequestParam(required = false) String signDateEnd,
                                 @RequestParam(required = false) String expireDateStart,
                                 @RequestParam(required = false) String expireDateEnd,
                                 @RequestParam(required = false) String contractStatus) {
        log.info("Excel导出合同: contractNo={}, customerName={}, contractStatus={}",
                contractNo, customerName, contractStatus);
        contractService.exportContracts(response, contractNo, customerName,
                signDateStart, signDateEnd, expireDateStart, expireDateEnd, contractStatus);
    }
}
