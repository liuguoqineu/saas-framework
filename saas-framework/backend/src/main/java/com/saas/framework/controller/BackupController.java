package com.saas.framework.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.saas.framework.common.Result;
import com.saas.framework.entity.SysBackupRecord;
import com.saas.framework.service.BackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

/**
 * 数据库备份管理接口
 * 仅超级管理员可操作
 */
@Slf4j
@RestController
@RequestMapping("/api/backup")
@Tag(name = "数据库备份管理", description = "数据库备份相关接口")
public class BackupController {

    @Resource
    private BackupService backupService;

    /**
     * 手动备份数据库
     */
    @PostMapping("/manual")
    @Operation(summary = "手动备份数据库", description = "手动触发数据库备份，仅超级管理员可操作")
    public Result<SysBackupRecord> manualBackup() {
        SysBackupRecord record = backupService.manualBackup();
        return Result.ok(record);
    }

    /**
     * 分页查询备份记录
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询备份记录", description = "获取数据库备份记录列表")
    public Result<IPage<SysBackupRecord>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        IPage<SysBackupRecord> result = backupService.page(page, size);
        return Result.ok(result);
    }

    /**
     * 下载备份文件
     */
    @GetMapping("/download/{id}")
    @Operation(summary = "下载备份文件", description = "下载指定的数据库备份文件")
    public void download(@PathVariable Long id, HttpServletResponse response) {
        backupService.download(id, response);
    }

    /**
     * 删除备份记录
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除备份记录", description = "删除指定的备份记录及文件")
    public Result<Void> delete(@PathVariable Long id) {
        backupService.delete(id);
        return Result.ok();
    }
}
