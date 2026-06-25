package com.saas.framework.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.saas.framework.common.Result;
import com.saas.framework.common.annotation.OperationLog;
import com.saas.framework.common.annotation.RequirePermission;
import com.saas.framework.common.dto.*;
import com.saas.framework.entity.BizRepairAttachment;
import com.saas.framework.entity.BizRepairOrder;
import com.saas.framework.entity.BizRepairProcessLog;
import com.saas.framework.service.RepairService;
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
@RequestMapping("/api/repair")
@Tag(name = "报修管理", description = "报修录入、处理流程、查询统计")
public class RepairController {

    @Resource
    private RepairService repairService;

    @Operation(summary = "分页查询报修列表（多条件筛选+租户隔离）")
    @GetMapping("/page")
    @RequirePermission("repair:list")
    public Result<PageResult<BizRepairOrder>> page(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size,
                                                     @RequestParam(required = false) String customerName,
                                                     @RequestParam(required = false) String repairTimeStart,
                                                     @RequestParam(required = false) String repairTimeEnd,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(required = false) String assigneeName,
                                                     @RequestParam(required = false) String urgency,
                                                     @RequestParam(required = false) String repairType) {
        log.info("查询报修列表: page={}, size={}, customerName={}, status={}", page, size, customerName, status);
        IPage<BizRepairOrder> iPage = repairService.page(page, size, customerName, repairTimeStart,
                repairTimeEnd, status, assigneeName, urgency, repairType);
        return Result.ok(PageResult.of(iPage));
    }

    @Operation(summary = "查看报修详情")
    @GetMapping("/{id}")
    @RequirePermission("repair:list")
    public Result<BizRepairOrder> detail(@PathVariable Long id) {
        log.info("查看报修详情: id={}", id);
        BizRepairOrder order = repairService.detail(id);
        return Result.ok(order);
    }

    @Operation(summary = "新增报修单")
    @PostMapping
    @RequirePermission("repair:add")
    @OperationLog(operation = "CREATE", module = "报修", description = "新增报修单")
    public Result<BizRepairOrder> create(@Valid @RequestBody RepairOrderRequest request) {
        log.info("新增报修单: customerName={}, repairContent={}", request.getCustomerName(), request.getRepairContent());
        BizRepairOrder order = repairService.create(request);
        return Result.ok("报修单创建成功", order);
    }

    @Operation(summary = "修改报修信息（补充报修细节）")
    @PutMapping("/{id}")
    @RequirePermission("repair:edit")
    @OperationLog(operation = "UPDATE", module = "报修", description = "修改报修单")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody RepairOrderRequest request) {
        log.info("修改报修单: id={}", id);
        repairService.update(id, request);
        return Result.ok("报修信息修改成功");
    }

    @Operation(summary = "删除报修单")
    @DeleteMapping("/{id}")
    @RequirePermission("repair:delete")
    @OperationLog(operation = "DELETE", module = "报修", description = "删除报修单")
    public Result<?> delete(@PathVariable Long id) {
        log.info("删除报修单: id={}", id);
        repairService.delete(id);
        return Result.ok("报修单已删除");
    }

    @Operation(summary = "分配报修单给运维人员")
    @PutMapping("/{id}/assign")
    @RequirePermission("repair:assign")
    @OperationLog(operation = "UPDATE", module = "报修", description = "分配报修单")
    public Result<?> assign(@PathVariable Long id, @Valid @RequestBody RepairAssignRequest request) {
        log.info("分配报修单: id={}, assignee={}", id, request.getAssigneeName());
        repairService.assign(id, request);
        return Result.ok("报修单分配成功");
    }

    @Operation(summary = "更新报修进度")
    @PutMapping("/{id}/process")
    @RequirePermission("repair:process")
    @OperationLog(operation = "UPDATE", module = "报修", description = "更新报修进度")
    public Result<?> process(@PathVariable Long id, @Valid @RequestBody RepairProcessRequest request) {
        log.info("更新报修进度: id={}, status={}", id, request.getStatus());
        repairService.process(id, request);
        return Result.ok("报修进度更新成功");
    }

    @Operation(summary = "确认报修单闭环")
    @PutMapping("/{id}/confirm")
    @RequirePermission("repair:confirm")
    @OperationLog(operation = "UPDATE", module = "报修", description = "确认报修单闭环")
    public Result<?> confirm(@PathVariable Long id) {
        log.info("确认报修单: id={}", id);
        repairService.confirm(id);
        return Result.ok("报修确认成功");
    }

    @Operation(summary = "标记报修异常")
    @PutMapping("/{id}/exception")
    @RequirePermission("repair:exception")
    @OperationLog(operation = "UPDATE", module = "报修", description = "标记报修异常")
    public Result<?> markException(@PathVariable Long id, @Valid @RequestBody RepairExceptionRequest request) {
        log.info("标记报修异常: id={}", id);
        repairService.markException(id, request);
        return Result.ok("异常标记成功");
    }

    @Operation(summary = "查询报修附件列表")
    @GetMapping("/{id}/attachments")
    @RequirePermission("repair:list")
    public Result<List<BizRepairAttachment>> listAttachments(@PathVariable Long id) {
        log.info("查询报修附件: repairId={}", id);
        List<BizRepairAttachment> attachments = repairService.listAttachments(id);
        return Result.ok(attachments);
    }

    @Operation(summary = "上传报修附件（现场照片、故障截图等）")
    @PostMapping("/{id}/attachment")
    @RequirePermission("repair:edit")
    public Result<?> uploadAttachment(@PathVariable Long id,
                                       @RequestParam("file") MultipartFile file,
                                       @RequestParam(value = "fileType", defaultValue = "现场照片") String fileType) {
        log.info("上传报修附件: repairId={}, fileName={}, fileType={}", id, file.getOriginalFilename(), fileType);
        repairService.uploadAttachment(id, file, fileType);
        return Result.ok("附件上传成功");
    }

    @Operation(summary = "删除报修附件")
    @DeleteMapping("/attachment/{attachmentId}")
    @RequirePermission("repair:edit")
    public Result<?> deleteAttachment(@PathVariable Long attachmentId) {
        log.info("删除报修附件: attachmentId={}", attachmentId);
        repairService.deleteAttachment(attachmentId);
        return Result.ok("附件已删除");
    }

    @Operation(summary = "下载报修附件")
    @GetMapping("/attachment/{attachmentId}/download")
    @RequirePermission("repair:list")
    public void downloadAttachment(@PathVariable Long attachmentId, HttpServletResponse response) {
        log.info("下载报修附件: attachmentId={}", attachmentId);
        repairService.downloadAttachment(attachmentId, response);
    }

    @Operation(summary = "查询报修处理记录")
    @GetMapping("/{id}/process-logs")
    @RequirePermission("repair:list")
    public Result<List<BizRepairProcessLog>> listProcessLogs(@PathVariable Long id) {
        log.info("查询报修处理记录: repairId={}", id);
        List<BizRepairProcessLog> logs = repairService.listProcessLogs(id);
        return Result.ok(logs);
    }

    @Operation(summary = "报修统计")
    @GetMapping("/stats")
    @RequirePermission("repair:stats")
    public Result<RepairStatsVO> stats() {
        log.info("查询报修统计");
        RepairStatsVO vo = repairService.stats();
        return Result.ok(vo);
    }

    @Operation(summary = "Excel导出报修列表")
    @GetMapping("/export")
    @RequirePermission("repair:export")
    @OperationLog(operation = "EXPORT", module = "报修", description = "导出报修列表")
    public void exportRepairOrders(HttpServletResponse response,
                                    @RequestParam(required = false) String customerName,
                                    @RequestParam(required = false) String repairTimeStart,
                                    @RequestParam(required = false) String repairTimeEnd,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) String assigneeName,
                                    @RequestParam(required = false) String urgency,
                                    @RequestParam(required = false) String repairType) {
        log.info("Excel导出报修: customerName={}, status={}", customerName, status);
        repairService.exportRepairOrders(response, customerName, repairTimeStart, repairTimeEnd,
                status, assigneeName, urgency, repairType);
    }

    @Operation(summary = "获取未确认报修提醒")
    @GetMapping("/unconfirmed")
    @RequirePermission("repair:list")
    public Result<List<BizRepairOrder>> getUnconfirmedReminders() {
        log.info("获取未确认报修提醒");
        List<BizRepairOrder> orders = repairService.getUnconfirmedReminders();
        return Result.ok(orders);
    }

    @Operation(summary = "设备故障报修")
    @PostMapping("/device-repair")
    @RequirePermission("repair:add")
    @OperationLog(operation = "CREATE", module = "设备报修", description = "设备故障报修")
    public Result<BizRepairOrder> deviceRepair(@Valid @RequestBody DeviceRepairRequest request) {
        log.info("设备故障报修: deviceId={}", request.getDeviceId());
        BizRepairOrder order = repairService.deviceRepair(request);
        return Result.ok("设备报修创建成功", order);
    }

    @Operation(summary = "设备维修处理（含更换记录）")
    @PutMapping("/{id}/device-process")
    @RequirePermission("repair:process")
    @OperationLog(operation = "UPDATE", module = "设备维修", description = "设备维修处理")
    public Result<?> deviceProcess(@PathVariable Long id, @Valid @RequestBody RepairProcessWithReplacementRequest request) {
        log.info("设备维修处理: id={}, hasReplacement={}", id, request.getHasReplacement());
        repairService.deviceProcess(id, request);
        return Result.ok("维修处理成功");
    }
}
