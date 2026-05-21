package com.saas.framework.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.saas.framework.common.Result;
import com.saas.framework.common.annotation.RequirePermission;
import com.saas.framework.common.dto.PageResult;
import com.saas.framework.entity.SysOperationLog;
import com.saas.framework.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/operation-log")
@Tag(name = "操作日志", description = "操作日志查询与导出")
public class OperationLogController {

    @Resource
    private OperationLogService operationLogService;

    @Operation(summary = "分页查询操作日志")
    @GetMapping("/page")
    @RequirePermission("log:list")
    public Result<PageResult<SysOperationLog>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        log.info("查询操作日志: page={}, size={}, username={}, operation={}, module={}", page, size, username, operation, module);
        IPage<SysOperationLog> iPage = operationLogService.page(page, size, username, operation, module, ip, startTime, endTime);
        return Result.ok(PageResult.of(iPage));
    }

    @Operation(summary = "导出操作日志CSV")
    @GetMapping("/export")
    @RequirePermission("log:export")
    public void export(
            HttpServletResponse response,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) throws IOException {
        log.info("导出操作日志: username={}, operation={}, module={}", username, operation, module);

        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=operation_log_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".csv");

        IPage<SysOperationLog> iPage = operationLogService.page(1, 10000, username, operation, module, ip, startTime, endTime);
        List<SysOperationLog> records = iPage.getRecords();

        PrintWriter writer = response.getWriter();
        writer.write("\uFEFF");
        writer.println("操作时间,操作人,真实姓名,操作类型,操作模块,操作描述,请求URL,IP地址");

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (SysOperationLog logItem : records) {
            writer.println(String.format("%s,%s,%s,%s,%s,%s,%s,%s",
                    logItem.getCreateTime() != null ? logItem.getCreateTime().format(fmt) : "",
                    logItem.getUsername() != null ? logItem.getUsername() : "",
                    logItem.getRealName() != null ? logItem.getRealName() : "",
                    logItem.getOperation() != null ? logItem.getOperation() : "",
                    logItem.getModule() != null ? logItem.getModule() : "",
                    logItem.getDescription() != null ? logItem.getDescription() : "",
                    logItem.getRequestUrl() != null ? logItem.getRequestUrl() : "",
                    logItem.getIp() != null ? logItem.getIp() : ""
            ));
        }
        writer.flush();
    }
}
