package com.saas.framework.config;

import com.saas.framework.service.BackupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 自动备份定时任务
 * 默认每天凌晨2点执行
 */
@Slf4j
@Component
@EnableScheduling
public class BackupScheduleTask {

    @Resource
    private BackupService backupService;

    /**
     * 每天凌晨2点执行自动备份
     * cron表达式：秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void autoBackupTask() {
        log.info("========== 开始执行定时备份任务 ==========");
        try {
            backupService.autoBackup();
            log.info("========== 定时备份任务执行完成 ==========");
        } catch (Exception e) {
            log.error("========== 定时备份任务执行失败 ==========", e);
        }
    }
}
