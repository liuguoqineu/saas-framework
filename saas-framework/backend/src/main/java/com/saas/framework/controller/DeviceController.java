package com.saas.framework.controller;

import com.saas.framework.common.Result;
import com.saas.framework.common.dto.DeviceInstallRequest;
import com.saas.framework.common.dto.PageResult;
import com.saas.framework.entity.Device;
import com.saas.framework.entity.DeviceTimeline;
import com.saas.framework.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/device")
@Tag(name = "设备档案", description = "设备档案管理、安装投用、履历查询")
public class DeviceController {

    @Resource
    private DeviceService deviceService;

    @Operation(summary = "分页查询设备列表")
    @GetMapping("/page")
    public Result<PageResult<Device>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String deviceCode,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String warehouseName,
            @RequestParam(required = false) String installLocation) {
        log.info("查询设备列表: page={}, size={}, deviceCode={}, deviceName={}, status={}, warehouseName={}, installLocation={}",
                page, size, deviceCode, deviceName, status, warehouseName, installLocation);
        PageResult<Device> result = deviceService.page(page, size, deviceCode, deviceName, status, warehouseName, installLocation);
        return Result.ok(result);
    }

    @Operation(summary = "查看设备详情")
    @GetMapping("/{id}")
    public Result<Device> detail(@PathVariable Long id) {
        log.info("查看设备详情: id={}", id);
        Device device = deviceService.detail(id);
        return Result.ok(device);
    }

    @Operation(summary = "填写安装信息")
    @PutMapping("/{id}/install")
    public Result<?> install(@PathVariable Long id, @RequestBody DeviceInstallRequest request) {
        log.info("填写安装信息: id={}", id);
        deviceService.install(id, request);
        return Result.ok("安装信息填写成功");
    }

    @Operation(summary = "查看设备履历时间线")
    @GetMapping("/{id}/timeline")
    public Result<List<DeviceTimeline>> getTimeline(@PathVariable Long id) {
        log.info("查看设备履历时间线: deviceId={}", id);
        List<DeviceTimeline> timeline = deviceService.getTimeline(id);
        return Result.ok(timeline);
    }

    @Operation(summary = "按编码搜索设备（下拉选择用）")
    @GetMapping("/search")
    public Result<List<Device>> searchByCode(@RequestParam(required = false) String keyword) {
        log.info("按编码搜索设备: keyword={}", keyword);
        List<Device> devices = deviceService.searchByCode(keyword);
        return Result.ok(devices);
    }
}
