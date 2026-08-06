package com.saas.framework.controller;

import com.saas.framework.common.Result;
import com.saas.framework.common.annotation.RequirePermission;
import com.saas.framework.common.dto.PageResult;
import com.saas.framework.entity.DeviceReplacement;
import com.saas.framework.service.DeviceReplacementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/api/device-replacement")
@Tag(name = "更换记录", description = "设备/配件更换记录查询")
public class DeviceReplacementController {

    @Resource
    private DeviceReplacementService replacementService;

    @Operation(summary = "分页查询更换记录")
    @GetMapping("/page")
    @RequirePermission("repair:list")
    public Result<PageResult<DeviceReplacement>> page(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(required = false) String replacementNo,
                                                       @RequestParam(required = false) Integer replacementType,
                                                       @RequestParam(required = false) Long repairOrderId,
                                                       @RequestParam(required = false) String replaceTimeStart,
                                                       @RequestParam(required = false) String replaceTimeEnd) {
        log.info("查询更换记录: page={}, size={}, replacementNo={}, replacementType={}, repairOrderId={}, replaceTimeStart={}, replaceTimeEnd={}",
                page, size, replacementNo, replacementType, repairOrderId, replaceTimeStart, replaceTimeEnd);
        PageResult<DeviceReplacement> result = replacementService.page(page, size, replacementNo, replacementType, repairOrderId, replaceTimeStart, replaceTimeEnd);
        return Result.ok(result);
    }

    @Operation(summary = "查看更换记录详情")
    @GetMapping("/{id}")
    @RequirePermission("repair:list")
    public Result<DeviceReplacement> detail(@PathVariable Long id) {
        log.info("查看更换记录详情: id={}", id);
        DeviceReplacement replacement = replacementService.detail(id);
        return Result.ok(replacement);
    }
}
