package com.saas.framework.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.saas.framework.common.Result;
import com.saas.framework.common.annotation.OperationLog;
import com.saas.framework.common.annotation.RequirePermission;
import com.saas.framework.common.dto.CustomerStatusChangeRequest;
import com.saas.framework.common.dto.FollowUpRecordRequest;
import com.saas.framework.common.dto.PageResult;
import com.saas.framework.entity.BizCustomerStatusLog;
import com.saas.framework.entity.BizFollowUpRecord;
import com.saas.framework.service.FollowUpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/follow-up")
@Tag(name = "客户跟进管理", description = "跟进记录、客户状态变更")
public class FollowUpController {

    @Resource
    private FollowUpService followUpService;

    @Operation(summary = "分页查询跟进记录")
    @GetMapping("/records")
    @RequirePermission("followup:list")
    public Result<PageResult<BizFollowUpRecord>> pageRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) Long followUpPersonId,
            @RequestParam(required = false) String followUpPerson,
            @RequestParam(required = false) Integer followUpStatus,
            @RequestParam(required = false) Integer followUpMethod,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        log.info("查询跟进记录: page={}, size={}, customerId={}, followUpStatus={}", page, size, customerId, followUpStatus);
        IPage<BizFollowUpRecord> iPage = followUpService.pageRecords(page, size, customerId, customerName,
                followUpPersonId, followUpPerson, followUpStatus, followUpMethod, startTime, endTime);
        return Result.ok(PageResult.of(iPage));
    }

    @Operation(summary = "新增跟进记录")
    @PostMapping("/records")
    @RequirePermission("followup:add")
    @OperationLog(operation = "CREATE", module = "跟进", description = "新增跟进记录")
    public Result<BizFollowUpRecord> createRecord(@Valid @RequestBody FollowUpRecordRequest request) {
        log.info("新增跟进记录: customerId={}", request.getCustomerId());
        BizFollowUpRecord record = followUpService.createRecord(request);
        return Result.ok("跟进记录添加成功", record);
    }

    @Operation(summary = "查询跟进记录详情")
    @GetMapping("/records/{id}")
    @RequirePermission("followup:list")
    public Result<BizFollowUpRecord> getRecordDetail(@PathVariable Long id) {
        log.info("查询跟进记录详情: id={}", id);
        BizFollowUpRecord record = followUpService.getRecordDetail(id);
        return Result.ok(record);
    }

    @Operation(summary = "编辑跟进记录")
    @PutMapping("/records/{id}")
    @RequirePermission("followup:edit")
    @OperationLog(operation = "UPDATE", module = "跟进", description = "编辑跟进记录")
    public Result<BizFollowUpRecord> updateRecord(@PathVariable Long id, @Valid @RequestBody FollowUpRecordRequest request) {
        log.info("编辑跟进记录: id={}", id);
        BizFollowUpRecord record = followUpService.updateRecord(id, request);
        return Result.ok("跟进记录修改成功", record);
    }

    @Operation(summary = "删除跟进记录")
    @DeleteMapping("/records/{id}")
    @RequirePermission("followup:delete")
    @OperationLog(operation = "DELETE", module = "跟进", description = "删除跟进记录")
    public Result<?> deleteRecord(@PathVariable Long id) {
        log.info("删除跟进记录: id={}", id);
        followUpService.deleteRecord(id);
        return Result.ok("跟进记录已删除");
    }

    @Operation(summary = "导出跟进记录")
    @GetMapping("/records/export")
    @RequirePermission("followup:export")
    @OperationLog(operation = "EXPORT", module = "跟进", description = "导出跟进记录")
    public void exportRecords(HttpServletResponse response,
                               @RequestParam(required = false) Long customerId,
                               @RequestParam(required = false) String customerName,
                               @RequestParam(required = false) Long followUpPersonId,
                               @RequestParam(required = false) String followUpPerson,
                               @RequestParam(required = false) Integer followUpStatus,
                               @RequestParam(required = false) Integer followUpMethod,
                               @RequestParam(required = false) String startTime,
                               @RequestParam(required = false) String endTime) {
        log.info("导出跟进记录");
        followUpService.exportRecords(response, customerId, customerName, followUpPersonId, followUpPerson,
                followUpStatus, followUpMethod, startTime, endTime);
    }

    @Operation(summary = "查询客户的跟进记录列表")
    @GetMapping("/records/customer/{customerId}")
    @RequirePermission("followup:list")
    public Result<List<BizFollowUpRecord>> listRecordsByCustomerId(@PathVariable Long customerId) {
        log.info("查询客户跟进记录: customerId={}", customerId);
        List<BizFollowUpRecord> records = followUpService.listRecordsByCustomerId(customerId);
        return Result.ok(records);
    }

    @Operation(summary = "变更客户合作状态")
    @PutMapping("/customers/{customerId}/status")
    @RequirePermission("followup:status")
    @OperationLog(operation = "UPDATE", module = "跟进", description = "变更客户合作状态")
    public Result<?> changeCustomerStatus(@PathVariable Long customerId, @Valid @RequestBody CustomerStatusChangeRequest request) {
        log.info("变更客户状态: customerId={}, newStatus={}/{}", customerId, request.getNewCooperationCategory(), request.getNewCooperationStatus());
        followUpService.changeCustomerStatus(customerId, request);
        return Result.ok("客户状态变更成功");
    }

    @Operation(summary = "查询客户状态变更记录")
    @GetMapping("/customers/{customerId}/status-logs")
    @RequirePermission("followup:list")
    public Result<List<BizCustomerStatusLog>> listStatusLogs(@PathVariable Long customerId) {
        log.info("查询客户状态变更记录: customerId={}", customerId);
        List<BizCustomerStatusLog> logs = followUpService.listStatusLogs(customerId);
        return Result.ok(logs);
    }
}
