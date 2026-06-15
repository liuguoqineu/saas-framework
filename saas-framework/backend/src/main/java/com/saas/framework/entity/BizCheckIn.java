package com.saas.framework.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("biz_check_in")
public class BizCheckIn {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String userName;
    private LocalDateTime checkInTime;
    private String address;
    private String photoPath;
    private String remark;
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
