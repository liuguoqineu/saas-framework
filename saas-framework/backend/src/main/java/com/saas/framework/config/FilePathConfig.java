package com.saas.framework.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.File;

/**
 * 文件存储路径配置
 * 将相对路径转换为绝对路径，确保文件存储在项目根目录下
 */
@Slf4j
@Configuration
public class FilePathConfig {

    @Value("${file.upload-path:./uploads/}")
    private String uploadPath;

    @Value("${file.backup-path:./backups/}")
    private String backupPath;

    private String resolvedUploadPath;
    private String resolvedBackupPath;

    @PostConstruct
    public void init() {
        resolvedUploadPath = resolvePath(uploadPath);
        resolvedBackupPath = resolvePath(backupPath);

        log.info("文件上传路径: {}", resolvedUploadPath);
        log.info("备份文件路径: {}", resolvedBackupPath);

        ensureDirectoryExists(resolvedUploadPath);
        ensureDirectoryExists(resolvedBackupPath);
    }

    /**
     * 解析路径，将相对路径转换为基于项目根目录的绝对路径
     */
    private String resolvePath(String path) {
        if (!StringUtils.hasText(path)) {
            path = "./uploads/";
        }

        File pathFile = new File(path);

        if (pathFile.isAbsolute()) {
            return normalizePath(pathFile.getAbsolutePath());
        }

        String userDir = System.getProperty("user.dir");
        File resolvedPath = new File(userDir, path);
        return normalizePath(resolvedPath.getAbsolutePath());
    }

    /**
     * 规范化路径，确保以文件分隔符结尾
     */
    private String normalizePath(String path) {
        if (!path.endsWith(File.separator)) {
            path = path + File.separator;
        }
        return path;
    }

    /**
     * 确保目录存在，不存在则创建
     */
    private void ensureDirectoryExists(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                log.info("创建目录成功: {}", path);
            } else {
                log.error("创建目录失败: {}", path);
            }
        }
    }

    public String getUploadPath() {
        return resolvedUploadPath;
    }

    public String getBackupPath() {
        return resolvedBackupPath;
    }
}
