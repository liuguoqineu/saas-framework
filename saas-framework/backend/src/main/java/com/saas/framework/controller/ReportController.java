package com.saas.framework.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.saas.framework.common.dto.ApprovalChainItemDTO;
import com.saas.framework.common.Result;
import com.saas.framework.common.annotation.RequirePermission;
import com.saas.framework.common.dto.*;
import com.saas.framework.entity.report.*;
import com.saas.framework.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/report")
@Tag(name = "工作报表", description = "日报/周报/月报填报、审批、统计、导出")
public class ReportController {

    @Resource
    private ReportService reportService;

    // ========== Templates ==========

    @Operation(summary = "获取当前用户可用的模板列表")
    @GetMapping("/templates")
    @RequirePermission("report:fill")
    public Result<List<RpTemplate>> getTemplates() {
        return Result.ok(reportService.getTemplates());
    }

    @Operation(summary = "获取模板详情")
    @GetMapping("/templates/{id}")
    @RequirePermission("report:fill")
    public Result<RpTemplate> getTemplate(@PathVariable Long id) {
        return Result.ok(reportService.getTemplate(id));
    }

    // ========== Report CRUD ==========

    @Operation(summary = "查询报表列表")
    @GetMapping("/reports")
    @RequirePermission("report:view")
    public Result<PageResult<RpReport>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false) String reportPeriod,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        IPage<RpReport> iPage = reportService.page(page, size, userId, deptId, reportType, reportPeriod, status, startDate, endDate);
        return Result.ok(PageResult.of(iPage));
    }

    @Operation(summary = "获取报表详情")
    @GetMapping("/reports/{id}")
    @RequirePermission("report:view")
    public Result<RpReport> getReport(@PathVariable Long id) {
        return Result.ok(reportService.getReport(id));
    }

    @Operation(summary = "创建/保存报表草稿")
    @PostMapping("/reports")
    @RequirePermission("report:fill")
    public Result<RpReport> create(@Valid @RequestBody ReportRequest request) {
        return Result.ok(reportService.createOrSave(request));
    }

    @Operation(summary = "更新报表")
    @PutMapping("/reports/{id}")
    @RequirePermission("report:fill")
    public Result<RpReport> update(@PathVariable Long id, @Valid @RequestBody ReportRequest request) {
        return Result.ok(reportService.update(id, request));
    }

    @Operation(summary = "删除草稿")
    @DeleteMapping("/reports/{id}")
    @RequirePermission("report:fill")
    public Result<?> delete(@PathVariable Long id) {
        reportService.deleteDraft(id);
        return Result.ok("删除成功");
    }

    // ========== Submit & Approval ==========

    @Operation(summary = "提交报表")
    @PostMapping("/reports/{id}/submit")
    @RequirePermission("report:fill")
    public Result<?> submit(@PathVariable Long id) {
        reportService.submit(id);
        return Result.ok("提交成功");
    }

    @Operation(summary = "驳回后重新提交")
    @PostMapping("/reports/{id}/resubmit")
    @RequirePermission("report:fill")
    public Result<?> resubmit(@PathVariable Long id, @Valid @RequestBody ReportRequest request) {
        reportService.resubmit(id, request);
        return Result.ok("重新提交成功");
    }

    @Operation(summary = "获取待审批列表")
    @GetMapping("/approvals/pending")
    @RequirePermission("report:approve")
    public Result<List<RpApproval>> pendingApprovals() {
        return Result.ok(reportService.getPendingApprovals());
    }

    @Operation(summary = "审批通过")
    @PostMapping("/approvals/{id}/approve")
    @RequirePermission("report:approve")
    public Result<?> approve(@PathVariable Long id) {
        reportService.approve(id);
        return Result.ok("审批通过");
    }

    @Operation(summary = "驳回")
    @PostMapping("/approvals/{id}/reject")
    @RequirePermission("report:approve")
    public Result<?> reject(@PathVariable Long id, @Valid @RequestBody ApprovalRequest request) {
        reportService.reject(id, request);
        return Result.ok("已驳回");
    }

    @Operation(summary = "获取报表审批链")
    @GetMapping("/reports/{id}/approval-chain")
    @RequirePermission("report:view")
    public Result<List<ApprovalChainItemDTO>> approvalChain(@PathVariable Long id) {
        return Result.ok(reportService.getApprovalChain(id));
    }

    // ========== Revisions ==========

    @Operation(summary = "获取报表修改记录")
    @GetMapping("/reports/{id}/revisions")
    @RequirePermission("report:view")
    public Result<List<RpReportRevision>> revisions(@PathVariable Long id) {
        return Result.ok(reportService.getRevisions(id));
    }

    // ========== Export ==========

    @Operation(summary = "批量导出Excel")
    @GetMapping("/reports/export/excel")
    @RequirePermission("report:export")
    public void exportExcel(HttpServletResponse response,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false) String reportPeriod,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        reportService.exportExcel(response, userId, deptId, reportType, reportPeriod, status, startDate, endDate);
    }

    @Operation(summary = "单条导出PDF")
    @GetMapping("/reports/{id}/export/pdf")
    @RequirePermission("report:export")
    public void exportPdf(HttpServletResponse response, @PathVariable Long id) {
        reportService.exportPdf(response, id);
    }

    // ========== Dashboard ==========

    @Operation(summary = "综合看板")
    @GetMapping("/dashboard/overview")
    @RequirePermission("report:dashboard")
    public Result<DashboardOverview> dashboardOverview() {
        return Result.ok(reportService.getDashboardOverview());
    }

    @Operation(summary = "岗位看板")
    @GetMapping("/dashboard/{postType}")
    @RequirePermission("report:dashboard")
    public Result<Map<String, Object>> dashboardByPost(@PathVariable String postType) {
        return Result.ok(reportService.getDashboardByPost(postType));
    }

    // ========== Overdue ==========

    @Operation(summary = "逾期清单")
    @GetMapping("/overdue/list")
    @RequirePermission("report:overdue:manage")
    public Result<List<Map<String, Object>>> overdueList() {
        return Result.ok(reportService.getOverdueList());
    }

    @Operation(summary = "导出逾期清单")
    @GetMapping("/overdue/export")
    @RequirePermission("report:overdue:manage")
    public void exportOverdue(HttpServletResponse response) {
        reportService.exportOverdueExcel(response);
    }
}
