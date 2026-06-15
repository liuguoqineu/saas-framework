package com.saas.framework.common.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ApprovalChainItemDTO {
    private Long id;
    private Long reportId;
    private Long approverId;
    private String approverName;
    private Integer approvalLevel;
    private String status;
    private String comment;
    private LocalDateTime approveTime;
    private LocalDateTime createTime;
}
