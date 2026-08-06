package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("biz_contract_modify_log")
public class BizContractModifyLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long contractId;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private Long modifyUserId;
    private String modifyUser;
    private LocalDateTime modifyTime;
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
