package com.saas.framework.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.saas.framework.common.Result;
import com.saas.framework.common.annotation.OperationLog;
import com.saas.framework.common.annotation.RequirePermission;
import com.saas.framework.common.dto.PageResult;
import com.saas.framework.common.dto.RoleCreateRequest;
import com.saas.framework.common.dto.RoleResponse;
import com.saas.framework.entity.SysRole;
import com.saas.framework.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 角色管理控制器
 * 超级账户和租户管理员均可操作（各自管理自己范围内的角色）
 */
@Slf4j
@RestController
@RequestMapping("/api/role")
@Tag(name = "角色管理", description = "角色的增删改查及权限分配")
public class RoleController {

    @Resource
    private RoleService roleService;

    @Operation(summary = "查询角色详情（含权限ID）")
    @GetMapping("/{id}")
    @RequirePermission("role:list")
    public Result<RoleResponse> getById(@PathVariable Long id) {
        log.info("查询角色详情: id={}", id);
        RoleResponse role = roleService.getById(id);
        return Result.ok(role);
    }

    @Operation(summary = "分页查询角色列表")
    @GetMapping("/page")
    @RequirePermission("role:list")
    public Result<PageResult<SysRole>> page(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        log.info("查询角色列表: page={}, size={}", page, size);
        IPage<SysRole> iPage = roleService.page(page, size);
        return Result.ok(PageResult.of(iPage));
    }

    @Operation(summary = "新增角色")
    @PostMapping
    @RequirePermission("role:add")
    @OperationLog(operation = "CREATE", module = "角色", description = "新增角色")
    public Result<?> create(@Valid @RequestBody RoleCreateRequest request) {
        log.info("新增角色: name={}, permissionIds={}", request.getName(), request.getPermissionIds());
        roleService.create(request);
        return Result.ok("角色创建成功");
    }

    @Operation(summary = "修改角色")
    @PutMapping("/{id}")
    @RequirePermission("role:edit")
    @OperationLog(operation = "UPDATE", module = "角色", description = "修改角色")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody RoleCreateRequest request) {
        log.info("修改角色: id={}, name={}", id, request.getName());
        roleService.update(id, request);
        return Result.ok("角色修改成功");
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    @RequirePermission("role:delete")
    @OperationLog(operation = "DELETE", module = "角色", description = "删除角色")
    public Result<?> delete(@PathVariable Long id) {
        log.info("删除角色: id={}", id);
        roleService.delete(id);
        return Result.ok("角色删除成功");
    }
}
