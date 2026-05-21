package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("biz_repair_attachment")
public class BizRepairAttachment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long repairId;
    private String fileName;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
