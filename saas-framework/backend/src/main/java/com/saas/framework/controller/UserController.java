package com.saas.framework.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.saas.framework.common.Result;
import com.saas.framework.common.annotation.OperationLog;
import com.saas.framework.common.annotation.RequirePermission;
import com.saas.framework.common.dto.PageResult;
import com.saas.framework.common.dto.UserCreateRequest;
import com.saas.framework.common.dto.UserUpdateRequest;
import com.saas.framework.entity.SysUser;
import com.saas.framework.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * 员工（用户）管理控制器
 * 租户管理员操作本租户员工
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@Tag(name = "员工管理", description = "租户员工的增删改查")
public class UserController {

    @Resource
    private UserService userService;

    @Operation(summary = "分页查询员工列表")
    @GetMapping("/page")
    @RequirePermission("user:list")
    public Result<PageResult<SysUser>> page(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) String realName) {
        log.info("查询员工列表: page={}, size={}, realName={}", page, size, realName);
        IPage<SysUser> iPage = userService.page(page, size, realName);
        return Result.ok(PageResult.of(iPage));
    }

    @Operation(summary = "获取当前租户员工列表（不分页，用于选择器）")
    @GetMapping("/list")
    public Result<List<SysUser>> list(@RequestParam(required = false) String roleName) {
        List<SysUser> users;
        if (StringUtils.hasText(roleName)) {
            users = userService.listByTenantAndRoleName(roleName);
        } else {
            users = userService.listByTenant();
        }
        return Result.ok(users);
    }

    @Operation(summary = "新增员工")
    @PostMapping
    @RequirePermission("user:add")
    @OperationLog(operation = "CREATE", module = "员工", description = "新增员工")
    public Result<?> create(@Valid @RequestBody UserCreateRequest request) {
        log.info("新增员工: username={}, realName={}", request.getUsername(), request.getRealName());
        userService.create(request);
        return Result.ok("员工创建成功");
    }

    @Operation(summary = "修改员工")
    @PutMapping("/{id}")
    @RequirePermission("user:edit")
    @OperationLog(operation = "UPDATE", module = "员工", description = "修改员工")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        log.info("修改员工: id={}", id);
        userService.update(id, request);
        return Result.ok("员工修改成功");
    }

    @Operation(summary = "重置密码")
    @PutMapping("/{id}/reset-password")
    @RequirePermission("user:edit")
    @OperationLog(operation = "UPDATE", module = "员工", description = "重置员工密码")
    public Result<?> resetPassword(@PathVariable Long id) {
        log.info("重置密码: userId={}", id);
        userService.resetPassword(id);
        return Result.ok("密码已重置为 123456");
    }

    @Operation(summary = "删除员工")
    @DeleteMapping("/{id}")
    @RequirePermission("user:delete")
    @OperationLog(operation = "DELETE", module = "员工", description = "删除员工")
    public Result<?> delete(@PathVariable Long id) {
        log.info("删除员工: id={}", id);
        userService.delete(id);
        return Result.ok("员工删除成功");
    }
}
