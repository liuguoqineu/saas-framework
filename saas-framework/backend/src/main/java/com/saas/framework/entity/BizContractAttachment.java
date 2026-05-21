package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("biz_contract_attachment")
public class BizContractAttachment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long contractId;
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
