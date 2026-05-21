package com.saas.framework.controller;

import com.saas.framework.common.Result;
import com.saas.framework.common.annotation.RequirePermission;
import com.saas.framework.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 权限控制器
 * 提供权限树查询
 */
@Slf4j
@RestController
@RequestMapping("/api/permission")
@Tag(name = "权限管理", description = "权限树查询")
public class PermissionController {

    @Resource
    private PermissionService permissionService;

    @Operation(summary = "获取权限树")
    @GetMapping("/tree")
    @RequirePermission("role:list")
    public Result<List<Map<String, Object>>> getTree() {
        log.debug("查询权限树");
        List<Map<String, Object>> tree = permissionService.getPermissionTree();
        return Result.ok(tree);
    }
}
