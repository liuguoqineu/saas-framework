package com.saas.framework.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.saas.framework.entity.SysBackupRecord;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;

/**
 * 数据库备份服务
 */
public interface BackupService {

    /**
     * 手动备份数据库
     * @return 备份记录
     */
    SysBackupRecord manualBackup();

    /**
     * 自动备份（定时任务调用）
     * @return 备份记录
     */
    SysBackupRecord autoBackup();

    /**
     * 分页查询备份记录
     * @param page 页码
     * @param size 每页大小
     * @return 备份记录分页数据
     */
    IPage<SysBackupRecord> page(int page, int size);

    /**
     * 下载备份文件
     * @param id 备份记录ID
     * @param response HTTP响应
     */
    void download(Long id, HttpServletResponse response);

    /**
     * 删除备份记录及文件
     * @param id 备份记录ID
     */
    void delete(Long id);
}
