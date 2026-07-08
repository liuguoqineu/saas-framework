package com.saas.framework.common.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量审核员工申请 DTO
 */
@Data
public class EmployeeRequestBatchReviewDTO {

    /** 申请ID列表 */
    @NotEmpty(message = "申请ID列表不能为空")
    private List<Long> ids;

    /** 审核动作：APPROVED-通过，REJECTED-拒绝 */
    @NotNull(message = "审核动作不能为空")
    private String action;

    /** 审核意见（可选，批量审核时统一意见） */
    private String reviewComment;
}