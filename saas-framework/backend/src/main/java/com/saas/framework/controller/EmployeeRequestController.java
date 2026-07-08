package com.saas.framework.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.saas.framework.common.Result;
import com.saas.framework.common.annotation.OperationLog;
import com.saas.framework.common.annotation.RequirePermission;
import com.saas.framework.common.dto.EmployeeRequestBatchReviewDTO;
import com.saas.framework.common.dto.EmployeeRequestCreateDTO;
import com.saas.framework.common.dto.EmployeeRequestReviewDTO;
import com.saas.framework.common.dto.PageResult;
import com.saas.framework.config.FilePathConfig;
import com.saas.framework.entity.SysEmployeeRequest;
import com.saas.framework.service.EmployeeRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * 员工申请管理控制器
 * 租户管理员提交申请，超级管理员审核
 */
@Slf4j
@RestController
@RequestMapping("/api/employee-request")
@Tag(name = "员工申请", description = "租户管理员提交新增员工申请，超级管理员审核")
public class EmployeeRequestController {

    @Resource
    private EmployeeRequestService employeeRequestService;

    @Resource
    private FilePathConfig filePathConfig;

    @Operation(summary = "提交员工申请（租户管理员）")
    @PostMapping
    @RequirePermission("user:add")
    @OperationLog(operation = "CREATE", module = "员工申请", description = "提交员工申请")
    public Result<?> submit(@Valid @RequestBody EmployeeRequestCreateDTO request) {
        log.info("提交员工申请: username={}, realName={}", request.getUsername(), request.getRealName());
        employeeRequestService.submit(request);
        return Result.ok("员工申请已提交，等待超级管理员审核");
    }

    @Operation(summary = "上传资质证书图片")
    @PostMapping("/upload-zhizhi-image")
    @RequirePermission("user:add")
    public Result<Map<String, String>> uploadZhizhiImage(@RequestParam("file") MultipartFile file) {
        log.info("上传资质证书图片");

        if (file == null || file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        // 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("只能上传图片文件");
        }

        // 校验文件大小（最大5MB）
        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.error("图片大小不能超过5MB");
        }

        // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;

        // 保存文件
        String zhizhiDir = filePathConfig.getUploadPath() + "zhizhi" + File.separator + datePath + File.separator;
        File dir = new File(zhizhiDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File destFile = new File(zhizhiDir + fileName);
        try {
            file.transferTo(destFile);
        } catch (IOException e) {
            log.error("资质证书图片上传失败", e);
            return Result.error("图片上传失败");
        }

        // 生成访问URL
        String imageUrl = "/uploads/zhizhi/" + datePath + "/" + fileName;
        log.info("资质证书图片上传成功: imageUrl={}", imageUrl);
        return Result.ok(Map.of("imageUrl", imageUrl));
    }

    @Operation(summary = "审核列表（超级管理员）")
    @GetMapping("/page")
    @RequirePermission("employee-request:review")
    @OperationLog(operation = "QUERY", module = "员工申请", description = "查询审核列表")
    public Result<PageResult<SysEmployeeRequest>> page(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size,
                                                        @RequestParam(required = false) String status) {
        log.info("查询员工审核列表: page={}, size={}, status={}", page, size, status);
        IPage<SysEmployeeRequest> iPage = employeeRequestService.page(page, size, status);
        return Result.ok(PageResult.of(iPage));
    }

    @Operation(summary = "查询员工申请详情")
    @GetMapping("/{id}")
    @RequirePermission("employee-request:review")
    public Result<SysEmployeeRequest> getById(@PathVariable Long id) {
        log.info("查询员工申请详情: id={}", id);
        SysEmployeeRequest request = employeeRequestService.getById(id);
        return Result.ok(request);
    }

    @Operation(summary = "本租户申请列表（租户管理员）")
    @GetMapping("/my")
    @RequirePermission("user:list")
    public Result<PageResult<SysEmployeeRequest>> myRequests(@RequestParam(defaultValue = "1") int page,
                                                              @RequestParam(defaultValue = "10") int size,
                                                              @RequestParam(required = false) String status) {
        log.info("查询本租户员工申请: page={}, size={}, status={}", page, size, status);
        IPage<SysEmployeeRequest> iPage = employeeRequestService.myRequests(page, size, status);
        return Result.ok(PageResult.of(iPage));
    }

    @Operation(summary = "审核员工申请（超级管理员）")
    @PutMapping("/{id}/review")
    @RequirePermission("employee-request:review")
    @OperationLog(operation = "REVIEW", module = "员工申请", description = "审核员工申请")
    public Result<?> review(@PathVariable Long id, @Valid @RequestBody EmployeeRequestReviewDTO request) {
        log.info("审核员工申请: id={}, action={}", id, request.getAction());
        employeeRequestService.review(id, request);
        return Result.ok("APPROVED".equals(request.getAction()) ? "审核通过，员工已创建" : "已拒绝申请");
    }

    @Operation(summary = "批量审核员工申请（超级管理员）")
    @PutMapping("/batch-review")
    @RequirePermission("employee-request:review")
    @OperationLog(operation = "BATCH_REVIEW", module = "员工申请", description = "批量审核员工申请")
    public Result<?> batchReview(@Valid @RequestBody EmployeeRequestBatchReviewDTO request) {
        log.info("批量审核员工申请: ids={}, action={}", request.getIds(), request.getAction());
        employeeRequestService.batchReview(request);
        return Result.ok("批量审核完成");
    }

    @Operation(summary = "撤销员工申请（租户管理员）")
    @PutMapping("/{id}/cancel")
    @RequirePermission("user:add")
    @OperationLog(operation = "CANCEL", module = "员工申请", description = "撤销员工申请")
    public Result<?> cancel(@PathVariable Long id) {
        log.info("撤销员工申请: id={}", id);
        employeeRequestService.cancel(id);
        return Result.ok("申请已撤销");
    }
}