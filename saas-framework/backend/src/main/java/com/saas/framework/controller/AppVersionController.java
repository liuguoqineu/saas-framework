package com.saas.framework.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.saas.framework.common.Result;
import com.saas.framework.common.annotation.RequirePermission;
import com.saas.framework.common.dto.AppVersionCheckRequest;
import com.saas.framework.common.dto.AppVersionRequest;
import com.saas.framework.common.dto.PageResult;
import com.saas.framework.entity.AppVersion;
import com.saas.framework.service.AppVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Map;

/**
 * APP版本管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/app-version")
@Tag(name = "APP版本管理", description = "版本检查更新、版本CRUD")
public class AppVersionController {

    @Resource
    private AppVersionService appVersionService;

    @Operation(summary = "检查APP更新（手机端调用）")
    @PostMapping("/check")
    public Result<Map<String, Object>> checkUpdate(@Valid @RequestBody AppVersionCheckRequest request) {
        log.info("检查APP更新: currentVersionCode={}, platform={}", request.getCurrentVersionCode(), request.getPlatform());
        Map<String, Object> data = appVersionService.checkUpdate(request);
        return Result.ok(data);
    }

    @Operation(summary = "分页查询版本列表")
    @GetMapping("/page")
    @RequirePermission("app-version:list")
    public Result<PageResult<AppVersion>> page(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) String platform) {
        IPage<AppVersion> iPage = appVersionService.page(page, size, platform);
        return Result.ok(PageResult.of(iPage));
    }

    @Operation(summary = "获取版本详情")
    @GetMapping("/{id}")
    @RequirePermission("app-version:list")
    public Result<AppVersion> getById(@PathVariable Long id) {
        AppVersion version = appVersionService.getById(id);
        return Result.ok(version);
    }

    @Operation(summary = "创建版本")
    @PostMapping
    @RequirePermission("app-version:add")
    public Result<?> create(@Valid @RequestBody AppVersionRequest request) {
        log.info("创建APP版本: versionName={}, platform={}", request.getVersionName(), request.getPlatform());
        appVersionService.create(request);
        return Result.ok("版本创建成功");
    }

    @Operation(summary = "修改版本")
    @PutMapping("/{id}")
    @RequirePermission("app-version:edit")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody AppVersionRequest request) {
        log.info("修改APP版本: id={}", id);
        appVersionService.update(id, request);
        return Result.ok("版本修改成功");
    }

    @Operation(summary = "删除版本")
    @DeleteMapping("/{id}")
    @RequirePermission("app-version:delete")
    public Result<?> delete(@PathVariable Long id) {
        log.info("删除APP版本: id={}", id);
        appVersionService.delete(id);
        return Result.ok("版本删除成功");
    }
}
