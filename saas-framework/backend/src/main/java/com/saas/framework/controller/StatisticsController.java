package com.saas.framework.controller;

import com.saas.framework.common.Result;
import com.saas.framework.common.annotation.RequirePermission;
import com.saas.framework.common.dto.*;
import com.saas.framework.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@RestController
@RequestMapping("/api/statistics")
@Tag(name = "统计分析", description = "客户、报修、拜访、合同统计与报表导出")
public class StatisticsController {

    @Resource
    private StatisticsService statisticsService;

    @Operation(summary = "客户统计")
    @GetMapping("/customer")
    @RequirePermission("statistics:customer")
    public Result<CustomerStatsVO> customerStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        log.info("查询客户统计: startDate={}, endDate={}", startDate, endDate);
        return Result.ok(statisticsService.customerStats(startDate, endDate));
    }

    @Operation(summary = "报修统计")
    @GetMapping("/repair")
    @RequirePermission("statistics:repair")
    public Result<RepairTrendStatsVO> repairStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "month") String period) {
        log.info("查询报修统计: startDate={}, endDate={}, period={}", startDate, endDate, period);
        return Result.ok(statisticsService.repairStats(startDate, endDate, period));
    }

    @Operation(summary = "拜访统计")
    @GetMapping("/visit")
    @RequirePermission("statistics:visit")
    public Result<VisitStatsVO> visitStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        log.info("查询拜访统计: startDate={}, endDate={}", startDate, endDate);
        return Result.ok(statisticsService.visitStats(startDate, endDate));
    }

    @Operation(summary = "合同统计")
    @GetMapping("/contract")
    @RequirePermission("statistics:contract")
    public Result<ContractStatsVO> contractStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        log.info("查询合同统计: startDate={}, endDate={}", startDate, endDate);
        return Result.ok(statisticsService.contractStats(startDate, endDate));
    }

    @Operation(summary = "导出客户统计Excel")
    @GetMapping("/customer/export")
    @RequirePermission("statistics:export")
    public void exportCustomerStats(HttpServletResponse response,
                                     @RequestParam(required = false) String startDate,
                                     @RequestParam(required = false) String endDate) {
        log.info("导出客户统计: startDate={}, endDate={}", startDate, endDate);
        statisticsService.exportCustomerStats(response, startDate, endDate);
    }

    @Operation(summary = "导出报修统计Excel")
    @GetMapping("/repair/export")
    @RequirePermission("statistics:export")
    public void exportRepairStats(HttpServletResponse response,
                                   @RequestParam(required = false) String startDate,
                                   @RequestParam(required = false) String endDate,
                                   @RequestParam(required = false, defaultValue = "month") String period) {
        log.info("导出报修统计: startDate={}, endDate={}, period={}", startDate, endDate, period);
        statisticsService.exportRepairStats(response, startDate, endDate, period);
    }

    @Operation(summary = "导出拜访统计Excel")
    @GetMapping("/visit/export")
    @RequirePermission("statistics:export")
    public void exportVisitStats(HttpServletResponse response,
                                  @RequestParam(required = false) String startDate,
                                  @RequestParam(required = false) String endDate) {
        log.info("导出拜访统计: startDate={}, endDate={}", startDate, endDate);
        statisticsService.exportVisitStats(response, startDate, endDate);
    }

    @Operation(summary = "导出合同统计Excel")
    @GetMapping("/contract/export")
    @RequirePermission("statistics:export")
    public void exportContractStats(HttpServletResponse response,
                                     @RequestParam(required = false) String startDate,
                                     @RequestParam(required = false) String endDate) {
        log.info("导出合同统计: startDate={}, endDate={}", startDate, endDate);
        statisticsService.exportContractStats(response, startDate, endDate);
    }
}
