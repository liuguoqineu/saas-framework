package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户附件表 (biz_customer_attachment)
 * 存储客户相关的合同扫描件、资质文件、现场照片等附件
 */
@Data
@TableName("biz_customer_attachment")
public class BizCustomerAttachment {

    /** 主键ID，自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联客户ID */
    private Long customerId;

    /** 文件名 */
    private String fileName;

    /** 文件存储路径 */
    private String filePath;

    /** 文件类型: 合同扫描件/资质文件/现场照片/其他 */
    private String fileType;

    /** 文件大小(字节) */
    private Long fileSize;

    /** 租户ID，用于数据隔离 */
    private Long tenantId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0-未删除，1-已删除 */
    @TableLogic
    private Integer deleted;
}
