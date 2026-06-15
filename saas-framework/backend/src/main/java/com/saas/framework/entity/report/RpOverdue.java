package com.saas.framework.entity.report;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("rp_overdue")
public class RpOverdue {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String reportType;
    private String reportPeriod;
    private LocalDateTime deadline;
    private Integer isReminded;
    private Long tenantId;
    private LocalDateTime createTime;
}
