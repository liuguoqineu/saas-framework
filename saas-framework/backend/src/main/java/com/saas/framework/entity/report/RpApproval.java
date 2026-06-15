package com.saas.framework.entity.report;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("rp_approval")
public class RpApproval {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reportId;
    private Long approverId;
    private Integer approvalLevel;
    private String status;
    private String comment;
    private LocalDateTime approveTime;
    private LocalDateTime createTime;
}
