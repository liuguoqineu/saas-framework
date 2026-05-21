package com.saas.framework.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.saas.framework.common.Result;
import com.saas.framework.common.annotation.OperationLog;
import com.saas.framework.common.annotation.RequirePermission;
import com.saas.framework.common.dto.PageResult;
import com.saas.framework.common.dto.TenantCreateRequest;
import com.saas.framework.entity.SysTenant;
import com.saas.framework.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Map;

/**
 * 租户管理控制器
 * 仅超级账户可操作
 */
@Slf4j
@RestController
@RequestMapping("/api/tenant")
@Tag(name = "租户管理", description = "租户的增删改查（仅超级账户）")
public class TenantController {

    @Resource
    private TenantService tenantService;

    @Operation(summary = "分页查询租户列表")
    @GetMapping("/page")
    @RequirePermission("tenant:list")
    public Result<PageResult<SysTenant>> page(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        log.info("查询租户列表: page={}, size={}", page, size);
        IPage<SysTenant> iPage = tenantService.page(page, size);
        return Result.ok(PageResult.of(iPage));
    }

    @Operation(summary = "创建租户")
    @PostMapping
    @RequirePermission("tenant:add")
    @OperationLog(operation = "CREATE", module = "租户", description = "创建租户")
    public Result<Map<String, String>> create(@Valid @RequestBody TenantCreateRequest request) {
        log.info("创建租户: name={}, code={}", request.getName(), request.getCode());
        Map<String, String> data = tenantService.create(request);
        return Result.ok("租户创建成功", data);
    }

    @Operation(summary = "修改租户状态")
    @PutMapping("/{id}/status")
    @RequirePermission("tenant:edit")
    @OperationLog(operation = "UPDATE", module = "租户", description = "修改租户状态")
    public Result<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        if (body == null || body.get("status") == null) {
            return Result.error(400, "状态值不能为空");
        }
        Integer status = body.get("status");
        if (status != 0 && status != 1) {
            return Result.error(400, "状态值必须为 0 或 1");
        }
        log.info("修改租户状态: id={}, status={}", id, status);
        tenantService.updateStatus(id, status);
        return Result.ok();
    }
}
