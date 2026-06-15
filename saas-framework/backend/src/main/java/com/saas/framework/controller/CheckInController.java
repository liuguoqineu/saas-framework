package com.saas.framework.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.saas.framework.common.Result;
import com.saas.framework.common.annotation.OperationLog;
import com.saas.framework.common.annotation.RequirePermission;
import com.saas.framework.common.dto.CheckInRequest;
import com.saas.framework.common.dto.PageResult;
import com.saas.framework.entity.BizCheckIn;
import com.saas.framework.service.CheckInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/check-in")
@Tag(name = "打卡管理", description = "员工打卡、打卡记录查询")
public class CheckInController {

    @Resource
    private CheckInService checkInService;

    @Operation(summary = "打卡（提交时间、地点、照片）")
    @PostMapping
    @RequirePermission("checkin:add")
    @OperationLog(operation = "CREATE", module = "打卡", description = "员工打卡")
    public Result<BizCheckIn> checkIn(@Valid CheckInRequest request,
                                       @RequestParam(value = "photo", required = false) MultipartFile photo) {
        log.info("用户打卡: address={}", request.getAddress());
        BizCheckIn checkIn = checkInService.checkIn(request, photo);
        return Result.ok("打卡成功", checkIn);
    }

    @Operation(summary = "分页查询打卡记录")
    @GetMapping("/page")
    @RequirePermission("checkin:list")
    public Result<PageResult<BizCheckIn>> page(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) String userName,
                                                @RequestParam(required = false) String checkInTimeStart,
                                                @RequestParam(required = false) String checkInTimeEnd) {
        log.info("查询打卡记录: page={}, size={}, userName={}", page, size, userName);
        IPage<BizCheckIn> iPage = checkInService.page(page, size, userName, checkInTimeStart, checkInTimeEnd);
        return Result.ok(PageResult.of(iPage));
    }

    @Operation(summary = "查看打卡详情")
    @GetMapping("/{id}")
    @RequirePermission("checkin:list")
    public Result<BizCheckIn> detail(@PathVariable Long id) {
        log.info("查看打卡详情: id={}", id);
        BizCheckIn checkIn = checkInService.detail(id);
        return Result.ok(checkIn);
    }

    @Operation(summary = "删除打卡记录")
    @DeleteMapping("/{id}")
    @RequirePermission("checkin:delete")
    @OperationLog(operation = "DELETE", module = "打卡", description = "删除打卡记录")
    public Result<?> delete(@PathVariable Long id) {
        log.info("删除打卡记录: id={}", id);
        checkInService.delete(id);
        return Result.ok("打卡记录已删除");
    }

    @Operation(summary = "查询今日打卡状态")
    @GetMapping("/today")
    @RequirePermission("checkin:add")
    public Result<BizCheckIn> todayStatus() {
        log.info("查询今日打卡状态");
        BizCheckIn checkIn = checkInService.todayStatus();
        return Result.ok(checkIn);
    }

    @Operation(summary = "下载打卡照片")
    @GetMapping("/{id}/photo")
    @RequirePermission("checkin:list")
    public void downloadPhoto(@PathVariable Long id, HttpServletResponse response) {
        log.info("下载打卡照片: id={}", id);
        checkInService.downloadPhoto(id, response);
    }
}
