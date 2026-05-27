package com.saas.framework.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.saas.framework.common.context.UserContext;
import com.saas.framework.common.exception.BusinessException;
import com.saas.framework.entity.SysBackupRecord;
import com.saas.framework.mapper.SysBackupRecordMapper;
import com.saas.framework.service.BackupService;
import com.saas.framework.config.FilePathConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 数据库备份服务实现
 */
@Slf4j
@Service
public class BackupServiceImpl implements BackupService {

    @Resource
    private SysBackupRecordMapper sysBackupRecordMapper;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    @Resource
    private FilePathConfig filePathConfig;

    /**
     * 解析数据库名称
     */
    private String parseDatabaseName() {
        try {
            String url = datasourceUrl;
            if (url.contains("?")) {
                url = url.substring(0, url.indexOf("?"));
            }
            if (url.contains("/")) {
                return url.substring(url.lastIndexOf("/") + 1);
            }
        } catch (Exception e) {
            log.error("解析数据库名称失败", e);
        }
        return "saaslearn";
    }

    /**
     * 解析数据库端口
     */
    private int parseDatabasePort() {
        try {
            String url = datasourceUrl;
            if (url.contains(":") && url.contains("/")) {
                int portStart = url.indexOf(":", url.indexOf("//") + 2);
                int portEnd = url.indexOf("/", portStart);
                if (portStart > 0 && portEnd > portStart) {
                    return Integer.parseInt(url.substring(portStart + 1, portEnd));
                }
            }
        } catch (Exception e) {
            log.error("解析数据库端口失败", e);
        }
        return 3306;
    }

    /**
     * 生成备份文件名
     */
    private String generateBackupName(String type) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        return String.format("%s_%s_%s.sql", parseDatabaseName(), type, timestamp);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysBackupRecord manualBackup() {
        log.info("开始手动数据库备份, 操作人: {}", UserContext.getUsername());

        try {
            String backupName = generateBackupName("manual");
            Path backupFilePath = Paths.get(filePathConfig.getBackupPath(), backupName);

            Files.createDirectories(Paths.get(filePathConfig.getBackupPath()));

            SysBackupRecord record = new SysBackupRecord();
            record.setBackupName(backupName);
            record.setBackupPath(backupFilePath.toString());
            record.setBackupType("MANUAL");
            record.setStatus("PROCESSING");
            record.setTenantId(UserContext.getTenantId());
            record.setCreateBy(UserContext.getUsername());

            sysBackupRecordMapper.insert(record);

            executeMysqldump(backupFilePath.toString(), record);

            File backupFile = backupFilePath.toFile();
            record.setBackupSize(backupFile.length());
            record.setStatus("SUCCESS");
            record.setRemark("手动备份成功");

            sysBackupRecordMapper.updateById(record);

            log.info("手动数据库备份成功: {}", backupName);
            return record;

        } catch (Exception e) {
            log.error("手动数据库备份失败", e);
            throw new BusinessException("备份失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysBackupRecord autoBackup() {
        log.info("开始自动数据库备份");

        try {
            String backupName = generateBackupName("auto");
            Path backupFilePath = Paths.get(filePathConfig.getBackupPath(), backupName);

            Files.createDirectories(Paths.get(filePathConfig.getBackupPath()));

            SysBackupRecord record = new SysBackupRecord();
            record.setBackupName(backupName);
            record.setBackupPath(backupFilePath.toString());
            record.setBackupType("AUTO");
            record.setStatus("PROCESSING");
            record.setTenantId(0L);
            record.setCreateBy("SYSTEM");

            sysBackupRecordMapper.insert(record);

            executeMysqldump(backupFilePath.toString(), record);

            File backupFile = backupFilePath.toFile();
            record.setBackupSize(backupFile.length());
            record.setStatus("SUCCESS");
            record.setRemark("自动备份成功");

            sysBackupRecordMapper.updateById(record);

            log.info("自动数据库备份成功: {}", backupName);
            return record;

        } catch (Exception e) {
            log.error("自动数据库备份失败", e);

            SysBackupRecord failedRecord = new SysBackupRecord();
            failedRecord.setBackupName(generateBackupName("auto_failed"));
            failedRecord.setBackupPath("");
            failedRecord.setBackupType("AUTO");
            failedRecord.setStatus("FAILED");
            failedRecord.setTenantId(0L);
            failedRecord.setCreateBy("SYSTEM");
            failedRecord.setRemark("自动备份失败: " + e.getMessage());

            sysBackupRecordMapper.insert(failedRecord);
            return failedRecord;
        }
    }

    /**
     * 执行mysqldump命令
     */
    private void executeMysqldump(String filePath, SysBackupRecord record) throws Exception {
        String databaseName = parseDatabaseName();
        int databasePort = parseDatabasePort();

        log.info("执行mysqldump备份数据库: {}, 端口: {}", databaseName, databasePort);

        ProcessBuilder processBuilder = new ProcessBuilder(
                "mysqldump",
                "-h", "localhost",
                "-P", String.valueOf(databasePort),
                "-u", datasourceUsername,
                "-p" + datasourcePassword,
                "--single-transaction",
                "--routines",
                "--triggers",
                "--default-character-set=utf8mb4",
                databaseName
        );

        log.debug("mysqldump命令参数: {}", processBuilder.command());

        processBuilder.redirectErrorStream(false);
        Process process = processBuilder.start();

        StringBuilder errorOutput = new StringBuilder();
        try (InputStream inputStream = process.getInputStream();
             InputStream errorStream = process.getErrorStream();
             OutputStream outputStream = new FileOutputStream(filePath)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            byte[] errorBuffer = new byte[1024];
            int errorBytesRead;
            while ((errorBytesRead = errorStream.read(errorBuffer)) != -1) {
                errorOutput.append(new String(errorBuffer, 0, errorBytesRead));
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String errorMessage = errorOutput.toString().trim();
            log.error("mysqldump执行失败，退出码: {}, 错误信息: {}", exitCode, errorMessage);
            throw new RuntimeException("mysqldump执行失败，退出码: " + exitCode + (errorMessage.isEmpty() ? "" : ", 错误信息: " + errorMessage));
        }

        cleanupOldBackups();
    }

    /**
     * 清理过期备份（保留最近30天）
     */
    private void cleanupOldBackups() {
        try {
            LambdaQueryWrapper<SysBackupRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysBackupRecord::getStatus, "SUCCESS")
                   .lt(SysBackupRecord::getCreateTime, LocalDateTime.now().minusDays(30))
                   .orderByAsc(SysBackupRecord::getCreateTime);

            IPage<SysBackupRecord> oldBackups = sysBackupRecordMapper.selectPage(new Page<>(1, 100), wrapper);

            for (SysBackupRecord backup : oldBackups.getRecords()) {
                try {
                    Path path = Paths.get(backup.getBackupPath());
                    if (Files.exists(path)) {
                        Files.delete(path);
                        log.info("删除过期备份文件: {}", backup.getBackupName());
                    }
                    sysBackupRecordMapper.deleteById(backup.getId());
                } catch (Exception e) {
                    log.error("删除备份文件失败: {}", backup.getBackupName(), e);
                }
            }
        } catch (Exception e) {
            log.error("清理过期备份失败", e);
        }
    }

    @Override
    public IPage<SysBackupRecord> page(int page, int size) {
        LambdaQueryWrapper<SysBackupRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SysBackupRecord::getCreateTime);

        return sysBackupRecordMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public void download(Long id, HttpServletResponse response) {
        SysBackupRecord record = sysBackupRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(404, "备份记录不存在");
        }

        File file = new File(record.getBackupPath());
        if (!file.exists()) {
            throw new BusinessException(404, "备份文件不存在");
        }

        try (InputStream inputStream = new FileInputStream(file);
             OutputStream outputStream = response.getOutputStream()) {

            String encodedFileName = URLEncoder.encode(record.getBackupName(), "UTF-8");
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=" + encodedFileName);
            response.setContentLengthLong(file.length());

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();

            log.info("下载备份文件: {}, 用户: {}", record.getBackupName(), UserContext.getUsername());

        } catch (IOException e) {
            log.error("下载备份文件失败", e);
            throw new BusinessException("下载失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysBackupRecord record = sysBackupRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(404, "备份记录不存在");
        }

        try {
            Path path = Paths.get(record.getBackupPath());
            if (Files.exists(path)) {
                Files.delete(path);
                log.info("删除备份文件: {}", record.getBackupName());
            }
        } catch (Exception e) {
            log.error("删除备份文件失败", e);
        }

        sysBackupRecordMapper.deleteById(id);
        log.info("删除备份记录: id={}", id);
    }
}
