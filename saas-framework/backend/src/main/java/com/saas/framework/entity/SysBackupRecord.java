package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据库备份记录表 (sys_backup_record)
 */
@Data
@TableName("sys_backup_record")
public class SysBackupRecord {

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 备份文件名 */
    private String backupName;

    /** 备份文件路径 */
    private String backupPath;

    /** 备份文件大小（字节） */
    private Long backupSize;

    /** 备份类型：MANUAL-手动备份，AUTO-自动备份 */
    private String backupType;

    /** 备份状态：SUCCESS-成功，FAILED-失败，PROCESSING-处理中 */
    private String status;

    /** 备注/错误信息 */
    private String remark;

    /** 租户ID，0表示系统级备份 */
    private Long tenantId;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
